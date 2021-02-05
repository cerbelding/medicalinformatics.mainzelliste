package de.sessionTokenSimulator;

import de.pseudonymisierung.mainzelliste.*;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.matcher.BloomFilterTransformer;
import de.securerecordlinkage.CommunicatorResource;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

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
        JSONArray array = new JSONArray();
        try {
            List<Patient> patientList = Persistor.instance.getPatients();
            for (Patient p : patientList) {
                JSONObject tmpObject = new JSONObject();
                tmpObject.put("fields", getFieldsObject(p));
                // TODO: use real IDs
                tmpObject.put("id", getRandomID());
                array.put(tmpObject);
            }

            //IDGeneratorFactory.instance.getSrlIdTypes();
        } catch (Exception e) {
            logger.info(e);
        }

        return array;
    }

    public void linkPatient(Patient p, String IDType, String IDString) {
        try {
            JSONObject recordAsJSON = new JSONObject();
            recordAsJSON.put("fields", getFieldsObject(p));
            CommunicatorResource rs = new CommunicatorResource();
            rs.sendLinkRecord(IDType, IDString, recordAsJSON);
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

    public String getRandomID(){
        //TODO: only for testing, use real IDs
        Random rand = new Random();
        int  n = rand.nextInt(50000) + 1;
        return String.valueOf(n);
    }

    public JSONObject getFieldsObject(Patient p) {
        Config config = Config.instance;
        JSONObject fields = new JSONObject();
        Base64.Encoder encoder = Base64.getEncoder();

        try {
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
                        logger.info("resultField: " + resultField);

                        byte[] encodedBytes = encoder.encode(((HashedField) resultField).getValue().toByteArray());

                        logger.info("encodedBytes:" + encodedBytes);
                        logger.info("encodedBytes length:" + encodedBytes.length);
                        resultField = new PlainTextField(new String(encodedBytes));
                    } else {
                        resultField = field;
                    }
                } else {
                    resultField = field;
                }

                if (resultField.getValue().toString().isEmpty()) {
                    fields.put(fieldKey, JSONObject.NULL);
                } else {
                    fields.put(fieldKey, resultField.getValue());
                }
            }

        } catch (Exception e) {
            logger.info(e);
        }

        return fields;
    }

    /**
     * Conversion of a BitSet to a String representation.
     *
     * @param hash A BitSet
     * @return A String of length hash.size() where the i-th position is set to
     * "1" if the i-th bit of hash is set and "0" otherwise.
     */
    private String BitSet2String(BitSet hash) {
        StringBuffer result = new StringBuffer(hash.size());
        for (int i = 0; i < hash.length(); i++) {
            if (hash.get(i))
                result.append("1");
            else
                result.append("0");
        }
        return result.toString();
    }
}
