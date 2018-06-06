package de.securerecordlinkage;

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
import java.util.List;

//TODO: Verify against APIkey
//TODO: Extract PatientRecords class, to use this class independent of Mainzelliste
@Path("Communicator")
public class CommunicatorResource {

    private Logger logger = Logger.getLogger(this.getClass());

    // 2a. Re-send linkRecord to SRL
    // 2b. Process callback from SRL (linkRecord)
    // 4a. Process Request from SRL (getAllRecords)
    // 4b. Send Request to ML to get all records
    // 4c. Send all Records to SRL

    //TODO: from config
    private int pageSize = 50;
    private int toDate = 0;
    private int page = 1;

    private String requestedIDType = "SRL1";
    private String baseCommunicatorURL = "http://localhost:8079/Communicator/getAllRecords";

    // Read config with SRL links to know where to send the request
    public void init() {

    }

    //-----------------------------------------------------------------------

    /**
     * send linkRecord, which should be linked, to SRL - In Architectur-XML (v6) step 2
     */
    public void sendLinkRecord(JSONObject recordAsJson) {
        logger.info("sendLinkRecord");

        SendHelper.doRequest("http://localhost:8079/Communicator/linkCallBack", "POST", recordAsJson.toString());

    }

    /**
     * rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7
     */
    @POST
    @Path("/linkCallBack")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLinkRecord(@Context HttpServletRequest req, String json) {
        logger.info("/linkCallBack called");
        logger.info("setLinkRecord");

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

    //-----------------------------------------------------------------------

    /**
     * return all entrys, which schould be compared, to SRL  - In Architectur-XML (v6) step 4
     */
    @GET
    @Path("/getAllRecords")
    //@Produces(MediaType.APPLICATION_JSON)
    public Response getAllRecords(@Context HttpServletRequest req, @Context UriInfo info) {
        logger.info("getAllRecords");
        if (!authorizationValidator(req)) {
            return Response.status(401).build();
        } else {
            try {
                logger.info("Query parameters: " + info.getQueryParameters());

                setQueryParameter(info.getQueryParameters().get("page"), info.getQueryParameters().get("pagesize"), info.getQueryParameters().get("todate"), info.getQueryParameters().get("requestedIDType"));
                PatientRecords records = new PatientRecords();

                return Response.ok(prepareReturnDataSet(records.readAllPatientsAsArray()), MediaType.APPLICATION_JSON).build();
                //return Response.ok(records.readAllPatientsAsArray(), MediaType.APPLICATION_JSON).build();
            } catch (Exception e) {
                logger.error("gerAllRecords failed. " + e.toString());
                return Response.status(500).build();
            }
        }
    }

    //TODO: still necessary?
    // Process callback
    public void processCallback() {
        // Implemented in Class editID
    }

    //----Helper functions ---------------------------------------------
    private boolean authorizationValidator(HttpServletRequest request) {

        logger.info("authorizationValidator " + "validate ApiKey");
        //TODO: get authKey from Config
        String authKey = "123abc";
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

    }

    private JSONObject prepareReturnDataSet(JSONArray records) throws JSONException {
        //TODO: handle page 0 and > last page

        logger.info("prepareReturnDataSet");

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
        int lastPage = ((int) Math.ceil(Double.valueOf(records.length()) / Double.valueOf(pageSize)));
        if ((page - 1) > 0) {
            minPage = (page - 1);
        }

        selfObject.put("href", baseCommunicatorURL);
        firstObject.put("href", baseCommunicatorURL + "?" + "page=" + 1 + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        prevObject.put("href", baseCommunicatorURL + "?" + "page=" + minPage + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        nextObject.put("href", baseCommunicatorURL + "?" + "page=" + (page + 1) + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);
        lastObject.put("href", baseCommunicatorURL + "?" + "page=" + lastPage + "&" + "pageSize=" + pageSize + "&" + "toDate=" + toDate);

        linkObject.setEscapeForwardSlashAlways(false);

        linkObject.put("self", selfObject);
        linkObject.put("first", firstObject);
        linkObject.put("prev", prevObject);
        linkObject.put("next", nextObject);
        linkObject.put("last", lastObject);

        answerObject.put("_links", linkObject);

        answerObject.put("total", records.length());
        answerObject.put("currentPageNumber", page);
        answerObject.put("lastPageNumber", (int) Math.ceil(Double.valueOf(records.length()) / Double.valueOf(pageSize)));
        answerObject.put("pageSize", pageSize);
        answerObject.put("toDate", toDate);

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


        return answerObject;

    }

    /**
     * Function sets Parameter which can be used to query the records
     *
     * @param newPage
     * @param newPageSize
     * @param newToDate
     */
    private void setQueryParameter(List<String> newPage, List<String> newPageSize, List<String> newToDate, List<String> newRequestedIDType) {
        logger.info("setQueryParameter: " + "newPage: " + newPage + ", newPageSize: " + newPageSize + ", newToDate" + newToDate + ", newRequestedIDType" + newRequestedIDType);

        if (newPage != null) {
            if (newPage.get(0).matches("[0-9]+") == true) {
                logger.debug("set newPage to: " + newPage);
                page = Integer.valueOf(newPage.get(0));
            } else {
                logger.info("newPage contains not only numbers: " + newPage);
            }
        } else {
            logger.debug("pageNumber is not sent via request. Using default value: " + page);
        }

        if (newPageSize != null) {
            if (newPageSize.get(0).matches("[0-9]+") == true) {
                logger.debug("set newPageSize to: " + newPageSize);
                pageSize = Integer.valueOf(newPageSize.get(0));
            } else {
                logger.info("newPageSize contains not only numbers: " + newPageSize);
            }
        } else {
            logger.debug("pageSize is not sent via request. Using default value: " + page);
        }

        if (newToDate != null) {
            if (newToDate.get(0).matches("[0-9]+") == true) {
                logger.debug("set newToDate to: " + newToDate);
                toDate = Integer.valueOf(newToDate.get(0));
            } else {
                logger.info("newToDate contains not only numbers: " + newToDate);
            }
        } else {
            logger.debug("toDate is not sent via request. Using default value: " + toDate);
        }

        if (newRequestedIDType != null) {

            logger.debug("set newRequestedIDType to: " + newRequestedIDType);
            requestedIDType = newRequestedIDType.get(0);

        } else {
            logger.debug("newRequestedIDType is not sent via request. Using default value: " + requestedIDType);
        }


    }

    //----Dummy implementation ------------------------------------------

    //Temporal object, just for developing purpose
    private JSONObject jsondummy() {
        JSONObject reqObject = new JSONObject();
        JSONObject tmpObj = new JSONObject();
        try {
            tmpObj.put("authType", "apiKey");
            tmpObj.put("sharedKey", "123abc");
            reqObject.put("localAuthentification", tmpObj);
        } catch (Exception e) {
            logger.info("jsondummy exception" + e.getMessage());
        }

        return reqObject;
    }

}
