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
package de.pseudonymisierung.mainzelliste.webservice;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.spi.resource.Singleton;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

/**
 * Resource-based access to server-side client sessions. A server-side client
 * session is a set of key-value pairs about a given client session shared
 * between Mainzelliste and an xDAT server. Apart from listing and creating
 * sessions, knowing the session ID is deemed as authentication for session
 * access.
 */
@Path("/sessions")
@Singleton
public class SessionsResource {

	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Create a new session.
	 *
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return An HTTP response as specified in the API documentation.
	 * @throws JSONException
	 *             If a JSON error occurs.
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response newSession(@Context HttpServletRequest req) throws JSONException{
		logger.info("Request to create session received by host " + req.getRemoteHost());

		Servers.instance.checkPermission(req, "createSession");

		Session s = Servers.instance.newSession();
		String sid = s.getId();
		URI newUri = UriBuilder
				.fromUri(req.getRequestURL().toString())
				.path("{sid}/")
				.build(sid);
		s.setURI(newUri);

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

	/**
	 * Read a session.
	 *
	 * @param sid
	 *            Id of the session to read.
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return An HTTP response as specified in the API documentation.
	 * @throws JSONException
	 *             If a JSON error occurs.
	 */
	@Path("/{session}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readSession (
			 @PathParam("session") String sid,
			 @Context HttpServletRequest req) throws JSONException {
		logger.info(String.format("Request to read session %s received by host %s", sid, req.getRemoteHost()));
		// No authorization except for knowing the session id
		Session s = Servers.instance.getSession(sid);
		if (s == null) {
			return Response.status(
					Status.NOT_FOUND)
					.entity(String.format("No session with id %s", sid))
					.build();
		}
		JSONObject ret = new JSONObject()
			.put("sessionId", sid)
			.put("uri", s.getURI());

		return Response.status(Status.OK)
				.entity(ret)
				.build();
	}

	/**
	 * Delete a session.
	 *
	 * @param sid
	 *            Id of the session to delete.
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return An HTTP 204 (No Content) response.
	 */
	@Path("/{session}")
	@DELETE
	public synchronized Response deleteSession(
			@PathParam("session") String sid,
			@Context HttpServletRequest req){
		// No authentication other than knowing the session id.
		logger.info("Received request to delete session " + sid + " from host " +
				req.getRemoteHost());
		Servers.instance.deleteSession(sid);
		logger.info("Deleted session " + sid);
		return Response
			.status(Status.NO_CONTENT)
			.build();
	}

	/**
	 * Get the tokens of a session.
	 *
	 * @param sid
	 *            The id of the session whose tokens to get.
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return An HTTP response as specified in the API documentation.
	 */
	@Path("/{session}/tokens")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<Token> getTokens(
			@PathParam("session") SessionIdParam sid,
			@Context HttpServletRequest req){
		logger.info("Received request to list tokens for session " + sid + " from host " +
			req.getRemoteHost());
		// No authorization except for knowing the session id
		return Servers.instance.getAllTokens(sid.getValue().getId());
	}

	/**
	 * Create a token.
	 *
	 * @param req
	 *            The injected HttpServletRequest.
	 * @param uriInfo
	 *            Injected information on application and request URI.
	 * @param sid
	 *            Id of the session in which to create the token.
	 * @param tp
	 *            JSON representation of the token to create.
	 * @return An HTTP response as specified in the API documentation.
	 * @throws JSONException
	 *             If a JSON error occurs.
	 */
	@Path("/{session}/tokens")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response newToken(
			@Context HttpServletRequest req,
			@Context UriInfo uriInfo,
			@PathParam("session") SessionIdParam sid,
			String tp) throws JSONException {

		Session s = sid.getValue();

		logger.info("Received request to create token for session " + s.getId() + " by host " +
				req.getRemoteHost());
		logger.debug("Received data: " + tp);

		Token t = new TokenParam(tp).getValue();
		t.setParentSessionId(s.getId());
		Object parentServerName = req.getSession(true).getAttribute("serverName");
		if(parentServerName == null) {
			logger.info("parentServerName can't be derived from this request. Reason could be that JSESSIONID is not being sent, or the Tomcat servlet context is badly configured (e.g. SSL enabled/disabled), see server.xml  ");
		}
		else{
			t.setParentServerName(parentServerName.toString());
		}
		
		if(t.getType() == null) {
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Token type must not be empty.")
					.build());
		} else {
			Servers.instance.checkPermission(req, "createToken");
			Servers.instance.checkPermission(req, "tt_" + t.getType());
		}

		// Check validity of token (i.e. data items have correct format etc.)
		t.checkValidity(Servers.instance.getRequestApiVersion(req));

		//Token erstellen, speichern und URL zurückgeben
		  Servers.instance.registerToken(s.getId(), t);

		URI newUri = UriBuilder
				.fromUri(req.getRequestURL().toString())
				.path("/{tid}")
				.build(t.getId());

		logger.info("Created token of type " + t.getType() + " with id " + t.getId() +
				" in session " + s.getId());
		logger.debug("Returned data for token " + t.getId() + ": "
				+ t.toJSON(Servers.instance.getRequestApiVersion(req)));

		return Response
			.status(Status.CREATED)
			.location(newUri)
			.entity(t.toJSON(Servers.instance.getRequestApiVersion(req)))
			.build();
	}

