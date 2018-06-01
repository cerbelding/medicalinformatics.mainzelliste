package de.securerecordlinkage;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class Communicator {
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

    }

    /** rest endpoint, used to set a linked record - In Architectur-XML (v6) step 7*/
    @PUT
    @Path("/linkCallBack")
    @Produces(MediaType.APPLICATION_JSON)
    public Produces setLinkRecord(){

        return null;
    }


    //-----------------------------------------------------------------------

    /** return all entrys, which schould be compared, to SRL  - In Architectur-XML (v6) step 4*/
    @GET
    @Path("getAllRecords")
    @Produces(MediaType.APPLICATION_JSON)
    public Produces getAllRecords(){

        return null;
    }






    // Process callback
    public void processCallback() {
        // Implemented in Class editID
    }

}
