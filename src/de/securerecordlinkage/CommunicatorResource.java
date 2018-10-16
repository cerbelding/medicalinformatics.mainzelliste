package de.securerecordlinkage;

import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.securerecordlinkage.initializer.Config;
import de.sessionTokenSimulator.PatientRecords;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

//TODO: Verify against APIkey
//TODO: Extract PatientRecords class, to use this class independent of Mainzelliste
@Path("Communicator")
public class CommunicatorResource {

    private static Logger logger = Logger.getLogger("de.securerecordlinkage.CommunicatorResource");

    // 2a. Re-send linkRecord to SRL
    // 2b. Process callback from SRL (linkRecord)
    // 4a. Process Request from SRL (getAllRecords)
    // 4b. Send Request to ML to get all records
    // 4c. Send all Records to SRL

    //TODO: from config
    private int pageSize = 50;
    private int toDate = 0;
    private int page = 1;

    private static String localId;
    private static String remoteId;
    private static String baseCommunicatorURL = "http://localhost:8082/";
    private static String localCallbackLinkURL = "http://localhost:8082/Communicator/linkCallBack";
    private static String localCallbackMatchURL = "http://localhost:8082/Communicator/matchCallBack";
    private static String localDataServiceURL = "http://localhost:8082/Communicator/getAllRecords";
    private static List<String> apiKey = new ArrayList<>();
    private static String authenticationType = "apiKey";

    public static String linkRequestURL = "http://192.168.0.101:8080/linkRecord/dkfz";
    public static String linkAllRequestURL = "http://192.168.0.101:8080/linkRecords/dkfz";

    // Read config with SRL links to know where to send the request
    //TODO: make init non static to communicate with X partners
    public static void init(Config config, String id) {
        logger.info("Load config variables for communicator");

        localId = config.getLocalID();
        remoteId = id;
        baseCommunicatorURL = config.getServers().get(remoteId).getUrl();
        localCallbackLinkURL = config.getLocalCallbackLinkUrl();
        localCallbackMatchURL = config.getLocalCallbackMatchUrl();
        localDataServiceURL = config.getLocalDataServiceUrl();
        apiKey.add(config.getLocalApiKey());
        authenticationType = config.getLocalAuthenticationType();

        logger.info("remoteID: " + remoteId + " baseCommunicatorURL: " + localDataServiceURL);

    }

    //-----------------------------------------------------------------------

