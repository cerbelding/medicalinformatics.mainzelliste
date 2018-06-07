package de.sessionTokenSimulator;

import de.pseudonymisierung.mainzelliste.*;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.matcher.BloomFilterTransformer;
import de.securerecordlinkage.CommunicatorResource;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.Base64;
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
                    Field<?> field = p.getFields().get(fieldKey);
                    Field<?> resultField;

                    if ((field instanceof PlainTextField) || (field instanceof CompoundField)) {
                        if (config.getProperty(FieldName) != null && config.getProperty(FieldName)
                                                                              .contains("Decomposer")) {
                            String fieldValue = String.valueOf(p.getFields().get(fieldKey));
                            fieldValue = fieldValue.substring(1, fieldValue.length() - 2);
                            fieldValue = fieldValue.replaceAll(",", "");
                            field = new PlainTextField(fieldValue.trim());
                        }

                        if (field != null && !field.isEmpty()) {
                            BloomFilterTransformer transformer = new BloomFilterTransformer();
                            resultField = transformer.transform((PlainTextField) field);
                            byte[] encodedBytes = Base64.getEncoder().encode(resultField.getValue().toString().getBytes());
                            resultField = new PlainTextField(new String(encodedBytes));
                        }
                        else {
                            resultField = field;
                        }
                    } else {
                        resultField = field;
                    }

                    fields.put(fieldKey, resultField.getValue());
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

    public int updatePatient(JSONObject patient) {
        logger.info("updatePatient(" + patient + ")");
        boolean updateSuccessful = true;

        if (updateSuccessful == true) {
            return 200;
        } else {
            return 500;
        }
    }
}
