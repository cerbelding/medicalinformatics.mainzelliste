package de.sessionTokenSimulator;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.securerecordlinkage.CommunicatorResource;
import de.pseudonymisierung.mainzelliste.Config;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;

// 1. Read all Patients and save in Communicator Format
// 2. Encode the fields with BloomFilter

public class PatientRecords {

    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * public JSONObject readAllPatients() {
     * JSONObject reqObject = new JSONObject();
     * JSONArray array = new JSONArray();
     * try{
     * List<Patient> patientList = Persistor.instance.getPatients();
     * for (Patient p : patientList) {
     * JSONObject fields = new JSONObject();
     * for (String fieldKey : p.getFields().keySet()) {
     * fields.put(fieldKey, p.getFields().get(fieldKey));
     * }
     * JSONObject ids = new JSONObject();
     * for (ID id : p.getIds()) {
     * ids.put(id.getType(), id.getIdString());
     * }
     * JSONObject tmpObject = new JSONObject();
     * tmpObject.put("fields", fields);
     * tmpObject.put("ids", ids);
     * array.put(tmpObject);
     * }
     * reqObject.put("records", array);
     * } catch (Exception e) {
     * logger.info(e);
     * }
     * <p>
     * return reqObject;
     * }
     **/

    public JSONArray readAllPatientsAsArray() {

        Config config = Config.instance;
        JSONArray array = new JSONArray();
        try {
            List<Patient> patientList = Persistor.instance.getPatients();
            for (Patient p : patientList) {
                JSONObject fields = new JSONObject();
                for (String fieldKey : p.getFields().keySet()) {
                    String FieldName = "field." + fieldKey + ".transformers";
                    if (!config.getProperty(FieldName).isEmpty()) {
                        if (config.getProperty(FieldName).contains("Decomposer")) {
                            String fieldValue = String.valueOf(p.getFields().get(fieldKey));
                            fieldValue = fieldValue.substring(1, fieldValue.length()-2);
                            fieldValue = fieldValue.replaceAll(",","");
                            fields.put(fieldKey, fieldValue.trim());
                        }
                    } else {
                        fields.put(fieldKey, p.getFields().get(fieldKey));
                    }
                }
                JSONObject tmpObject = new JSONObject();
                tmpObject.put("fields", fields);
                array.put(tmpObject);
            }

        } catch (Exception e) {
            logger.info(e);
        }

        return array;
    }

    public void linkPatient(Patient p, String IDType, String IDString) {
        try {
            JSONObject recordAsJSON = new JSONObject();
            JSONObject tmpObj = new JSONObject();
            tmpObj.put("IDType", IDType);
            tmpObj.put("IDString", IDString);
            recordAsJSON.put("id", tmpObj);

            JSONObject fields = new JSONObject();
            for (String fieldKey : p.getFields().keySet()) {
                fields.put(fieldKey, p.getFields().get(fieldKey));
            }
            recordAsJSON.put("fields", fields);
            CommunicatorResource rs = new CommunicatorResource();
            rs.sendLinkRecord(recordAsJSON);

        } catch (Exception e) {
            logger.info(e);
        }
    }

    public int updatePatient(JSONObject patient){
        logger.info("updatePatient(" + patient + ")");
        boolean updateSuccessful = true;

        if(updateSuccessful==true){
            return 200;
        }
        else{
            return 500;
        }
    }
}
