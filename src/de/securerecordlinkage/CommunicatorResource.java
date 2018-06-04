package de.securerecordlinkage;

import de.sessionTokenSimulator.PatientRecords;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//TODO: Verify against APIkey
@Path("Communicator")
public class CommunicatorResource {

    private Logger logger = Logger.getLogger(this.getClass());

    // 2a. Re-send linkRecord to SRL
    // 2b. Process callback from SRL (linkRecord)
    // 4a. Process Request from SRL (getAllRecords)
    // 4b. Send Request to ML to get all records
    // 4c. Send all Records to SRL

    // Read config with SRL links to know where to send the request
    public void init() {

    }

    //-----------------------------------------------------------------------

    /** send linkRecord, which should be linked, to SRL - In Architectur-XML (v6) step 2*/
    public void sendLinkRecord(JSONObject recordAsJson) {
        logger.info("sendLinkRecord");

        SendHelper.doRequest("http://localhost:8079/Communicator/linkCallBack", "POST", recordAsJson.toString());

    }

    /** rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7*/
    @POST
    @Path("/linkCallBack")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLinkRecord(@Context HttpServletRequest req, String json) {
        logger.info("/linkCallBack called");
        logger.info("setLinkRecord");
        try {
            JSONObject newLinkRecord = new JSONObject(json);
            PatientRecords updatePatient = new PatientRecords();
            return Response.status(updatePatient.updatePatient(newLinkRecord)).build();
        } catch (Exception e) {
            logger.error("setLinkRecord failed. " + e.toString());
            return Response.status(500).build();
        }
    }

    //-----------------------------------------------------------------------

    /** return all entrys, which schould be compared, to SRL  - In Architectur-XML (v6) step 4*/
    @GET
    @Path("/getAllRecords")
    //@Produces(MediaType.APPLICATION_JSON)
    public Response getAllRecords() {
        logger.info("getAllRecords");

        try {
            PatientRecords records = new PatientRecords();
            return Response.ok(records.readAllPatients(), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.error("gerAllRecords failed. " + e.toString());
            return Response.status(500).build();
        }
    }


    // Process callback
    public void processCallback() {
        // Implemented in Class editID
    }


    //----Dummy implementation ------------------------------------------

    //Temporal object, just for developing purpose
    private JSONObject jsondummy(){
        JSONObject reqObject = new JSONObject();
        JSONObject tmpObj = new JSONObject();
        try {
            tmpObj.put("authType", "apiKey");
            tmpObj.put("sharedKey", "123abc");
            reqObject.put("localAuthentification", tmpObj);
        }catch (Exception e){
            logger.info("jsondummy exception" + e.getMessage());
        }

        return  reqObject;
    }

}
