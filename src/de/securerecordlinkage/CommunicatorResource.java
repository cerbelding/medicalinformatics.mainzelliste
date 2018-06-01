package de.securerecordlinkage;

import de.sessionTokenSimulator.PatientRecords;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
    public void sendLinkRecord() {
        logger.info("sendLinkRecord");

    }

    /** rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7*/
    @PUT
    @Path("/linkCallBack")
    @Produces(MediaType.APPLICATION_JSON)
    public Produces setLinkRecord(){
        logger.info("setLinkRecord");

        return null;
    }


    //-----------------------------------------------------------------------

    /** return all entrys, which schould be compared, to SRL  - In Architectur-XML (v6) step 4*/
    @GET
    @Path("/getAllRecords")
    //@Produces(MediaType.APPLICATION_JSON)
    public JSONObject getAllRecords(){
        logger.info("getAllRecords");
        PatientRecords records = new PatientRecords();
        return records.readAllPatients();
    }


    // Process callback
    public void processCallback() {
        // Implemented in Class editID
    }

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