	/**
	 * Get a token as JSON.
	 *
	 * @param sid
	 *            Id of the session the requested token belongs to.
	 * @param tokenId
	 *            Id of the token to get.
	 * @param req
	 *            The injected HttpServletRequest.
	 * @param uriInfo
	 *            Injected information on application and request URI.
	 * @return An HTTP response as specified in the API documentation.
	 */
	@Path("/{session}/tokens/{tokenid}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getSingleToken(
			@PathParam("session") SessionIdParam sid,
			@PathParam("tokenid") String tokenId,
			@Context HttpServletRequest req,
			@Context UriInfo uriInfo){

		logger.info("Received request to get token " + tokenId + " in session " + sid.getValue().getId()  +
				" by host " + req.getRemoteHost());

		Session s = sid.getValue();
		Token t = Servers.instance.getTokenByTid(tokenId);

		// Check that token exists and belongs to specified session
		if (t == null || !s.getTokens().contains(t))
			throw new WebApplicationException(Response
					.status(Status.NOT_FOUND)
					.entity("No token with id " + tokenId + " in session " + sid.getValue().getId()  + ".")
					.build());		
		return t.toJSON(Servers.instance.getRequestApiVersion(req));
	}

	/**
	 * Delete a token.
	 *
	 * @param session
	 *            Id of the session the token to delete belongs to.
	 * @param tokenId
	 *            Id of the token to delete.
	 * @return An HTTP response as specified in the API documentation.
	 */
	@Path("/{session}/tokens/{tokenid}")
	@DELETE
	public synchronized Response deleteToken(@PathParam("session") SessionIdParam session,
			@PathParam("tokenid") String tokenId) {
		/*
		 * Knowing the session and the token id authorizes to delete a token.
		 * Check that session exists in order to prevent requests by users who
		 * only know the token id.
		 */
		session.getValue(); // returns 404 if session does not exist
		Servers.instance.deleteToken(tokenId);
		return Response.status(Status.NO_CONTENT).build();
	}
	
	/**
	 * Get the list of patients related to this session. Requires the
	 * "manageSessionPatients" permission.
	 * 
	 * @param req
	 *            The injected HttpServletRequest
	 * @param session
	 *            Id of the session for which to get the list of patients.
	 * @return On success, an array of patient IDs.
	 */
	@Path("/{session}/patients/")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response getPatients(@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam session) {
		Servers.instance.checkPermission(req, "manageSessionPatients");
		session.getValue(); // Checks if session is valid
		String returnIdType = IDGeneratorFactory.instance.getDefaultIDType();
		JSONArray idsOfPatients = new JSONArray();
		for (Patient thisPatient : session.getValue().getPatients()) {
			idsOfPatients.put(thisPatient.getId(returnIdType).toJSON());
		}
		return Response.ok(idsOfPatients).build();
	}

