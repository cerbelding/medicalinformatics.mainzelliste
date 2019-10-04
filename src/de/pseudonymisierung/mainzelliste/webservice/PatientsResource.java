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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import de.pseudonymisierung.mainzelliste.exceptions.*;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.spi.resource.Singleton;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Resource-based access to patients.
 */
@Path("/patients")
@Singleton
public class PatientsResource {

    /**
     * The logging instance.
     */
    private Logger logger = Logger.getLogger(PatientsResource.class);

    /**
     * Get a list of patients.
     *
     * @param req     The injected HttpServletRequest.
     * @param tokenId Id of a valid "readPatients" token.
     * @return A JSON result as specified in the API documentation.
     * @throws UnauthorizedException If no token is provided.
     */
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

        else
            throw new UnauthorizedException();
    }


    /**
     * Create a new patient. Interface for web browser.
     *
     * @param tokenId                Id of a valid "addPatient" token.
     * @param mainzellisteApiVersion The API version used to make the request.
     * @param form                   Input as provided by the HTML form.
     * @param request                The injected HttpServletRequest.
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
            Token t = Servers.instance.getTokenByTid(tokenId);
            IDRequest createRet = PatientBackend.instance.createNewPatient(tokenId, form, Servers.instance.getRequestApiVersion(request));
            Set<ID> ids = createRet.getRequestedIds();
            MatchResult result = createRet.getMatchResult();
            Map<String, Object> map = new HashMap<String, Object>();
            if (ids == null) { // unsure case
                // Copy form to JSP model so that input is redisplayed
                for (String key : form.keySet()) {
                    map.put(key, form.getFirst(key));
                }
                map.put("readonly", "true");
                map.put("tokenId", tokenId);
                map.put("mainzellisteApiVersion", mainzellisteApiVersion);
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
                        String showResult = Config.instance.getProperty("result.show");
                        if (showResult != null && !Boolean.parseBoolean(showResult)) {
                            return Response.status(Status.SEE_OTHER)
                                    .location(redirectURI)
                                    .build();
                        }
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
                    for (String key : form.keySet()) {
                        map.put(key, form.getFirst(key));
                    }
                    // and set flag for JSP to display them
                    map.put("printIdat", true);
                }

                map.put("ids", ids);

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
                return Response.ok(new Viewable("/patientCreated.jsp", map)).build();
            }
        } catch (WebApplicationException e) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("message", e.getResponse().getEntity());
            return Response.status(e.getResponse().getStatus())
                    .entity(new Viewable("/errorPage.jsp", map)).build();
        }
    }

    /**
     * Create a new patient. Interface for software applications.
     *
     * @param tokenId Id of a valid "addPatient" token.
     * @param request The injected HttpServletRequest.
     * @param context Injected information of application and request URI.
     * @param form    Input as provided by the HTTP request.
     * @return An HTTP response as specified in the API documentation.
     * @throws JSONException If a JSON error occurs.
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
        IDRequest response = PatientBackend.instance.createNewPatient(tokenId, form, Servers.instance.getRequestApiVersion(request));
        if (response.getMatchResult().getResultType() == MatchResultType.POSSIBLE_MATCH && response.getRequestedIds() == null) {
            JSONObject ret = new JSONObject();
            if (response.getToken().showPossibleMatches()) {
                JSONArray possibleMatches = new JSONArray();
                for (Entry<Double, List<Patient>> possibleMatch : response.getMatchResult().getPossibleMatches().entrySet()) {
                    for (Patient p : possibleMatch.getValue())
                        possibleMatches.put(p.getId(IDGeneratorFactory.instance.getDefaultIDType()).toJSON());
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
     * Get patients via "readPatient" token.
     *
     * @param tid Id of a valid "readPatient" token.
     * @return A JSON result as specified in the API documentation.
     */
    @Path("/tokenId/{tid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientsToken(
            @PathParam("tid") String tid) {
        logger.debug("@GET getPatientsToken");
        logger.info("Received request to get patient with token " + tid);
        // Check if token exists and has the right type. 
        // Validity of token is checked upon creation
        Token token = Servers.instance.getTokenByTid(tid);
        if (token == null) {
            logger.info("No token with id " + tid + " found");
            throw new InvalidTokenException("Please supply a valid 'readPatients' token.", Status.UNAUTHORIZED);
        }

        token.checkTokenType("readPatients");
        List<?> requests = token.getDataItemList("searchIds");

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
            if (token.hasDataItem("resultFields")) {
                // get fields for output
                Map<String, String> outputFields = new HashMap<String, String>();
                @SuppressWarnings("unchecked")
                List<String> fieldNames = (List<String>) token.getDataItemList("resultFields");
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

            if (Boolean.TRUE.equals(token.getData().get("readAllPatientIds"))) {

                if(token.getParentServerName() == null){
                    throw new NoParentServerNameException();
                }
                else if(Servers.instance.hasServerPermission(token.getParentServerName(), "readAllPatientIds"))
                {
                    try {
                        thisPatient.put("ids", getAllIDsOfPatient(patient));

                    } catch (JSONException e) {
                        logger.error("Error while transforming patient ids into JSON", e);
                        throw new InternalErrorException("Error while transforming patient ids into JSON");
                    }
                }
                else{
                    logger.info("Server has no readAllPatientIds permission");
                    throw new UnauthorizedException("Server has no readAllPatientIds permission");
                }

            } else if (token.hasDataItem("resultIds")) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> idTypes = (List<String>) token.getDataItemList("resultIds");
                    List<JSONObject> returnIds = new LinkedList<JSONObject>();
                    for (String thisIdType : idTypes) {
                        ID returnId = patient.getId(thisIdType);
                        if (returnId != null) returnIds.add(returnId.toJSON());
                    }
                    thisPatient.put("ids", returnIds);
                } catch (JSONException e) {
                    logger.error("Error while transforming patient ids into JSON", e);
                    throw new InternalErrorException("Error while transforming patient ids into JSON");
                }
            }
            if (Boolean.TRUE.equals(token.getData().get("readAllPatientIdTypes"))) {

                if(token.getParentServerName() == null){
                    throw new NoParentServerNameException();
                }
                else if (Servers.instance.hasServerPermission(token.getParentServerName(), "readAllPatientIdTypes")) {
                    try {
                        thisPatient.put("idTypes", getAllIdTypesOfPatient(patient));
                    } catch (JSONException e) {
                        logger.error("Error while transforming ID types into JSON", e);
                        throw new InternalErrorException("Error while transforming ID types into JSON");
                    }
                } else {
                    logger.info("Server has no resultAllIds permission");
                    throw new UnauthorizedException("Server has no readAllPatientIdTypes permission");
                }
            }


            ret.put(thisPatient);
        }

        return Response.ok().entity(ret).build();
    }

    private JSONArray getAllIdTypesOfPatient(Patient patient) {
        return new JSONArray(
                patient.getIds().stream().map(i -> i.getType()).collect(Collectors.toList()));
    }


    private List<JSONObject> getAllIDsOfPatient(Patient patient) {

        List<JSONObject> returnIds = patient.getIds().stream().map(i -> i.toJSON())
                .collect(Collectors.toList());
        return returnIds;
    }

    /**
     * Edit a patient. Interface for web browsers. The patient to edit is
     * determined from the given "editPatient" token.
     *
     * @param tokenId A valid "editPatient" token.
     * @param form    Input as provided by the HTML form.
     * @param request The injected HttpServletRequest.
     * @return An HTTP response as specified in the API documentation.
     */
    @Path("/tokenId/{tokenId}")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response editPatientBrowser(@PathParam("tokenId") String tokenId,
                                       MultivaluedMap<String, String> form,
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
                return Response.status(Status.SEE_OTHER)
                        .header("Location", t.getRedirect().toString())
                        .build();
            }
            return Response.ok(new Viewable("/patientEdited.jsp")).build();
        } catch (WebApplicationException e) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("message", e.getResponse().getEntity());
            return Response.status(e.getResponse().getStatus())
                    .entity(new Viewable("/errorPage.jsp", map)).build();
        }
    }

    /**
     * Edit a patient. Interface for software applications. The patient to edit
     * is determined from the given "editPatient" token.
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
    public Response editPatientJSON(@PathParam("tokenId") String tokenId,
                                    String data,
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
            this.editPatient(tokenId, newFieldValues, request);
            return Response.status(Status.NO_CONTENT).build();
        } catch (JSONException e) {
            throw new InvalidJSONException(e);
        }
    }

    /**
     * Handles requests to edit a patient (i.e. change IDAT fields). Methods for
     * specific media types should delegate all processing apart from converting
     * the input (e.g. form fields) to this function, including error handling
     * for invalid tokens etc.
     *
     * @param tokenId        Id of a valid editPatient token.
     * @param newFieldValues Field values to set. Fields that do not appear as map keys are
     *                       left as they are. In order to delete a field value, provide an
     *                       empty string.
     * @param request        The injected HttpServletRequest.
     * @return The token that is as authorization the patient. Used for retreiving the redirect URL afterwards.
     */
    private synchronized EditPatientToken editPatient(String tokenId, Map<String, String> newFieldValues, HttpServletRequest request) {

        Token t = Servers.instance.getTokenByTid(tokenId);
        EditPatientToken tt;
        if (t == null || !"editPatient".equals(t.getType())) {
            logger.info("Token with id " + tokenId + " " + (t == null ? "is unknown." : ("has wrong type '" + t.getType() + "'")));
            throw new InvalidTokenException("Please supply a valid 'editPatient' token.", Status.UNAUTHORIZED);
        }
        // synchronize on token 
        synchronized (t) {
            /* Get token again and check if it still exist.
             * This prevents the following race condition:
             *  1. Thread A gets token t and enters synchronized block
             *  2. Thread B also gets token t, now waits for A to exit the synchronized block
             *  3. Thread A deletes t and exits synchronized block
             *  4. Thread B enters synchronized block with invalid token
             */
            tt = (EditPatientToken) Servers.instance.getTokenByTid(tokenId);
            if (tt == null) {
                String infoLog = "Token with ID " + tokenId + " is invalid. It was invalidated by a concurrent request or the session timed out during this request.";
                logger.info(infoLog);
                throw new WebApplicationException(Response
                        .status(Status.UNAUTHORIZED)
                        .entity("Please supply a valid 'editPatient' token.")
                        .build());
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
                            throw new UnauthorizedException("No authorization to edit external id " + fieldName +
                                    " with this token.");
                        } else {
                            throw new UnauthorizedException("No authorization to edit field " + fieldName +
                                    " with this token.");
                        }
                    }
                }
            }

            PatientBackend.instance.editPatient(tt.getPatientId(), newFieldValues);
        } // end of synchronized block

        if (!Config.instance.debugIsOn())
            Servers.instance.deleteToken(t.getId());

        return tt;
    }

    /**
     * Delete a patient
     *
     * @param idType
     *            ID type of an ID of the patient to delete.
     * @param idString
     *            ID string of an ID of the patient to delete.
     * @param withDuplicatesParam
     *            Whether to delete duplicates of the given patient.
     * @param request
     *            The injected HttpServletRequest.
     * @return A response according to the API documentation.
     */
    @Path("{tokenId}/{idType}/{idString}")
    @DELETE
    public Response deletePatient(@PathParam("tokenId") String tokenId, @PathParam("idType") String idType, @PathParam("idString") String idString,
                                  @QueryParam("withDuplicates") String withDuplicatesParam, @Context HttpServletRequest request) {
        logger.debug("@DELETE deletePatientIDAT request");

        Token token = Servers.instance.getTokenByTid(tokenId);

        if (token == null) {
            logger.info("No token with id " + tokenId + " found");
            throw new InvalidTokenException("Please supply a valid 'deletePatient' token.", Status.UNAUTHORIZED);
        }
        token.checkTokenType("deletePatient");
        boolean withDuplicates = Boolean.parseBoolean(withDuplicatesParam);

        ID id = IDGeneratorFactory.instance.buildId(idType, idString);

        if (withDuplicates){
            Persistor.instance.deletePatientWithDuplicates(id);
        }
        else{
            Persistor.instance.deletePatient(id);
        }

        return Response.status(Status.NO_CONTENT).build();
    }

}
