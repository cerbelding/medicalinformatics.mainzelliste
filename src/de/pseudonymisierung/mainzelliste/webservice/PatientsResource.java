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

import com.google.gson.Gson;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.spi.resource.Singleton;
import de.pseudonymisierung.mainzelliste.AuditTrail;
import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.DerivedIDGenerator;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.api.AddPatientRequest;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidJSONException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.exceptions.NoParentServerNameException;
import de.pseudonymisierung.mainzelliste.exceptions.UnauthorizedException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.webservice.commons.MainzellisteCallback;
import de.pseudonymisierung.mainzelliste.webservice.commons.Redirect;
import de.pseudonymisierung.mainzelliste.webservice.commons.RedirectBuilder;
import de.pseudonymisierung.mainzelliste.webservice.commons.RedirectUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Resource-based access to patients.
 */
@Path("/patients")
@Singleton
public class PatientsResource {
  /**
   * The logging instance.
   */
  private static final Logger logger = LogManager.getLogger(PatientsResource.class);

  private static final Gson gson = new Gson();

  /**
   * Session to be used when in debug mode.
   */
  private Session debugSession = null;

  /**
   * Get a list of patients.
   *
   * @param request The injected HttpServletRequest.
   * @param tokenId Id of a valid "readPatients" token.
   * @return A JSON result as specified in the API documentation.
   * @throws UnauthorizedException If no token is provided.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllPatients(@Context HttpServletRequest request, @QueryParam("tokenId") String tokenId) {
    logger.debug("Received GET /patients");
    return this.getPatients(tokenId, request);
  }

	/**
	 * Create a new patient. Interface for web browser.
	 * 
	 * @param tokenId
	 *            Id of a valid "addPatient" token.
	 * @param mainzellisteApiVersion
	 *            The API version used to make the request.
	 * @param form
	 *            Input as provided by the HTML form.
	 * @param request
	 *            The injected HttpServletRequest.
	 * @return An HTTP response as specified in the API documentation.
	 */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces({MediaType.TEXT_HTML, MediaType.WILDCARD})
  public synchronized Response newPatientBrowser(
      @QueryParam("tokenId") String tokenId,
      @QueryParam("mainzellisteApiVersion") String mainzellisteApiVersion,
      MultivaluedMap<String, String> form,
      @Context HttpServletRequest request) {
    try {
      logger.debug("@POST newPatientBrowser");
      AddPatientToken token = getAddPatientToken(tokenId);
      logger.info("Handling ID Request with token {}", token.getId());
      IDRequest createRet = addNewPatient(form, token.getFields(), token.getIds(),
          token.getRequestedIdTypes(), token.getId());

			Set<ID> ids = createRet.createRequestedIds();
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
				map.put("mainzellisteApiVersion", mainzellisteApiVersion);
				return Response.status(Status.CONFLICT)
						.entity(new Viewable("/unsureMatch.jsp", map)).build();
			} else {
                    // If Idat are to be redisplayed in the result form...
                    if (Boolean.parseBoolean(Config.instance.getProperty("result.printIdat"))) {
                        // ...copy input to JSP
                        for (String key : form.keySet()) {
                            map.put(key, form.getFirst(key));
                        }
                        // and set flag for JSP to display them
                        map.put("printIdat", true);
                    }

                    JSONArray idsAsJson = new JSONArray();
                    ids.forEach(id -> idsAsJson.put(id.toJSON()));
                    map.put("ids", idsAsJson);

                    map.put("tentative", false);
                    // Only put true in map if one or more PID are tentative
                    for (ID id : ids) {
                        if (id != null && id.isTentative()) {
                            map.put("tentative", true);
                            break;
                        }
                    }

                    if (Config.instance.debugIsOn() && result.getResultType() != MatchResultType.NON_MATCH) {
                        map.put("debug", "on");
                        map.put("weight", Double.toString(result.getBestMatchedWeight()));
                        Map<String, Field<?>> matchedFields = result.getBestMatchedPatient().getFields();
                        Map<String, String> bestMatch = new HashMap<String, String>();
                        for (String fieldName : matchedFields.keySet()) {
                            bestMatch.put(fieldName, matchedFields.get(fieldName).toString());
                        }
                        map.put("bestMatch", bestMatch);
                    }
                    // Callback request
                    String callback = token.getDataItemString("callback");
                    if (callback != null && callback.length() > 0) {
                        sendCallback(request, token, ids, null, callback);
                    }
                    String redirectRequest = token.getDataItemString("redirect");
                    if (redirectRequest != null && redirectRequest.length() > 0) {
                        UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
                        List<String> templateVariables = redirectURITempl.getTemplateVariables();
                        List<String> requestedIds = RedirectUtils.getRequestedIDsTypeFromToken(token);

                        Redirect redirect = new RedirectBuilder().setTokenId(token.getId())
                                .setMappedIdTypesdAndIds(requestedIds, createRet).setTemplateURI(redirectURITempl)
                                .build();

                        if (templateVariables.contains("tokenId")
                                && !Boolean.parseBoolean(Config.instance.getProperty("result.show"))) {
                            return redirect.execute();
                        } else {
                            // Remove query parameters and pass them to JSP. The redirect is put
                            // into the "action" tag of a form and the parameters are passed as
                            // hidden fields
                            // TODO: generate for frontend
                            map.put("redirect", redirect.getRedirectURI());
                            map.put("redirectParams", redirect.getRedirectParams());
                            return Response.ok(new Viewable("/patientCreated.jsp", map)).build();
                        }
                    }

                    return Response.ok(new Viewable("/patientCreated.jsp", map)).build();
                }

            } catch (WebApplicationException e) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("message", e.getResponse().getEntity());
                return Response.status(e.getResponse().getStatus()).entity(new Viewable("/errorPage.jsp", map)).build();
            }
    }

	/**
	 * Create a new patient. Interface for software applications.
	 * 
	 * @param tokenId
	 *            Id of a valid "addPatient" token.
	 * @param request
	 *            The injected HttpServletRequest.
	 * @param context
	 *            Injected information of application and request URI.
	 * @param form
	 *            Input as provided by the HTTP request.
	 * @return An HTTP response as specified in the API documentation.
	 * @throws JSONException
	 *             If a JSON error occurs.
	 */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response newPatientJson(
      @QueryParam("tokenId") String tokenId,
      @Context HttpServletRequest request,
      @Context UriInfo context,
      MultivaluedMap<String, String> form) throws JSONException {
    logger.debug("@POST newPatientJson");

    AddPatientToken token = getAddPatientToken(tokenId);
    logger.info("Handling ID Request with token {}", token.getId());
    IDRequest response = addNewPatient(form, token.getFields(), token.getIds(),
        token.getRequestedIdTypes(), token.getId());
    return handleAddPatientResponse(request, context, response, token);
  }

  /**
   * Create a new patient. Interface for software applications.
   *
   * @param tokenId   Id of a valid "addPatient" token.
   * @param request   The injected HttpServletRequest.
   * @param context   Injected information of application and request URI.
   * @param inputData Input as json.
   * @return An HTTP response as specified in the API documentation.
   * @throws JSONException If a JSON error occurs.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response addPatientFromJson(
      @QueryParam("tokenId") String tokenId,
      @Context HttpServletRequest request,
      @Context UriInfo context,
      String inputData) throws JSONException {
    logger.debug("@POST addPatientFromJson");
    AddPatientToken token = getAddPatientToken(tokenId);
    logger.info("Handling ID Request with token {}", token.getId());

    // deserialize input to json
    AddPatientRequest inputDataJson = gson.fromJson(inputData, AddPatientRequest.class);

    // override input fields and external ids from Token
    Map<String, String> fields = new HashMap<>(token.getFields());
    if (inputDataJson.getFields() != null) {
      fields.putAll(inputDataJson.getFields());
    }
    Map<String, String> externalIds = new HashMap<>(token.getIds());
    if (inputDataJson.getIds() != null) {
      externalIds.putAll(inputDataJson.getIds());
    }

    // create patient
    IDRequest response = PatientBackend.instance.createAndPersistPatient(fields, externalIds,
        token.getRequestedIdTypes(), inputDataJson.isSureness(), token.getId());
    return handleAddPatientResponse(request, context, response, token);
  }

  private Response handleAddPatientResponse(HttpServletRequest request, UriInfo context,
      IDRequest response, AddPatientToken token) throws JSONException{
    //handle possible matches
		if (response.getMatchResult().getResultType() == MatchResultType.POSSIBLE_MATCH && response.createRequestedIds() == null) {
			JSONObject ret = new JSONObject();
			if (token.showPossibleMatches()) {
				JSONArray possibleMatches = new JSONArray();
				for (Entry<Double, List<Patient>> possibleMatch : response.getMatchResult().getPossibleMatches().entrySet()) {
					for (Patient p : possibleMatch.getValue())
						possibleMatches.put(p.createId(IDGeneratorFactory.instance.getDefaultIDType()).toJSON());
				}
				ret.put("possibleMatches", possibleMatches);
			}
			ret.put("message", "Unable to definitely determined whether the data refers to an existing or to a new "
					+ "patient. Please check data or resubmit with sureness=true to get a tentative result. Please check"
					+ " documentation for details.");
			return Response
					.status(Status.CONFLICT)
					.entity(ret)
					.build();
		}
		logger.debug(() -> "Accept: " + request.getHeader("Accept"));
		logger.debug(() -> "Content-Type: " + request.getHeader("Content-Type"));
		List<ID> newIds = new LinkedList<ID>(response.createRequestedIds());
		
		int apiMajorVersion = Servers.instance.getRequestMajorApiVersion(request);

    String callback = token.getDataItemString("callback");
    if (callback != null && callback.length() > 0) {
        sendCallback(request, token, newIds, null, callback);
    }

		if (apiMajorVersion >= 2) {
            String redirect = token.getDataItemString("redirect");
            if (redirect != null && redirect.length() > 0) {
                UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
                List<String> templateVariables = redirectURITempl.getTemplateVariables();
                List<String> requestedIds = RedirectUtils.getRequestedIDsTypeFromToken(token);

                if (templateVariables.contains("tokenId")) {
                    return new RedirectBuilder().setTokenId(token.getId())
                            .setMappedIdTypesdAndIds(requestedIds, response).setTemplateURI(redirectURITempl)
                            .build().execute();
                } else {
                    return new RedirectBuilder().setMappedIdTypesdAndIds(requestedIds, response).setTemplateURI(redirectURITempl)
                            .build().execute();
                }
            }

			JSONArray ret = new JSONArray();
			for (ID thisID : newIds) {
				URI newUri = context.getBaseUriBuilder()
						.path(PatientsResource.class)
						.path("/{idtype}/{idvalue}")
						.build(thisID.getType(), thisID.getEncryptedIdStringFirst());
	
				ret.put(thisID.toJSON()
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
					.put("newId", newId.getEncryptedIdStringFirst())
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
   * Get patients via "readPatient" token.
   *
   * @param tokenId Id of a valid "readPatient" token.
   * @return A JSON result as specified in the API documentation.
   */
  @Path("/tokenId/{tokenId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPatients(@PathParam("tokenId") String tokenId, @Context HttpServletRequest request) {
    logger.debug("@GET getPatients");
    logger.info("Received request to get patient with token {}", tokenId);

    // Check if token exists and has the right type.
    // Validity of token is checked upon creation
    Token token = Servers.instance.getTokenByTid(tokenId);
    if (token == null) {
      logger.info("No token with id {} found", tokenId);
      throw new InvalidTokenException("Please supply a valid 'readPatients' token.", Status.UNAUTHORIZED);
    }

    token.checkTokenType("readPatients");

    JSONArray resultAsJson = new JSONArray();
    ArrayList<Patient> patientList = new ArrayList<>();

    for (Object item : token.getDataItemList("searchIds")) {
      @SuppressWarnings("unchecked")
      Map<String, String> thisSearchId = (Map<String, String>) item;
      String idType = thisSearchId.get("idType");
      String idString = thisSearchId.get("idString");

      // find patients from database
      List<Patient> patients;
      if(idString.trim().equals("*")) {
        patients = Persistor.instance.getPatients(idType);
      } else {
        ID id;
        try {
          // Decrypt input id, if configured
          id = IDGeneratorFactory.instance.decryptAndBuildId(idType, idString);
        } catch (Exception e) {
          // if cannot decrypt id, proceed with plain id string
          logger.warn("Decryption failed, try to proceed with search id string");
          id = IDGeneratorFactory.instance.buildId(idType, idString);
        }
        if (IDGeneratorFactory.instance.getTransientIdTypes().contains(idType)){
          // if id is an instance of transient id types, find a corresponding persistent base id
          id = ((DerivedIDGenerator)IDGeneratorFactory.instance.getFactory(idType)).getBaseId(id);
        }
        patients = Collections.singletonList(Persistor.instance.getPatient(id));
      }

      // convert result to json and perform audit trail log
      for (Patient patient : patients) {
        JSONObject patientJson = patientToJson(patient, token);

        //Write audit trail
        if (Config.instance.auditTrailIsOn()) {
          AuditTrail at = PatientBackend.instance.buildAuditTrailRecord(token.getId(),
              idString,
              idType,
              "read",
              patientJson.toString(),
              null);
          Persistor.instance.createAuditTrail(at);
        }

        resultAsJson.put(patientJson);
        if (patient != null) {
          patientList.add(patient);
        }
      }
    }

    // return 404 if no patient found
    if (patientList.isEmpty()) {
      return Response.status(Status.NOT_FOUND)
          .entity("No patient found")
          .build();
    }

    // Callback
    String callback = token.getDataItemString("callback");
    if (callback != null && callback.length() > 0
        && Servers.instance.hasServerPermission(token.getParentServerName(), "callback")) {
      MainzellisteCallback mainzellisteCallback = new MainzellisteCallback();
      try {
        mainzellisteCallback.url(callback)
            .apiVersion(Servers.instance.getRequestApiVersion(request))
            .tokenId(token.getId()).addPatients(resultAsJson).build().execute();
      } catch (IOException ioe) {
        logger.error("Error while sending callback to url " + callback, ioe);
        ioe.printStackTrace();
      } catch (JSONException jsone) {
        logger.error("Couldn't serialize content for callback on url " + callback, jsone);
        jsone.printStackTrace();
      }
    }

    // Redirect
    String redirect = token.getDataItemString("redirect");
    if (redirect != null && redirect.length() > 0
        && Servers.instance.hasServerPermission(token.getParentServerName(), "redirect")) {
      UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
      List<String> templateVariables = redirectURITempl.getTemplateVariables();

      if (templateVariables.contains("tokenId")) {
        // TODO: send mapped list of requests?
        if (token.getDataItemList("searchIds").size() > 0 && patientList.size() > 0) {
          List<String> requestedIds = RedirectUtils.getRequestedIDsTypeFromToken(token);
          return new RedirectBuilder().setTokenId(token.getId())
              .setMappedIdTypesdAndIds(requestedIds, patientList.get(0))
              .setTemplateURI(redirectURITempl)
              .build().execute();
        }
      } else {
        return Response.status(HttpStatus.SC_BAD_REQUEST)
            .entity("Couldn't generate redirect because request is not valid").build();
      }
    }

    return Response.ok().entity(resultAsJson).build();
  }

  private JSONObject patientToJson(Patient patient, Token token) {
    JSONObject patientJson = new JSONObject();

    if(patient == null) {
      return patientJson;
    }

    // serialize patient fields
    if (token.hasDataItem("resultFields")) {
      List<?> requestedFieldTypes = token.getDataItemList("resultFields");
      Map<String, String> requestedFields = patient.getInputFields().entrySet().stream()
          .filter(e -> requestedFieldTypes.contains(e.getKey()))
          .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString()));

      try {
        if (!requestedFields.isEmpty()) {
          patientJson.put("fields", requestedFields);
        }
      } catch (JSONException e) {
        logger.error("Error while transforming patient fields into JSON", e);
        throw new InternalErrorException("Error while transforming patient fields into JSON");
      }
    }

    // serialize patient ids
    if (Boolean.TRUE.equals(token.getData().get("readAllPatientIds"))) {
      if (token.getParentServerName() == null) {
        throw new NoParentServerNameException();
      } else if (Servers.instance
          .hasServerPermission(token.getParentServerName(), "readAllPatientIds")) {
        try {
          List<JSONObject> foundIds = patient.getIds().stream()
              .map(ID::toJSON)
              .collect(Collectors.toList());
          if (!foundIds.isEmpty()) {
            patientJson.put("ids", foundIds);
          }
        } catch (JSONException e) {
          logger.error("Error while transforming patient ids into JSON", e);
          throw new InternalErrorException("Error while transforming patient ids into JSON");
        }
      } else {
        logger.info("Server has no readAllPatientIds permission");
        throw new UnauthorizedException("Server has no readAllPatientIds permission");
      }
    } else if (token.hasDataItem("resultIds")) {
      try {
        List<?> requestedIdTypes = token.getDataItemList("resultIds");
        Set<String> transientIdTypes = IDGeneratorFactory.instance.getTransientIdTypes();
        List<String> derivedIdTypes = (List<String>)requestedIdTypes.stream().filter(o -> (transientIdTypes.contains(o))).collect(Collectors.toList());
        if (!derivedIdTypes.isEmpty()) {
            derivedIdTypes.forEach(patient::createId);
        }
        List<JSONObject> requestedIds = patient.getAllIds().stream()
            .filter(id -> requestedIdTypes.contains(id.getType()))
            .map(ID::toJSON)
            .collect(Collectors.toList());
        if (!requestedIds.isEmpty()) {
          patientJson.put("ids", requestedIds);
        }
      } catch (JSONException e) {
        logger.error("Error while transforming patient ids into JSON", e);
        throw new InternalErrorException("Error while transforming patient ids into JSON");
      }
    }

    // serialize patient id types
    if (Boolean.TRUE.equals(token.getData().get("readAllPatientIdTypes"))) {
      if (token.getParentServerName() == null) {
        throw new NoParentServerNameException();
      } else if (Servers.instance.hasServerPermission(token.getParentServerName(),
          "readAllPatientIdTypes")) {
        try {
          patientJson.put("idTypes", new JSONArray(patient.getIds().stream()
              .map(ID::getType)
              .collect(Collectors.toList())));
        } catch (JSONException e) {
          logger.error("Error while transforming ID types into JSON", e);
          throw new InternalErrorException("Error while transforming ID types into JSON");
        }
      } else {
        logger.info("Server has no resultAllIds permission");
        throw new UnauthorizedException("Server has no readAllPatientIdTypes permission");
      }
    }
    return patientJson;
  }

    /**
     * Edit a patient. Interface for web browsers. The patient to edit is determined
     * from the given "editPatient" token.
     *
     * @param tokenId A valid "editPatient" token.
     * @param form    Input as provided by the HTML form.
     * @param request The injected HttpServletRequest.
     * @return An HTTP response as specified in the API documentation.
     */
    @Path("/tokenId/{tokenId}")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response editPatientBrowser(@PathParam("tokenId") String tokenId, MultivaluedMap<String, String> form,
                                       @Context HttpServletRequest request) {
        logger.debug("@PUT editPatientBrowser");

            try {
                // Collect fields from input form
                Map<String, String> newFieldValues = new HashMap<String, String>();
                for (String fieldName : form.keySet()) {
                    newFieldValues.put(fieldName, form.getFirst(fieldName));
                }

                EditPatientToken t = this.editPatient(tokenId, newFieldValues, request);

                if (t.getRedirect() != null) {
                    return Response.status(Status.SEE_OTHER).header("Location", t.getRedirect().toString()).build();
                }
                return Response.ok(new Viewable("/patientEdited.jsp")).build();
            } catch (WebApplicationException e) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("message", e.getResponse().getEntity());
                return Response.status(e.getResponse().getStatus()).entity(new Viewable("/errorPage.jsp", map)).build();
            }
    }

    /**
     * Edit a patient. Interface for software applications. The patient to edit is
     * determined from the given "editPatient" token.
     *
     * @param tokenId A valid "editPatient" token.
     * @param data    Input data as JSON object, keys are field names and values the
     *                respective field values.
     * @param request The injected HttpServletRequest.
     * @return An HTTP response as specified in the API documentation.
     */
    @Path("/tokenId/{tokenId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editPatientJSON(@PathParam("tokenId") String tokenId, String data,
                                    @Context HttpServletRequest request) {
        logger.debug("@PUT editPatientJSON");

        // Collect fields from input form
            try {
                JSONObject newFieldValuesJSON = new JSONObject(data);
                Map<String, String> newFieldValues = new HashMap<String, String>();
                Iterator<?> i = newFieldValuesJSON.keys();
                while (i.hasNext()) {
                    String fieldName = i.next().toString();
                    if (newFieldValuesJSON.isNull(fieldName))
                        newFieldValues.put(fieldName, "");
                    else
                        newFieldValues.put(fieldName, newFieldValuesJSON.get(fieldName).toString());
                }
                EditPatientToken token = this.editPatient(tokenId, newFieldValues, request);
                String callback = token.getDataItemString("callback");
                if (callback != null && callback.length() > 0) {
                    MainzellisteCallback mainzellisteCallback = new MainzellisteCallback();
                    logger.debug("Sending request to callback {}", callback);
                    HttpResponse httpResponse = mainzellisteCallback
                            .apiVersion(Servers.instance.getRequestApiVersion(request)).url(callback)
                            .tokenId(token.getId())
                            // TODO: Check if newIds is really okay
                            .build().execute();
                    StatusLine sline = httpResponse.getStatusLine();
                    // Accept callback if OK, CREATED or ACCEPTED is returned
                    if ((sline.getStatusCode() < 200) || sline.getStatusCode() >= 300) {
                        logger.error("Received invalid status form mdat callback: {}", httpResponse.getStatusLine());
                        throw new InternalErrorException("Request to callback failed!");
                    }
                }
                String redirect = token.getDataItemString("redirect");
                if (redirect != null && redirect.length() > 0) {
                    UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
                    List<String> templateVariables = redirectURITempl.getTemplateVariables();
                    if (templateVariables.contains("tokenId")) {
                        return new RedirectBuilder().setTokenId(token.getId()).setTemplateURI(redirectURITempl).build()
                                .execute();
                    }
                }
                return Response.status(Status.NO_CONTENT).build();
            } catch (JSONException e) {
                logger.error("Couldn't parse json in PatientResource.editPatientJSON", e);
                e.printStackTrace();
                throw new InvalidJSONException("Couldn't parse editPatient JSON");
            } catch (IOException ioe) {
                logger.error("", ioe);
                ioe.printStackTrace();
                throw new InternalErrorException("Executing post request on callback url failed!");
            }
    }

    /**
     * Handles requests to edit a patient (i.e. change IDAT fields). Methods for
     * specific media types should delegate all processing apart from converting the
     * input (e.g. form fields) to this function, including error handling for
     * invalid tokens etc.
     *
     * @param tokenId        Id of a valid editPatient token.
     * @param newFieldValues Field values to set. Fields that do not appear as map
     *                       keys are left as they are. In order to delete a field
     *                       value, provide an empty string.
     * @param request        The injected HttpServletRequest.
     * @return The token that is as authorization the patient. Used for retreiving
     * the redirect URL afterwards.
     */
    private synchronized EditPatientToken editPatient(String tokenId, Map<String, String> newFieldValues,
                                                      HttpServletRequest request) {

        Token t = Servers.instance.getTokenByTid(tokenId);
        EditPatientToken tt;
        if (t == null || !"editPatient".equals(t.getType())) {
            logger.info(() -> "Token with id " + tokenId + " "
                    + (t == null ? "is unknown." : ("has wrong type '" + t.getType() + "'")));
            throw new InvalidTokenException("Please supply a valid 'editPatient' token.", Status.UNAUTHORIZED);
        }
        // synchronize on token
        synchronized (t) {
            /*
             * Get token again and check if it still exist. This prevents the following race
             * condition: 1. Thread A gets token t and enters synchronized block 2. Thread B
             * also gets token t, now waits for A to exit the synchronized block 3. Thread A
             * deletes t and exits synchronized block 4. Thread B enters synchronized block
             * with invalid token
             */
            tt = (EditPatientToken) Servers.instance.getTokenByTid(tokenId);
            if (tt == null) {
                String infoLog = "Token with ID " + tokenId
                        + " is invalid. It was invalidated by a concurrent request or the session timed out during this request.";
                logger.info(infoLog);
                throw new WebApplicationException(Response.status(Status.UNAUTHORIZED)
                        .entity("Please supply a valid 'editPatient' token.").build());
            }

            // Form fields (union of fields and ids)
            Set<String> allowedFormFields = tt.getFields();
            if (tt.getIds() != null) {
                if (allowedFormFields != null) {
                    allowedFormFields.addAll(tt.getIds());
                } else {
                    allowedFormFields = tt.getIds();
                }
            }

            // Check that the caller is allowed to change the provided fields or ids
            if (allowedFormFields != null) {
                for (String fieldName : newFieldValues.keySet()) {
                    if (!allowedFormFields.contains(fieldName)) {
                        if (IDGeneratorFactory.instance.getExternalIdTypes().contains(fieldName)) {
                            throw new UnauthorizedException(
                                    "No authorization to edit external id " + fieldName + " with this token.");
                        } else {
                            throw new UnauthorizedException(
                                    "No authorization to edit field " + fieldName + " with this token.");
                        }
                    }
                }
            }

            PatientBackend.instance.editPatient(tt.getPatientId(), newFieldValues, tokenId);
        } // end of synchronized block

        return tt;
    }

    /**
     * Delete a patient
     *
     * @param idType              ID type of an ID of the patient to delete.
     * @param idString            ID string of an ID of the patient to delete.
     * @param withDuplicatesParam Whether to delete duplicates of the given patient.
     * @param request             The injected HttpServletRequest.
     * @return A response according to the API documentation.
     */
    @Path("{tokenId}/{idType}/{idString}")
    @DELETE
    public Response deletePatient(@PathParam("tokenId") String tokenId, @PathParam("idType") String idType,
                                  @PathParam("idString") String idString, @QueryParam("withDuplicates") String withDuplicatesParam,
                                  @Context HttpServletRequest request) {
        logger.debug("@DELETE deletePatientIDAT request");

        Token token = Servers.instance.getTokenByTid(tokenId);

            if (token == null) {
                logger.info("No token with id {} found", tokenId);
                throw new InvalidTokenException("Please supply a valid 'deletePatient' token.", Status.UNAUTHORIZED);
            }
            token.checkTokenType("deletePatient");
            boolean withDuplicates = Boolean.parseBoolean(withDuplicatesParam);

            ID id = IDGeneratorFactory.instance.buildId(idType, idString);

            if (withDuplicates) {
                List<Patient> deletedPatients = Persistor.instance.deletePatientWithDuplicates(id);
                if (Config.instance.auditTrailIsOn()) {
                    deletedPatients.forEach( p -> performAuditTrail(p, token.getId(), "delete"));
                }
            } else {
                Patient deletedPatient = Persistor.instance.deletePatient(id);
                if (Config.instance.auditTrailIsOn()) {
                    performAuditTrail(deletedPatient, token.getId(), "delete");
                }
            }
            String callback = token.getDataItemString("callback");
            if (callback != null && callback.length() > 0) {
                sendCallback(request, token, null, null, callback);
            }
            String redirect = token.getDataItemString("redirect");
            if (redirect != null && redirect.length() > 0) {
                UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
                List<String> templateVariables = redirectURITempl.getTemplateVariables();
                if (templateVariables.contains("tokenId")) {
                    return new RedirectBuilder().setTokenId(token.getId()).setTemplateURI(redirectURITempl).build()
                            .execute();
                }
            }
            return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * @param tokenId
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("checkMatch/{tokenId}")
    public Response getBestMatch(@Context HttpServletRequest request, @PathParam("tokenId")
            String tokenId, MultivaluedMap<String, String> form) throws JSONException {
        logger.debug("checkMatch tokenId: {}", tokenId);

        //TODO: add permission checks
        Token token = Servers.instance.getTokenByTid(tokenId);
        token.checkTokenType("checkMatch");

        // read input fields and external ids from FORM
        Map<String, String> inputFields = new HashMap<>();
        Map<String, String> externalIds = new HashMap<>();
        extractFieldsAndExternalIds(form, inputFields, externalIds);

        MatchResult matchResult = PatientBackend.instance.findMatch(inputFields, externalIds);
        logger.info("CheckMatch/Bestmatch score: {}", matchResult.getBestMatchedWeight());
        List<Double> similarityScores = Collections.singletonList(matchResult.getBestMatchedWeight());
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonPatientObject = new JSONObject().put("similarityScore", similarityScores.get(0));

        //TODO: IMPORTANT ADD PERMISSION CONCEPT. send only "permitted" IDs
        if(matchResult.getBestMatchedPatient()!=null){
            Set<String> transientIdTypes = IDGeneratorFactory.instance.getTransientIdTypes();
            for (Object idType : token.getDataItemList("idTypes")) {
                ID id;
                if (transientIdTypes.contains((String) idType)) {
                    id = matchResult.getBestMatchedPatient().createId((String) idType);
                } else {
                    id = matchResult.getBestMatchedPatient().getId((String) idType);
                }
                if (id != null) {
                    jsonPatientObject.put(id.getType(), id.getEncryptedIdStringFirst());
                }
            }
        }

        jsonArray.put(jsonPatientObject);

        // needs to send weight and token
        String callback = token.getDataItemString("callback");
        if (callback != null && callback.length() > 0) {
            sendCallback(request, token, null, similarityScores, callback);
        }
        // needs to send weight and token
        String redirect = token.getDataItemString("redirect");
        if (redirect != null && redirect.length() > 0) {
            UriTemplate redirectURITempl = new UriTemplate(token.getDataItemString("redirect"));
            List<String> templateVariables = redirectURITempl.getTemplateVariables();
            if (templateVariables.contains("tokenId")) {
                return new RedirectBuilder().setTokenId(token.getId()).setSimilarityScores(similarityScores).setTemplateURI(redirectURITempl).build()
                        .execute();
            }
        }

        return Response
                .status(Status.OK)
                .entity(jsonArray)
                .build();

    }

    private void sendCallback(@Context HttpServletRequest request, Token token, Collection<ID> ids, Collection<Double> similarityScores, String callback) {
        try {
            logger.debug("Sending request to callback {}", callback);

            HttpResponse httpResponse = null;

            MainzellisteCallback mainzellisteCallback = new MainzellisteCallback()
                    .apiVersion(Servers.instance.getRequestApiVersion(request)).url(callback);
            if (token.getId() != null) {
                mainzellisteCallback = mainzellisteCallback.tokenId(token.getId());
            }
            if (ids != null) {
                mainzellisteCallback = mainzellisteCallback.returnIds(ids);
            }
            if (similarityScores != null) {
                mainzellisteCallback = mainzellisteCallback.similarityScores(similarityScores);
            }
            httpResponse = mainzellisteCallback.build().execute();
            StatusLine sline = httpResponse.getStatusLine();

            // Accept callback if OK, CREATED or ACCEPTED is returned
            if ((sline.getStatusCode() < 200) || sline.getStatusCode() >= 300) {
                logger.error("Received invalid status form mdat callback: " + httpResponse.getStatusLine());
                throw new InternalErrorException("Request to callback failed!");
            }
        } catch (JSONException jsone) {
            logger.error("Couldn't serialize JSON in Callback for url " + callback, jsone);
            jsone.printStackTrace();
        } catch (IOException ioe) {
            logger.error("Couldn't execute Callback for url " + callback, ioe);
            ioe.printStackTrace();
            throw new WebApplicationException(504);
        }
    }

    /* Utils */

  public static IDRequest addNewPatient(MultivaluedMap<String, String> form,
      Map<String, String> fieldsFromToken, Map<String, String> externalIdsFromToken,
      Set<String> requestedIdTypes, String tokeId) {

    // read input fields and external ids from FORM and Token
    Map<String, String> fields = new HashMap<>(fieldsFromToken);
    Map<String, String> externalIds = new HashMap<>(externalIdsFromToken);
    extractFieldsAndExternalIds(form, fields, externalIds);

    // read sureness flag
    boolean sureness =
        form.getFirst("sureness") != null || Boolean.parseBoolean(form.getFirst("sureness"));
    return PatientBackend.instance.createAndPersistPatient(fields, externalIds, requestedIdTypes,
        sureness, tokeId);
  }

  private static void extractFieldsAndExternalIds(MultivaluedMap<String, String> form,
      Map<String, String> inputFields, Map<String, String> externalIds) {
    form.entrySet().stream()
        .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
        .forEach(e -> {
          if (IDGeneratorFactory.instance.getExternalIdTypes().contains(e.getKey())) {
            externalIds.put(e.getKey(), e.getValue().get(0));
          }
          if (Config.instance.getFieldKeys().contains(e.getKey())) {
            inputFields.put(e.getKey(), e.getValue().get(0));
          }
        });
  }

  /**
   * find if add patient token exist otherwise create a new one if started in debug mode
   * @param tokenId token id
   * @return add patient token
   * @throws InvalidTokenException if no token with the given id found
   */
  private AddPatientToken getAddPatientToken(String tokenId) {
    Token token = Servers.instance.getTokenByTid(tokenId);
    // Try to read token from session.
    if (token == null) {
      // If no token found and debug mode is on, create token, otherwise fail
      if (!Config.instance.debugIsOn()) {
        logger.error("No token with id {} found", tokenId);
        throw new InvalidTokenException("Please supply a valid 'addPatient' token.",
            Status.UNAUTHORIZED);
      }
      AddPatientToken addPatientToken = new AddPatientToken();
      Servers.instance.registerToken(getDebugSession().getId(), addPatientToken, "127.0.0.1");
      return addPatientToken;
    } else if (!(token instanceof AddPatientToken)) { // correct token type?
      logger.error("Token {} is not of type 'addPatient' but '{}'", token.getId(), token.getType());
      throw new InvalidTokenException("Please supply a valid 'addPatient' token.",
          Status.UNAUTHORIZED);
    }
    return (AddPatientToken) token;
  }

  /**
   * Get a session for use in debug mode.
   *
   * @return The debug session.
   */
  private Session getDebugSession() {
    if (debugSession == null || Servers.instance.getSession(debugSession.getId()) == null) {
      debugSession = Servers.instance.newSession("");
      try {
        debugSession.setURI(new URI("debug"));
      } catch (URISyntaxException e) {
        throw new WebApplicationException(e);
      }
    }
    return debugSession;
  }

    private void performAuditTrail(Patient patient, String tokenId,String action)
    {
        patient.getIds().forEach(id -> Persistor.instance.createAuditTrail(
                PatientBackend.instance.buildAuditTrailRecord(
                        tokenId,
                        id.getIdString(),
                        id.getType(),
                        action,
                        patient.toString(),
                        null)
        ));
    }
}