    /**
     * send linkRecord, which should be linked, to SRL - In Architectur-XML (v6) step 2
     */
    //TODO: change idType and idString to map params
    public void sendLinkRecord(String url, String idType, String idString, JSONObject recordAsJson) {
        logger.info("sendLinkRecord");
        try {
            JSONObject recordToSend = new JSONObject();
            JSONObject callbackObj = new JSONObject();
            callbackObj.setEscapeForwardSlashAlways(false);
            callbackObj.put("url", localCallbackLinkURL + "?idType=" + idType + "&" + "idString=" + idString);
            recordToSend.put("callback", callbackObj);
            recordToSend.put("fields", recordAsJson.get("fields"));
            SendHelper.doRequest(url, "POST", recordToSend.toString());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * send linkRecord, which should be linked, to SRL - In Architectur-XML (v6) step 2
     */
    //TODO: change idType and idString to map params
    public void sendLinkRecords(String url, String idType, JSONArray recordsAsJson) {
        logger.info("sendLinkRecords");
        try {
            JSONObject recordToSend = new JSONObject();
            JSONObject callbackObj = new JSONObject();
            callbackObj.setEscapeForwardSlashAlways(false);
            callbackObj.put("url", localCallbackLinkURL + "?idType=" + idType);
            recordToSend.put("callback", callbackObj);
            recordToSend.put("total", recordsAsJson.length());
            recordToSend.put("toDate", toDate);
            recordToSend.put("fields", recordsAsJson);
            SendHelper.doRequest(url, "POST", recordToSend.toString());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * send matchRecord, which should be linked, to SRL - In Architectur-XML Demostrator (Prozess M) step 1
     */
    public void sendMatchRecord(String url, JSONObject recordAsJson) {
        logger.info("sendMatchRecord");
        try {
            JSONObject recordToSend = new JSONObject();
            JSONObject callbackObj = new JSONObject();
            callbackObj.setEscapeForwardSlashAlways(false);
            callbackObj.put("url", localCallbackMatchURL);
            recordToSend.put("callback", callbackObj);
            recordToSend.put("fields", recordAsJson.get("fields"));
            SendHelper.doRequest(url, "POST", recordToSend.toString());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7
     */
    @POST
    @Path("/linkCallback")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLinkRecord(@Context HttpServletRequest req, String json) {
        logger.info("/linkCallBack called");
        logger.info("setLinkRecord()");

        if (!authorizationValidator(req)) {
            return Response.status(401).build();
        } else {
            //APIKey correct, now do the work
            try {
                JSONObject newLinkRecord = new JSONObject(json);
                //TODO: Maybe as an Interface implementation, to unbind from Mainzelliste
                PatientRecords updatePatient = new PatientRecords();
                return Response.status(updatePatient.updatePatient(newLinkRecord)).build();
            } catch (Exception e) {
                logger.error("setLinkRecord failed. " + e.toString());
                return Response.status(500).build();
            }
        }
    }

    /**
     * rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7
     */
    @POST
    @Path("/matchCallback/{remoteID}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMatchResult(@Context HttpServletRequest req, String json, @PathParam("remoteID") String remoteID) {
        logger.info("/matchCallBack called");
        logger.info("addMatchResult");
        logger.info("request: " + req.getQueryString());
        logger.info("json: " + json);

        if (!authorizationValidator(req)) {
            return Response.status(401).build();
        } else {
            //APIKey correct, now do the work
            try {

                //if(json.match==true)
                // Call countMatchResult
                if (json.contains("\"match\":true")){
                    logger.info("matchCallback: match=" + true);
                    MatchCounter.incrementNumMatch(remoteID);
                }
                else{
                    //Count nonMatches
                    logger.info("matchCallback: non match");
                    MatchCounter.incrementNumNonMatch(remoteID);
                }

                if(json.contains("\"tentativeMatch\":true")){
                    logger.info("matchCallback: tentativeMatch=" + true);
                    TentativeMatchCounter.incrementNumMatch(remoteID);
                }


                //{"result":{"match":false,"tentativeMatch":true}}

                return Response.status(200).build();
            } catch (Exception e) {
                logger.error("addMatchResult failed. " + e.toString());
                return Response.status(500).build();
            }
        }
    }

    //-----------------------------------------------------------------------

    /**
     * return all entrys, which schould be compared, to SRL  - In Architectur-XML (v6) step 4
     */
    //TODO: make remoteID optional and if not set give answers back without ids
    @GET
    @Path("/getAllRecords/{remoteID}")
    //@Produces(MediaType.APPLICATION_JSON)
    public Response getAllRecords(@Context HttpServletRequest req, @Context UriInfo info, @PathParam("remoteID") String remoteID) {
        logger.info("getAllRecords()");
        if (!authorizationValidator(req)) {
            return Response.status(401).build();
        } else {

            try {
                logger.info("Query parameters: " + info.getQueryParameters());
                logger.info("Path parameters: " + remoteID);

                setQueryParameter(info.getQueryParameters().get("page"), info.getQueryParameters().get("pageSize"), info.getQueryParameters().get("toDate"));
                PatientRecords records = new PatientRecords();

                return Response.ok(prepareReturnDataSet(records.readAllPatientsAsArray(), remoteID), MediaType.APPLICATION_JSON).build();
                //return Response.ok(records.readAllPatientsAsArray(), MediaType.APPLICATION_JSON).build();
            } catch (Exception e) {
                logger.error("getAllRecords failed. " + e.toString());
                return Response.status(500).build();
            }
        }
    }

    //----Helper functions ---------------------------------------------
    private boolean authorizationValidator(HttpServletRequest request) {

        Map<String, List<String>> allowedAuthTypesAndValues = new HashMap<>();

        allowedAuthTypesAndValues.put(authenticationType, apiKey);

        AuthorizationValidator authorizationValidator = new AuthorizationValidator(allowedAuthTypesAndValues);
        return authorizationValidator.validate(request);



        /*
        logger.info("authorizationValidator() " + "validate ApiKey");
        //TODO: get authKey from Config
        String authKey = apiKey;
        String authHeader;

        try {
            authHeader = request.getHeader("Authorization");
        } catch (Exception e) {
            logger.error("Failed getting Authorization Header. " + e.toString());
            return false;
        }

        if (authHeader == null) {
            logger.info("Can't find ApiKey in request authHeader==null");
            return false;
        }

        if (authHeader.equals(authKey)) {
            logger.info("ApiKey correct");
            return true;
        } else {
            logger.info("Wrong ApiKey!");
            return false;
        }
        */

    }

    private JSONObject prepareReturnDataSet(JSONArray records, String remoteID) throws JSONException {
        //TODO: handle page 0 and > last page

        logger.info("prepareReturnDataSet()");

        JSONObject answerObject = new JSONObject();

        JSONObject linkObject = new JSONObject();

        JSONObject selfObject = new JSONObject();
        JSONObject firstObject = new JSONObject();
        JSONObject prevObject = new JSONObject();
        JSONObject nextObject = new JSONObject();
        JSONObject lastObject = new JSONObject();

        selfObject.setEscapeForwardSlashAlways(false);
        firstObject.setEscapeForwardSlashAlways(false);
        prevObject.setEscapeForwardSlashAlways(false);
        nextObject.setEscapeForwardSlashAlways(false);
        lastObject.setEscapeForwardSlashAlways(false);

        //Create URLs for paging navigation and add to JSON
        int minPage = 1;
        int lastPage = ((int) Math.ceil((double) records.length() / (double) pageSize));
        if ((page - 1) > 0) {
            minPage = (page - 1);
        }

        selfObject.put("href", baseCommunicatorURL + "/" + remoteID);
        firstObject.put("href", baseCommunicatorURL + "/" + remoteID + "?" + "page=" + 1 + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        prevObject.put("href", baseCommunicatorURL + "/" + remoteID + "?" + "page=" + minPage + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        nextObject.put("href", baseCommunicatorURL + "/" + remoteID + "?" + "page=" + (page + 1) + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        lastObject.put("href", baseCommunicatorURL + "/" + remoteID + "?" + "page=" + lastPage + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);

        linkObject.setEscapeForwardSlashAlways(false);

        linkObject.put("self", selfObject);
        linkObject.put("first", firstObject);
        linkObject.put("prev", prevObject);
        linkObject.put("next", nextObject);
        linkObject.put("last", lastObject);

        answerObject.put("_links", linkObject);

        answerObject.put("total", records.length());
        answerObject.put("currentPageNumber", page);
        answerObject.put("lastPageNumber", (int) Math.ceil((double) records.length() / (double) pageSize));
        answerObject.put("pageSize", pageSize);
        answerObject.put("toDate", toDate);

        answerObject.put("localId", localId);
        answerObject.put("remoteId", remoteId);

        //Add record entrys for specific paging request
        if (page > 0 && page <= lastPage) {

            int lastReturnedEntry = (pageSize * (page - 1)) + pageSize - 1;
            int actualEntry = (pageSize * (page - 1));
            JSONArray recordsToReturn = new JSONArray();
            do {
                if (actualEntry < records.length()) {
                    recordsToReturn.put(records.getJSONObject(actualEntry));
                }
                actualEntry++;

            } while (actualEntry <= lastReturnedEntry);

            answerObject.put("records", recordsToReturn);
        }
        //not possible to use, because myArrayList is private answerObject.put("records", records.myArrayList.subList((pageSize*page),(pageSize*page)+pageSize));
        //return the whole list
        //answerObject.put("records", records);

        logger.info("send DataSet");

        return answerObject;

    }

    /**
     * Function sets Parameter which can be used to query the records
     *
     * @param newPage new value for page number.
     * @param newPageSize new value for pageSize - how many records each site max. includes
     * @param newToDate new value for toDate - maximum time of the newest entry (TODO: not implemented yet)
     */
    private void setQueryParameter(List<String> newPage, List<String> newPageSize, List<String> newToDate) {
        logger.info("setQueryParameter(): " + "newPage: " + newPage + ", newPageSize: " + newPageSize + ", newToDate" + newToDate);

        if (newPage != null) {
            if (newPage.get(0).matches("[0-9]+")) {
                logger.debug("set newPage to: " + newPage);
                page = Integer.valueOf(newPage.get(0));
            } else {
                logger.info("newPage contains not only numbers: " + newPage);
            }
        } else {
            logger.debug("pageNumber is not sent via request. Using default value: " + page);
        }

        if (newPageSize != null) {
            if (newPageSize.get(0).matches("[0-9]+")) {
                logger.debug("set newPageSize to: " + newPageSize);
                pageSize = Integer.valueOf(newPageSize.get(0));
            } else {
                logger.info("newPageSize contains not only numbers: " + newPageSize);
            }
        } else {
            logger.debug("pageSize is not sent via request. Using default value: " + page);
        }

        if (newToDate != null) {
            if (newToDate.get(0).matches("[0-9]+")) {
                logger.debug("set newToDate to: " + newToDate);
                toDate = Integer.valueOf(newToDate.get(0));
            } else {
                logger.info("newToDate contains not only numbers: " + newToDate);
            }
        } else {
            logger.debug("toDate is not sent via request. Using default value: " + toDate);
        }

    }

    //TODO: search a better place and add return http statuscode
    @GET
    @Path("/triggerMatch/{remoteID}")
    public Response triggerMatch(@PathParam("remoteID") String remoteID) throws JSONException {

        logger.info("trigger matcher started");
        logger.info("trigger matcher " + remoteID);

        JSONObject answerObject = new JSONObject();

        //TODO: PatientRecords should use a generic interface, so we don't have to use a specific PatientRecords object here
        PatientRecords pr = new PatientRecords();
        Integer totalAmount = pr.matchPatients(remoteID);

        answerObject.put("totalAmount", totalAmount);
        MatchCounter.setNumAll(remoteID, totalAmount);
        return Response.ok(answerObject, MediaType.APPLICATION_JSON).build();
    }

    // Find better name
    // Triggers the first link process (M:N) for two patient list instances
    // Call only once, if repeated, the SRL IDs should be first deleted
    @GET
    @Path("/triggerLink/{remoteID}")
    public Response triggerLink(@PathParam("remoteID") String remoteID) throws JSONException {
        try {
            logger.info("trigger linker started");
            logger.info("trigger linker " + remoteID);

            JSONObject answerObject = new JSONObject();

            PatientRecords pr = new PatientRecords();
            Integer totalAmount = pr.linkPatients(remoteID);

            answerObject.put("totalAmount", totalAmount);
            return Response.ok(answerObject, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.error("SRL IDs cannot be generated: " + e.toString());
            return Response.status(500).build();
        }
    }

    @GET
    @Path("/triggerMatch/status/{remoteID}")
    public Response triggerMatchStatus(@PathParam("remoteID") String remoteID) throws JSONException {

        logger.info("triggerMatchStatus requested for remoteID: " + remoteID);

        JSONObject answerObject = new JSONObject();
        answerObject.put("totalAmount", MatchCounter.getNumAll(remoteID));
        answerObject.put("totalMatches", MatchCounter.getNumMatch(remoteID));
        answerObject.put("totalTentativeMatches", TentativeMatchCounter.getNumMatch(remoteID));

        logger.info("triggerMatchStatus response: " + answerObject);

        try {
            answerObject.put("matchingStatus", "in progress");
            if(MatchCounter.getNumMatch(remoteID) + MatchCounter.getNumNonMatch(remoteID) >= MatchCounter.getNumAll(remoteID)){
                answerObject.put("matchingStatus", "finished");
            }
            logger.info("getNumMatch:" + MatchCounter.getNumMatch(remoteID) + " getNumNonMatch: " + MatchCounter.getNumNonMatch(remoteID) + " getNumAll: " + MatchCounter.getNumAll(remoteID));

            logger.info("triggerMatchStatus (with progress status) response: " + answerObject);
        } catch (JSONException e) {
            logger.info("matchingStatus could not be set");
            logger.error(e.getMessage());
        }


        return Response.ok(answerObject, MediaType.APPLICATION_JSON).build();
    }

}
