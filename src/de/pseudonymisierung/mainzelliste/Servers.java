/*
 * Copyright (C) 2013 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it 
 * with Jersey (https://jersey.java.net) (or a modified version of that 
 * library), containing parts covered by the terms of the General Public 
 * License, version 2.0, the licensors of this Program grant you additional 
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.webservice.Token;

/**
 * Keeps track of servers, i.e. each communication partner that is not a user.
 */
public enum Servers {
	instance;
	
	class Server {
		String apiKey;
		Set<String> permissions;
		Set<String> allowedRemoteAdresses;
	}
	
	private final Map<String, Server> servers = new HashMap<String, Server>();
	private final Map<String, Session> sessions = new HashMap<String, Session>();
	private final Map<String, Token> tokensByTid = new HashMap<String, Token>();
	
	private final long sessionTimeout;
	
	/** The regular time interval after which to check for timed out sessions */
	private final long sessionCleanupInterval = 60000;
	
	Logger logger = Logger.getLogger(Servers.class);
	
	private Servers() {
		// read Server configuration from mainzelliste.conf
		Properties props = Config.instance.getProperties();
		for (int i = 0; ; i++)
		{
			if (!props.containsKey("servers." + i + ".apiKey") ||
				!props.containsKey("servers." + i + ".permissions") ||
				!props.containsKey("servers." + i + ".allowedRemoteAdresses"))
				break;
			
			Server s = new Server();
			s.apiKey = props.getProperty("servers." + i + ".apiKey");
			
			String permissions[] = props.getProperty("servers." + i + ".permissions").split("[;,]");
			s.permissions = new HashSet<String>(Arrays.asList(permissions));
			
			String allowedRemoteAdresses[] = props.getProperty("servers." + i + ".allowedRemoteAdresses").split("[;,]");
			s.allowedRemoteAdresses = new HashSet<String>(Arrays.asList(allowedRemoteAdresses));
			servers.put(s.apiKey, s);
		}
			
		if (Config.instance.getProperty("debug") == "true")
		{
			Token t = new Token("4223", "addPatient");
			tokensByTid.put(t.getId(), t);
		}

		// Read session timeout (maximum time a session can be inactive) from config
		String sessionTimeout = Config.instance.getProperty("sessionTimeout");
		if (sessionTimeout == null) {
			this.sessionTimeout = 600000; // 10 min
		} else {
			try {
				this.sessionTimeout = Long.parseLong(sessionTimeout) * 60000;
				if (this.sessionTimeout <= 0)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				throw new Error("Invalid session timout: " + sessionTimeout + ". Please specify a positive whole number.");
			}
		}
			
		// schedule a regular task to delete timed out sessions
		TimerTask sessionsCleanupThread = new TimerTask() {				
			@Override
			public void run() {
				Servers.this.cleanUpSessions();
			}									
		};
		new Timer().schedule(sessionsCleanupThread, new Date(), sessionCleanupInterval);
	}
	
	public Session newSession(){
		String sid = UUID.randomUUID().toString();
		Session s = new Session(sid);
		synchronized (sessions) {
			sessions.put(sid, s);
		}
		return s;
	}
	
	/**
	 * Returns Session with sid (or null if unknown)
	 * Caller MUST ensure proper synchronization on the session.
	 */
	public Session getSession(String sid) {
		synchronized (sessions) {
			return sessions.get(sid);
		}
	}
	
	/**
	 * Returns all known session ids.
	 */
	public Set<String> getSessionIds(){
		synchronized (sessions) {
			return Collections.unmodifiableSet(new HashSet<String>(sessions.keySet()));
		}
	}
	
