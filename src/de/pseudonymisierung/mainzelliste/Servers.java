/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
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
import java.util.List;
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
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.openjpa.lib.log.Log;

/**
 * Keeps track of servers, i.e. each communication partner that is not a user.
 * Implemented as a singleton object, which can be referenced by
 * Servers.instance.
 */
public enum Servers {

	/** The singleton instance. */
	instance;

	/**
	 * Represents one registered server.
	 */
	class Server {

		String name;
		/** The apiKey by which this server authenticates itself. */
		String apiKey;
		/** The permissions of this server. */
		Set<String> permissions;
		/** Remote IP addresses that are accepted for requests by this server. */
		Set<String> allowedRemoteAdresses;
		/**
		 * Remote IP address ranges that are accepted for requests by this
		 * server.
		 */
		List<SubnetUtils> allowedRemoteAdressRanges;
	}

	/** All registerd servers. */
	private final Map<String, Server> servers = new HashMap<String, Server>();
	/** All currently active sessions, identified by their session ids. */
	private final Map<String, Session> sessions = new HashMap<String, Session>();
	/** All currently valid tokens, identified by their token ids. */
	private final Map<String, Token> tokensByTid = new HashMap<String, Token>();

	/** Time of inactivity after which a session is invalidated. */
	private final long sessionTimeout;

	/** The regular time interval after which to check for timed out sessions */
	private final long sessionCleanupInterval = 60000;

	/** The session cleanup timer. */
	private final Timer sessionsCleanupTimer;

	/** The loggging instance. */
	Logger logger = Logger.getLogger(Servers.class);

	/**
	 * Creates the singleton instance. Reads configuration properties and
	 * initializes the object.
	 */
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
			s.name = "server" + i;
			s.apiKey = props.getProperty("servers." + i + ".apiKey").trim();
			
			String permissions[] = props.getProperty("servers." + i + ".permissions").split("[;,]");
			s.permissions = new HashSet<String>(Arrays.asList(permissions));
			
