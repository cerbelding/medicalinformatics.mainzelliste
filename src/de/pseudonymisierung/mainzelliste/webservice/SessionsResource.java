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
package de.pseudonymisierung.mainzelliste.webservice;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.PID;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.dto.Persistor;

/**
 * Resource-based access to server-side client sessions.
 * A server-side client session is a set of key-value pairs about a given client session
 * shared between Mainzelliste and an xDAT server. Apart from listing and creating sessions, 
 * knowing the session ID is deemed authentication for session access.
 */
@Path("/sessions")
public class SessionsResource {
	private Logger logger = Logger.getLogger(this.getClass());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response newSession(@Context HttpServletRequest req) throws ServletException, JSONException{
		logger.info("Request to create session received by host " + req.getRemoteHost());
		
		Servers.instance.checkPermission(req, "createSession");
		String sid = Servers.instance.newSession().getId();
		
		URI newUri = UriBuilder
				.fromUri(req.getRequestURL().toString())
				.path("{sid}")
				.build(sid);
		
		logger.info("Created session " + sid);
		
		JSONObject ret = new JSONObject()
				.put("sessionId", sid)
				.put("uri", newUri);
		
		return Response
			.status(Status.CREATED)
			.entity(ret)
			.location(newUri)
			.build();
	}
		
	@Path("/{session}")
	@DELETE
	public Response deleteSession(
			@PathParam("session") String sid,
			@Context HttpServletRequest req){
		// No authentication other than knowing the session id.
		logger.info("Received request to delete session " + sid + " from host " +
				req.getRemoteHost());
		Servers.instance.deleteSession(sid);
		logger.info("Deleted session " + sid);
		return Response
			.status(Status.OK)
			.build();
	}
	
	@Path("/{session}/tokens")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<Token> getTokens(
			@PathParam("session") SessionIdParam sid,
			@Context HttpServletRequest req){
		logger.info("Received request to list tokens for session " + sid + " from host " + 
			req.getRemoteHost());
		return Servers.instance.getAllTokens(sid.getValue().getId());
	}
	
	@Path("/{session}/tokens")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newToken(
			@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam sid,
			String tp) throws JSONException {
		
		Session s = sid.getValue();
		
		logger.info("Received request to create token for session " + s.getId() + " by host " + 
				req.getRemoteHost() + "\n" +
				"Received data: " + tp);
		
		Token t = new TokenParam(tp).getValue();
		
		if(t.getType() == null) {
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Token type must not be empty.")
					.build());
		} else {
			Servers.instance.checkPermission(req, "createToken");
			Servers.instance.checkPermission(req, "tt_" + t.getType());
		}
		
		// Pr�fe Callback-URL
		String callback = t.getDataItemString("callback");
		if (callback != null && !callback.equals("")) {
			if (!Pattern.matches(Config.instance.getProperty("callback.allowedFormat"), callback)) {
				throw new WebApplicationException(Response
						.status(Status.BAD_REQUEST)
						.entity("Callback address " + callback + " does not conform to allowed format.")
						.build()); 
			}
			try {
				URI callbackURI = new URI(callback);
			} catch (URISyntaxException e) {
				throw new WebApplicationException(Response
						.status(Status.BAD_REQUEST)
						.entity("Callback address " + callback + " is not a valid URI.")
						.build());
			}
		}
				
		
		// Prüfe Existenz der ID bei Typ "readPatient"
		if (t.getType() == "readPatient") {
			String idString = t.getDataItemString("id");
			Patient p = Persistor.instance.getPatient(new PID(idString, "pid"));
			if (p == null) {
				throw new WebApplicationException(Response
						.status(Status.BAD_REQUEST)
						.entity("No patient with id '" + idString + "'.")
						.build());
			}
		}

		//Token erstellen, speichern und URL zur�ckgeben
		Token t2 = Servers.instance.newToken(s.getId(), t.getType());
		t2.setData(t.getData());
		
		URI newUri = UriBuilder
				.fromUri(req.getRequestURL().toString())
				.path("/{tid}")
				.build(t2.getId());
		
		JSONObject ret = new JSONObject()
				.put("tokenId", t2.getId())
				.put("uri", newUri);
		
		logger.info("Created token of type " + t2.getType() + " with id " + t2.getId() + 
				" in session " + s.getId() + "\n" +
				"Returned data: " + ret);

		return Response
			.status(Status.CREATED)
			.location(newUri)
			.entity(ret)
			.build();
	}
	
	@Path("/{session}/tokens/{tokenid}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Token getSingleToken(
			@PathParam("session") SessionIdParam sid,
			@PathParam("tokenid") String tokenId,
			@Context HttpServletRequest req){

		Session s = sid.getValue();
		Token t = Servers.instance.getTokenByTid(tokenId); 

		// Nicht jeder, der eine Token-Id hat, sollte das Token lesen können,
		// insbesondere bei Temp-Ids ("readPatient"): Token enthält echte ID
		Servers.instance.checkPermission(req, "tt_" + t.getType());

		// Check that token exists and belongs to specified session
		if (t == null || !s.getTokens().contains(t))
			throw new WebApplicationException(Response
					.status(Status.NOT_FOUND)
					.entity("No token with id " + tokenId + " in session " + sid + ".")
					.build());		
		logger.info("Received request to get token " + tokenId + " in session " + sid +
				" by host " + req.getRemoteHost());
		return t;
	}
}