	public void deleteSession(String sid){
		Session s;
		synchronized (sessions) {			
			s = sessions.get(sid);
			// silently return if session does not exist
			if (s == null)
				return;
			
			for(Token t: s.getTokens()){
				deleteToken(sid, t.getId());
			}
			sessions.remove(sid);
		}
	}
	
	
	public void cleanUpSessions() {
		logger.debug("Cleaning up sessions...");
		LinkedList<String> sessionsToDelete = new LinkedList<String>();
		Date now = new Date();
		synchronized (sessions) {
			for (Session s : this.sessions.values()) {
				if (now.getTime() - s.getLastAccess().getTime() > this.sessionTimeout)
					sessionsToDelete.add(s.getId());
			}
			// Delete sessions in a separate loop to avoid ConcurrentModificationException
			for (String sessionId : sessionsToDelete) {
				this.deleteSession(sessionId);
				logger.info(String.format("Session %s timed out", sessionId));
			}
		}
	}
	
	
	public void checkPermission(HttpServletRequest req, String permission){
		@SuppressWarnings("unchecked")
		Set<String> perms = (Set<String>) req.getSession(true).getAttribute("permissions");

		if(perms == null){ // Login
			String apiKey = req.getHeader("mainzellisteApiKey");
			if (apiKey == null) // Compatibility to pre 1.0 (needed by secuTrial interface)
				apiKey = req.getHeader("mzidApiKey");
			Server server = servers.get(apiKey);
			
			if(server == null){
				logger.info("No server found with provided API key " + apiKey);
				throw new WebApplicationException(Response
						.status(Status.UNAUTHORIZED)
						.entity("Please supply your API key in HTTP header field 'mainzellisteApiKey'.")
						.build());
			}
		
			if(!server.allowedRemoteAdresses.contains(req.getRemoteAddr())){
				logger.info("IP address " + req.getRemoteAddr() +  " rejected");
				throw new WebApplicationException(Response
						.status(Status.UNAUTHORIZED)
						.entity(String.format("Rejecting your IP address %s.", req.getRemoteAddr()))
						.build());
			}
		
			perms = server.permissions;
			req.getSession().setAttribute("permissions", perms);
			logger.info("Server " + req.getRemoteHost() + " logged in with permissions " + Arrays.toString(perms.toArray()) + ".");
		}
		
		if(!perms.contains(permission)){ // Check permission
			logger.info("Access from " + req.getRemoteHost() + " is denied since they lack permission " + permission + ".");
			throw new WebApplicationException(Response
					.status(Status.UNAUTHORIZED)
					.entity("Your permissions do not allow the requested access.")
					.build());
		}
	}
	
	public void registerToken(String sessionId, Token t){
		String tid = UUID.randomUUID().toString();
		t.setId(tid);
		
		getSession(sessionId).addToken(t);

		synchronized(tokensByTid){
			tokensByTid.put(t.getId(), t);
		}
	}

	/**
	 * If you know this token's sessionId, call deleteToken instead...
	 */
	public void deleteToken(String tokenId) {
		String sessionId = null;
		
		synchronized (sessions) {
			for(String sid: sessions.keySet()){
				for(Token t: sessions.get(sid).getTokens()){
					if(tokenId.equals(t.getId())){
						sessionId = sid;
						break;
					}
				}
			}
		}
		
		deleteToken(sessionId, tokenId);
	}
	
	public void deleteToken(String sessionId, String tokenId) {
		if(sessionId != null){
			getSession(sessionId).deleteToken(tokenId);
		}
		
		synchronized (tokensByTid) {
			tokensByTid.remove(tokenId);
		}
	}
	
	public Set<Token> getAllTokens(String sid){
		Session s = getSession(sid);
		if(s == null) return Collections.emptySet();
		
		return s.getTokens();
	}

	public Token getTokenByTid(String tokenId) {
		synchronized (tokensByTid) {
			return tokensByTid.get(tokenId);
		}
	}
	
	public class ApiVersion {
		public final int majorVersion;
		public final int minorVersion;
		
		protected ApiVersion(String versionString) {
			majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
			minorVersion = Integer.parseInt(versionString.split("\\.")[1]);
		}
	}
	
	public ApiVersion getRequestApiVersion(HttpServletRequest req) {
		// First try to read saved version String (prevents multiple parsing of header etc.)
		String version = null;
		Object versionHeader =req.getAttribute("de.pseudonymisierung.mainzelliste.apiVersion");
		if (versionHeader != null) {
			version = versionHeader.toString();
		} else {
			// Try to read from header
			version = req.getHeader("mainzellisteApiVersion");
			// Try to read from URL parameter
			if (version == null) {
				version = req.getParameter("mainzellisteApiVersion");
			}
			// Otherwise assume 1.0
			if (version == null) {
				version = "1.0";
			}
			if (!Pattern.matches("\\d+\\.\\d+", version)) {
				throw new WebApplicationException(
						Response.status(Status.BAD_REQUEST)
						.entity(String.format("'%s' is not a valid API version. Please " +
								"supply API version in format MAJOR.MINOR as HTTP header or " +
								"URL parameter 'mainzellisteApiVersion'.", version))
						.build());
			}
			// Save in request scope
			req.setAttribute("de.pseudonymisierung.mainzelliste.apiVersion", version);
		}
		return new ApiVersion(version);
	}
	
	public int getRequestMajorApiVersion(HttpServletRequest req) {
		return this.getRequestApiVersion(req).majorVersion;
	}

	public int getRequestMinorApiVersion(HttpServletRequest req) {
		return this.getRequestApiVersion(req).minorVersion;
	}
}
