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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.api.view.Viewable;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;
import de.pseudonymisierung.mainzelliste.exceptions.UnauthorizedException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Resource-based access to patients.
 */
@Path("/patients")
public class PatientsResource {
	
	private Logger logger = Logger.getLogger(PatientsResource.class);
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllPatients(@Context HttpServletRequest req,
			@QueryParam("tokenId") String tokenId) throws UnauthorizedException {		
		
		logger.info("Received GET /patients");
		
		/*
		 * If a token (type "readPatients") is provided, use this 
		 */
		if (tokenId != null)
			return this.getPatientsToken(tokenId);
		
		/* 
		 * Unrestricted access for user role 'admin' via tomcat-users.xml. 
		 */
		if (!req.isUserInRole("admin"))
			throw new UnauthorizedException();
		return Response.ok().entity(Persistor.instance.getAllIds()).build();
	}
	

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	public Response newPatientBrowser(
			@QueryParam("tokenId") String tokenId,
			MultivaluedMap<String, String> form,
			@Context HttpServletRequest request){
		Token t = Servers.instance.getTokenByTid(tokenId);
		IDRequest createRet = PatientBackend.instance.createNewPatient(tokenId, form, Servers.instance.getRequestApiVersion(request)); 
		Set<ID> ids = createRet.getRequestedIds();
		MatchResult result = createRet.getMatchResult();
		Map <String, Object> map = new HashMap<String, Object>();
		if (ids == null) { // unsure case
			// Copy form to JSP model so that input is redisplayed
			for (String key : form.keySet())
			{
				map.put(key, form.getFirst(key));
			}
			map.put("readonly", "true");
			map.put("tokenId", tokenId);
			return Response.status(Status.CONFLICT)
					.entity(new Viewable("/unsureMatch.jsp", map)).build();
		} else {
			if (t != null && t.getData() != null && t.getData().containsKey("redirect")) {
				UriTemplate redirectURITempl = new UriTemplate(t.getDataItemString("redirect"));
				HashMap<String, String> templateVarMap = new HashMap<String, String>();
				for (String templateVar : redirectURITempl.getTemplateVariables()) {
					if (templateVar.equals("tokenId")) {
						templateVarMap.put(templateVar, tokenId);
					} else {
						ID thisID = createRet.getAssignedPatient().getId(templateVar);
						String idString = thisID.getIdString();
						templateVarMap.put(templateVar, idString);
					}
				}
				try {
					URI redirectURI = new URI(redirectURITempl.createURI(templateVarMap));
					// Remove query parameters and pass them to JSP. The redirect is put
					// into the "action" tag of a form and the parameters are passed as 
					// hidden fields				
					MultivaluedMap<String, String> queryParams = UriComponent.decodeQuery(redirectURI, true);
					map.put("redirect", redirectURI);
					map.put("redirectParams", queryParams);
					//return Response.status(Status.SEE_OTHER).location(redirectURI).build();
				} catch (URISyntaxException e) {
					// Wird auch beim Anlegen des Tokens geprüft.
					throw new InternalErrorException("Die übergebene Redirect-URL " + redirectURITempl.getTemplate() + "ist ungültig!");
				}
			}
			
			// If Idat are to be redisplayed in the result form...
			if (Boolean.parseBoolean(Config.instance.getProperty("result.printIdat"))) {
				//...copy input to JSP 
				for (String key : form.keySet())
				{
					map.put(key, form.getFirst(key));
				}
				// and set flag for JSP to display them
				map.put("printIdat", true);
			}
			// FIXME alle IDs übergeben und anzeigen
			ID retId = ids.toArray(new ID[0])[0];
			map.put("id", retId.getIdString());
			map.put("tentative", retId.isTentative());
			
			if (Config.instance.debugIsOn() && result.getResultType() != MatchResultType.NON_MATCH)
			{
				map.put("debug", "on");
				map.put("weight", Double.toString(result.getBestMatchedWeight()));
				Map<String, Field<?>> matchedFields = result.getBestMatchedPatient().getFields();
				Map<String, String> bestMatch= new HashMap<String, String>();
				for(String fieldName : matchedFields.keySet())
				{
					bestMatch.put(fieldName, matchedFields.get(fieldName).toString());
				}
				map.put("bestMatch", bestMatch);
			}
			return Response.ok(new Viewable("/patientCreated.jsp", map)).build();
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newPatientJson(
			@QueryParam("tokenId") String tokenId,
			@Context HttpServletRequest request,
			@Context UriInfo context,
			MultivaluedMap<String, String> form) throws JSONException {
		IDRequest response = PatientBackend.instance.createNewPatient(tokenId, form, Servers.instance.getRequestApiVersion(request));
		if (response.getMatchResult().getResultType() == MatchResultType.POSSIBLE_MATCH && response.getRequestedIds() == null) {
			return Response
					.status(Status.CONFLICT)
					.entity("Unable to definitely determined whether the data refers to an existing or to a new patient. " +
							"Please check data or resubmit with sureness=true to get a tentative result. Please check documentation for details.")
					.build();
		}
		logger.debug("Accept: " + request.getHeader("Accept"));
		logger.debug("Content-Type: " + request.getHeader("Content-Type"));
		List<ID> newIds = new LinkedList<ID>(response.getRequestedIds());
		
		int apiMajorVersion = Servers.instance.getRequestMajorApiVersion(request);
		
		if (apiMajorVersion >= 2) {
			JSONArray ret = new JSONArray();
			for (ID thisID : newIds) {
				URI newUri = context.getBaseUriBuilder()
						.path(PatientsResource.class)
						.path("/{idtype}/{idvalue}")
						.build(thisID.getType(), thisID.getIdString());
	
				ret.put(new JSONObject()
					.put("idType", thisID.getType())
					.put("idString", thisID.getIdString())
					.put("tentative", thisID.isTentative())
					.put("uri", newUri));
			}
					
			return Response
				.status(Status.CREATED)
				.entity(ret)
				.build();
		} else {
			/*
			 *  Old api permits only one ID in response. If several
			 *  have been requested, which one to choose?
			 */
			if (newIds.size() > 1) {
				throw new WebApplicationException(
						Response.status(Status.BAD_REQUEST)
						.entity("Selected API version 1.0 permits only one ID in response, " +
								"but several were requested. Set mainzellisteApiVersion to a " +
								"value >= 2.0 or request only one ID type in token.")
								.build());
			}
			
			ID newId = newIds.get(0);
			
			URI newUri = context.getBaseUriBuilder()
					.path(PatientsResource.class)
					.path("/{idtype}/{idvalue}")
					.build(newId.getType(), newId.getIdString());
			
			JSONObject ret = new JSONObject()
					.put("newId", newId.getIdString())
					.put("tentative", newId.isTentative())
					.put("uri", newUri);

			return Response
				.status(Status.CREATED)
				.entity(ret)
				.location(newUri)
				.build();
		}
	}

	
	/**
	 * Interface for Temp-ID-Resolver
	 * 
	 * @param callback
	 * @param data
	 * @return
	 */
	@Path("/tempid")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response resolveTempIds(
			@QueryParam("callback") String callback,
			@QueryParam("data") JSONObject data) {
		if (data.has("subjects")) {
			JSONObject subjects;
			JSONObject result = new JSONObject();
			try {
				subjects = data.getJSONObject("subjects");
				Iterator<?> subjectIt = subjects.keys();
				while (subjectIt.hasNext()) {
					String subject = subjectIt.next().toString();
					JSONArray tempIds = subjects.getJSONArray(subject);
					for (int i = 0; i < tempIds.length(); i++) {
						String tempId = tempIds.getString(i);
						String value = resolveTempId(tempId, subject);
						JSONObject resultSubObject;
						if (!result.has(subject)) {
							resultSubObject = new JSONObject();
							result.putOpt(subject, resultSubObject);
						} else {
							resultSubObject = result.getJSONObject(subject);
						}
						resultSubObject.put(tempId, value);							
					}					
				}
				return Response.ok().entity(result.toString()).build();
			} catch (JSONException e) {
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
			} catch (NoSuchFieldException e) {
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
			}
		} else
			return Response.ok().build();
	}
	
	private String resolveTempId(String tempId, String subject) throws NoSuchFieldException {
		Patient p = getPatientByTempId(tempId);
		if (!p.getInputFields().containsKey(subject))
			throw new NoSuchFieldException("No subject " + subject + " for Temp-ID " + tempId);
		return p.getInputFields().get(subject).getValue().toString();
	}
	
	
	private Patient getPatientByTempId(String tid) throws UnauthorizedException {
		Token t = Servers.instance.getTokenByTid(tid);
		if (t == null || !t.getType().equals("readPatient")) {
			logger.info("Tried to access GET /patients/tempid/ with invalid token " + t);
			throw new UnauthorizedException();
		}
		// TODO: verallgemeinern für andere IDs
		String pidString = t.getDataItemString("id");
		return Persistor.instance.getPatient(IDGeneratorFactory.instance.getFactory("pid").buildId(pidString));		
	}
	
	/**
	 * Get patient via readPatient token
	 * @param tid
	 * @return
	 */
	@Path("/tokenId/{tid}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPatientsToken(
			@PathParam("tid") String tid){
		logger.info("Reveived request to get patient with token " + tid);
		// Check if token exists and has the right type. 
		// Validity of token is checked upon creation
		Token t = Servers.instance.getTokenByTid(tid);
		if (t == null) {
			logger.info("No token with id " + tid + " found");
			throw new InvalidTokenException("Please supply a valid 'readPatients' token.");
		}

		t.checkTokenType("readPatients");
		List<?> requests = t.getDataItemList("searchIds");
		
		JSONArray ret = new JSONArray();
		for (Object item : requests) {
			JSONObject thisPatient = new JSONObject();
			String idType;
			String idString;
			@SuppressWarnings("unchecked")
			Map<String, String> thisSearchId = (Map<String, String>) item; 
			idType = thisSearchId.get("idType");
			idString = thisSearchId.get("idString");
			ID id = IDGeneratorFactory.instance.buildId(idType, idString);
			Patient patient = Persistor.instance.getPatient(id);
			if (t.hasDataItem("resultFields")) {
				// get fields for output
				Map<String, String> outputFields = new HashMap<String, String>();
				@SuppressWarnings("unchecked")
				List<String> fieldNames = (List<String>) t.getDataItemList("resultFields");
				for (String thisFieldName : fieldNames) {
					outputFields.put(thisFieldName, patient.getInputFields().get(thisFieldName).toString());
				}
				try {
					thisPatient.put("fields", outputFields);
				} catch (JSONException e) {
					logger.error("Error while transforming patient fields into JSON", e);
					throw new InternalErrorException("Error while transforming patient fields into JSON");
				}
			}
			
			if (t.hasDataItem("resultIds")) {
				try {
					@SuppressWarnings("unchecked")
					List<String> idTypes = (List<String>) t.getDataItemList("resultIds");
					List<JSONObject> returnIds = new LinkedList<JSONObject>();
					for (String thisIdType : idTypes) {
						returnIds.add(patient.getId(thisIdType).toJSON());
					}
					thisPatient.put("ids", returnIds);
				} catch (JSONException e) {
					logger.error("Error while transforming patient ids into JSON", e);
					throw new InternalErrorException("Error while transforming patient ids into JSON");
				}			
			}
			
			ret.put(thisPatient);
		}
		
		return Response.ok().entity(ret).build();
	}
	
	@Path("/tempid/{tid}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setPatientByTempId(
			@PathParam("tid") String tid,
			Patient p){
		//Hier keine Auth notwendig. Wenn tid existiert, ist der Nutzer dadurch autorisiert.
		//Charakteristika des Patients in DB mit TempID tid austauschen durch die von p
		logger.info("Received PUT /patients/tempid/" + tid);
		throw new NotImplementedException();
	}
}