			String allowedRemoteAdresses[] = props.getProperty("servers." + i + ".allowedRemoteAdresses").split("[;,]");
			s.allowedRemoteAdressRanges = new LinkedList<SubnetUtils>();
			s.allowedRemoteAdresses = new HashSet<String>();
			for (String thisAddress : allowedRemoteAdresses) {
				// Check whether this is an IP mask in CIDR notation
				try {
					SubnetUtils thisAddressRange = new SubnetUtils(thisAddress);
					s.allowedRemoteAdressRanges.add(thisAddressRange);
				} catch (IllegalArgumentException e) {
					// If not, store as plain IP address
					s.allowedRemoteAdresses.add(thisAddress);
				}
			}
			servers.put(s.apiKey, s);
		}
			
		if(servers.size() == 0) {
			logger.error("No servers added. Is your config complete?");
		}

		if (Config.instance.getProperty("debug") == "true")
		{
			Token t = new Token("4223", "addPatient");
			tokensByTid.put(t.getId(), t);
		}

		// Read session timeout (maximum time a session can be inactive) from
		// config
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
		// remember the timer instance to be able to shut it down
		sessionsCleanupTimer = new Timer();
		sessionsCleanupTimer.schedule(sessionsCleanupThread, new Date(), sessionCleanupInterval);
	}

	/**
	 * Shut down instance. This method is called upon undeployment and releases
	 * resources, such as stopping background threads or removing objects that
	 * would otherwise persist and cause a memory leak. Called by
	 * {@link de.pseudonymisierung.mainzelliste.webservice.ContextShutdownHook}.
	 */
	public void shutdown() {
		// shut down session cleanup timer
		logger.info("Stopping sessions cleanup timer");
		sessionsCleanupTimer.cancel();
	}

	/**
	 * Create a new session. The session is assigned a new unique session id.
	 * 
	 * @return The new session object.
	 */
	public Session newSession() {
		String sid = UUID.randomUUID().toString();
		Session s = new Session(sid);
		synchronized (sessions) {
			sessions.put(sid, s);
		}
		return s;
	}

	/**
	 * Get a session by its session id. Caller MUST ensure proper
	 * synchronization on the session.
	 * 
	 * @param sid
	 *            The id of the session to get.
	 * @return The session or null if no session with the given id exists.
	 */
	public Session getSession(String sid) {
		synchronized (sessions) {
			return sessions.get(sid);
		}
	}

	/**
	 * Returns all known session ids.
	 * 
	 * @return The ids of all active sessions.
	 */
	public Set<String> getSessionIds() {
		synchronized (sessions) {
			return Collections.unmodifiableSet(new HashSet<String>(sessions.keySet()));
		}
	}

	/**
	 * Delete a session. This also deletes (i.e. invalidates) all tokens
	 * belonging to the session.
	 * 
	 * @param sid The id of the session to delete.
	 */
	public void deleteSession(String sid) {
		Session s;
		synchronized (sessions) {
			s = sessions.get(sid);
			// silently return if session does not exist
			if (s == null)
				return;

			for (Token t : s.getTokens()) {
				tokensByTid.remove(t.getId());
			}
			s.deleteAllTokens();
			sessions.remove(sid);
		}
	}

	/**
	 * Check for time-out sessions. Deletes all sessions that have been inactive
	 * for at least the configured timeout.
	 */
	public void cleanUpSessions() {
		logger.debug("Cleaning up sessions...");
		LinkedList<String> sessionsToDelete = new LinkedList<String>();
		Date now = new Date();
		synchronized (sessions) {
			for (Session s : this.sessions.values()) {
				if (now.getTime() - s.getLastAccess().getTime() > this.sessionTimeout)
					sessionsToDelete.add(s.getId());
			}
			// Delete sessions in a separate loop to avoid
			// ConcurrentModificationException
			for (String sessionId : sessionsToDelete) {
				this.deleteSession(sessionId);
				logger.info(String.format("Session %s timed out", sessionId));
			}
		}
	}

	
	/**
	 * Check whether a client is authorized for a request. The IP address of the
	 * requester and the API key (HTTP header "mainzellisteApiKey") are read
	 * from the HttpServletRequest. It is checked whether )according to the
	 * configuration) a server with the provided API exists, if the IP address
	 * lies in the configured set or range, and if the the requested permission
	 * is set for the server.
	 * 
	 * If access is denied, an appropriate WebApplicationException is thrown.
	 * 
	 * @param req
	 *            The injected HTTPServletRequest.
	 * @param permission
	 *            The permission to check, e.g. "addPatient".
	 */
	//TODO: This function is not only checking permissions. it's also adding the configured server permission to a session. The function should have another name and function should be separated.
	public void checkPermission(HttpServletRequest req, String permission) {
		@SuppressWarnings("unchecked")
		Set<String> perms = (Set<String>) req.getSession(true).getAttribute("permissions");

		if(perms == null){ // Login
			String apiKey = req.getHeader("mainzellisteApiKey");
			if (apiKey == null) // Compatibility to pre 1.0 (needed by secuTrial interface)
				apiKey = req.getHeader("mzidApiKey");
			Server server = servers.get(apiKey);
			
			if(server == null){
				logger.info("No server found with provided API key \"" + apiKey + "\"");
				throw new WebApplicationException(Response
						.status(Status.UNAUTHORIZED)
						.entity("Please supply your API key in HTTP header field 'mainzellisteApiKey'.")
						.build());
			}
		
			if(!server.allowedRemoteAdresses.contains(req.getRemoteAddr())){
				boolean addressInRange = false;
				for (SubnetUtils thisAddressRange : server.allowedRemoteAdressRanges) {
					try {
						if (thisAddressRange.getInfo().isInRange(req.getRemoteAddr())) {
							addressInRange = true;
							break;
						}
					} catch (IllegalArgumentException e) {
						// Occurs if an IPv6 address was transmitted
						logger.error("Could not parse IP address " + req.getRemoteAddr(), e);
						break;
					}
				}
				if (!addressInRange) {
					logger.info("IP address " + req.getRemoteAddr() +  " rejected");
					throw new WebApplicationException(Response
							.status(Status.UNAUTHORIZED)
							.entity(String.format("Rejecting your IP address %s.", req.getRemoteAddr()))
							.build());
				}
			}

			perms = server.permissions;

			req.getSession().setAttribute("permissions", perms);
			req.getSession().setAttribute("serverName", getServerNameForApiKey(apiKey));


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

	/**
	 * Register a token in a session. This is necessary so that a token is
	 * recognized as valid.
	 * 
	 * @param sessionId
	 *            Id of the session in which to register the token.
	 * @param t
	 *            The token to register.
	 */
	public void registerToken(String sessionId, Token t) {
		Session s = getSession(sessionId);
		String tid = UUID.randomUUID().toString();
		t.setId(tid);
		t.setURI(s.getURI().resolve("tokens/" + tid));

		getSession(sessionId).addToken(t);

		synchronized (tokensByTid) {
			tokensByTid.put(t.getId(), t);
		}
	}

	/**
	 * Delete the token with the given id. If you know this token's sessionId, call deleteToken instead...
	 * @param tokenId Id of the token to delete.
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

	/**
	 * Delete the token with the given id and containing session. This is more
	 * efficient than {@link #deleteToken(String)} as there is no need to search
	 * the session the token belongs to.
	 * 
	 * @param sessionId
	 *            Id of the session the token belongs to.
	 * @param tokenId
	 *            Id of the token to delete.
	 */
	public void deleteToken(String sessionId, String tokenId) {
		if(sessionId != null){
			getSession(sessionId).deleteToken(tokensByTid.get(tokenId));
		}

		synchronized (tokensByTid) {
			tokensByTid.remove(tokenId);
		}
	}

	/**
	 * Get all tokens of the given session.
	 * 
	 * @param sid
	 *            Id of the session whose tokens to get.
	 * @return The set of tokens.
	 */
	public Set<Token> getAllTokens(String sid) {
		Session s = getSession(sid);
		if(s == null) return Collections.emptySet();
		
		return s.getTokens();
	}

	/**
	 * Get a token by its id.
	 * 
	 * @param tokenId
	 *            Id of the token to get.
	 * @return The token or null if no token with the given id exists.
	 */
	public Token getTokenByTid(String tokenId) {
		synchronized (tokensByTid) {
			return tokensByTid.get(tokenId);
		}
	}

	/**
	 * Check if a token exists and has the given type. If
	 * 
	 * @param tid
	 *            Id of the token to check.
	 * @param type
	 *            Token type to check for (e.g. "addPatient").
	 * @throws InvalidTokenException
	 *             If no token with the given id and type exists.
	 */
	public void checkToken(String tid, String type) throws InvalidTokenException {
		Token t = getTokenByTid(tid);
		if (t == null || !type.equals(t.getType()) ) {
			logger.info("Token with id " + tid + " " + (t == null ? "is unknown." : ("has wrong type '" + t.getType() + "'")));
			throw new InvalidTokenException("Please supply a valid '" + type + "' token.");
		}
	}

	/**
	 * Represents the interface version, consisting of major and minor revision.
	 */
	public class ApiVersion {
		/** The major revision. */
		public final int majorVersion;
		/** The minor revision. */
		public final int minorVersion;
		
		/**
		 * Create an instance from a version string. The version string is split
		 * by dots and the first two segments are used as major and minor
		 * version, respectively.
		 * 
		 * @param versionString A version string in the format "major.minor".
		 */
		protected ApiVersion(String versionString) {
			majorVersion = Integer.parseInt(versionString.split("\\.")[0]);
			minorVersion = Integer.parseInt(versionString.split("\\.")[1]);
		}
		
		public String toString() {
			return majorVersion + "." + minorVersion;
		}
	}

	/**
	 * Get api version from a request. Reads the version string from either the
	 * "mainzellisteApiVersion" header or, if no such header is set, from an URL
	 * parameter of the same name. If neither exists, version 1.0 is assumed.
	 * 
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return The api version inferred from the request.
	 */
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

	/**
	 * Get api version (major version only) from a request.
	 * 
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return The major api version inferred from the request.
	 * 
	 * @see #getRequestApiVersion(HttpServletRequest)
	 */
	public int getRequestMajorApiVersion(HttpServletRequest req) {
		return this.getRequestApiVersion(req).majorVersion;
	}

	/**
	 * Get api version (minor version only) from a request.
	 * 
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return The major api version inferred from the request.
	 * 
	 * @see #getRequestApiVersion(HttpServletRequest)
	 */
	public int getRequestMinorApiVersion(HttpServletRequest req) {
		return this.getRequestApiVersion(req).minorVersion;
	}

	public String getServerNameForApiKey(String apiKey){
		Server server = servers.get(apiKey);
		return server.name;

	}

	public boolean hasServerPermission(String serverName, String permission){

		Server server = servers.get(getApiKeyForServerName(serverName));
		if(server.permissions.contains(permission)) {
			return true;
		}
		return false;
	}

	public String getApiKeyForServerName(String serverName){

		for (Map.Entry<String, Server> entry : this.servers.entrySet()) {
			if(serverName.equals(entry.getValue().name)){
				return entry.getKey();
			}
		}

		return "Server not found";
	}

}