	/**
	 * Add patients to the list of patients related to this session. Requires
	 * the "manageSessionPatients" permission.
	 * 
	 * @param req
	 *            The injected HttpServletRequest
	 * @param session
	 *            Id of the session for which to add patients.
	 * @param patientIds
	 *            An array of patient IDs.
	 * 
	 * @return The appropriate HTTP response (204 on success).
	 */
	@Path("/{session}/patients/")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response addPatients(@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam session, JSONArray patientIds) {
		Servers.instance.checkPermission(req, "manageSessionPatients");
		session.getValue(); // Checks if session is valid
		try {
			for (int i = 0; i < patientIds.length(); i++) {
				JSONObject patientId = patientIds.getJSONObject(i);
				ID thisId = IDGeneratorFactory.instance.idFromJSON(patientId);
				Patient thisPatient = Persistor.instance.getPatient(thisId);
				if (thisPatient == null)
					throw new InvalidIDException("No patient found with ID " + thisId.toString());
				session.getValue().addPatient(thisPatient);
			}
		} catch (JSONException e) {
			throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST)
					.entity("Received invalid JSON data: " + e.getMessage()).build());
		}
		return Response.noContent().build();
	}

	/**
	 * Set the list of patients related to this session. The existing list is
	 * replaced with the given one. Requires the "manageSessionPatients"
	 * permission.
	 * 
	 * @param req
	 *            The injected HttpServletRequest
	 * @param session
	 *            Id of the session for which to set the list of patients.
	 * @param patientIds
	 *            An array of patient IDs.
	 * 
	 * @return The appropriate HTTP response (204 on success).
	 */
	@Path("/{session}/patients/")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response setPatients(@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam session, JSONArray patientIds) {
		Servers.instance.checkPermission(req, "manageSessionPatients");
		session.getValue(); // Checks if session is valid
		// Collect patients first to make sure they exist in order to only
		// remove existing patients from the session if no eror occurs.
		HashSet<Patient> patientsToSet = new HashSet<Patient>();
		try {
			for (int i = 0; i < patientIds.length(); i++) {
				JSONObject patientId = patientIds.getJSONObject(i);
				ID thisId = IDGeneratorFactory.instance.idFromJSON(patientId);
				Patient thisPatient = Persistor.instance.getPatient(thisId);
				if (thisPatient == null)
					throw new InvalidIDException("No patient found with ID " + thisId.toString());
				patientsToSet.add(thisPatient);
			}
			session.getValue().deleteAllPatients();
			for (Patient thisPatient : patientsToSet) {
				session.getValue().addPatient(thisPatient);
			}
		} catch (JSONException e) {
			throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST)
					.entity("Received invalid JSON data: " + e.getMessage()).build());
		}
		return Response.noContent().build();
	}

	/**
	 * Clear the list of patients related to this session. Requires the
	 * "manageSessionPatients" permission.
	 * 
	 * @param req
	 *            The injected HttpServletRequest
	 * @param session
	 *            Id of the session for which to clear the list of patients.
	 * @return The appropriate HTTP response (204 on success).
	 */
	@Path("/{session}/patients/")
	@DELETE
	public synchronized Response deletePatients(@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam session) {
		Servers.instance.checkPermission(req, "manageSessionPatients");
		session.getValue().deleteAllPatients();
		return Response.noContent().build();
	}

	/**
	 * Remove a patient from the list of patients related to this session.
	 * Requires the "manageSessionPatients" permission.
	 * 
	 * @param req
	 *            The injected HttpServletRequest
	 * @param session
	 *            Id of the session from which to remove the patient.
	 * @param idType
	 *            Type of the ID that identifies the patient to remove.
	 * @param idString
	 *            Value of the ID that identifies the patient to remove.
	 * @return The appropriate HTTP response (204 on success).
	 */
	@Path("/{session}/patients/{idType}/{idString}")
	@DELETE
	public synchronized Response deletePatient(@Context HttpServletRequest req,
			@PathParam("session") SessionIdParam session, @PathParam("idType") String idType,
			@PathParam("idString") String idString) {
		Servers.instance.checkPermission(req, "manageSessionPatients");
		ID idOfPatient = IDGeneratorFactory.instance.buildId(idType, idString);
		Patient patientToDelete = Persistor.instance.getPatient(idOfPatient);
		session.getValue().deletePatient(patientToDelete);
		return Response.noContent().build();
	}
}
