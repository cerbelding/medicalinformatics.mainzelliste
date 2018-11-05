package de.sessionTokenSimulator;

import de.pseudonymisierung.mainzelliste.*;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.matcher.BloomFilterTransformer;
import de.securerecordlinkage.CommunicatorResource;
import de.securerecordlinkage.initializer.Config;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

// 1. Read all Patients and save in Communicator Format
// 2. Encode the fields with BloomFilter

public class PatientRecords {

    private Logger logger = Logger.getLogger(this.getClass());
    private int numPatients = 0;

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
                //TODO: use real IDs
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
            JSONObject tmpObj = new JSONObject();
            tmpObj.put("idType", IDType);
            tmpObj.put("idString", IDString);
            recordAsJSON.put("id", tmpObj);
            recordAsJSON.put("fields", getFieldsObject(p));
            CommunicatorResource rs = new CommunicatorResource();
            de.securerecordlinkage.initializer.Config c = de.securerecordlinkage.initializer.Config.instance;
            String remoteId = getRemoteID(IDType);
            rs.sendLinkRecord(c.getLocalSELUrl()+"/linkRecord/" + remoteId, IDType, IDString, recordAsJSON);
        } catch (Exception e) {
            logger.info(e);
        }
    }

    public Integer linkPatients(String remoteID) {
        JSONArray array = new JSONArray();
        int index = 0;
        try {
            List<Patient> patientList = Persistor.instance.getPatients();
            numPatients = patientList.size();
            logger.info(numPatients + " patients to be matched");
            logger.info("Linking started...");
            CommunicatorResource rs = new CommunicatorResource();
            de.securerecordlinkage.initializer.Config c = de.securerecordlinkage.initializer.Config.instance;
            String IDType = getIDType(c.getLocalID(), remoteID);
            IDGenerator<? extends ID> factory = IDGeneratorFactory.instance.getFactory(IDType);

            if (factory == null) {
                throw new InvalidIDException("ID type " + IDType + " not defined!");
            }

            factory.reset(IDType);

            for (Patient p: patientList) {
                Patient existingPatient = Persistor.instance.getPatient(new SrlID(String.valueOf(index+1), IDType));
                index = index + 1;
                if (existingPatient != null && !existingPatient.equals(p)) {
                    throw new Exception("Delete Secure Record Linkage IDs before new linkage");
                }
                String IDString = p.getId(IDType).getIdString();
                if (existingPatient == null) {
                    Persistor.instance.updatePatient(p);
                }
                if (IDString.equals(String.valueOf(index))) {
                    JSONObject tmpObject = new JSONObject();
                    tmpObject.put("fields", getFieldsObject(p));
                    array.put(tmpObject);
                } else {
                    throw new Exception("Delete Secure Record Linkage IDs before new linkage");
                }
            }
            rs.sendLinkRecords(c.getLocalSELUrl()+"/linkRecords/" + remoteID, IDType, array);

        } catch (Exception e) {
            logger.info(e);
        }

        return numPatients;
    }

    public Integer matchPatients(String remoteId) {
        try {
            List<Patient> patientList = Persistor.instance.getPatients();
            numPatients = patientList.size();
            logger.info(numPatients + " patients to be matched");
            logger.info("Matching started...");
            for (Patient p: patientList) {
                matchPatient(p, remoteId);
            }
        } catch (Exception e) {
            logger.info(e);
        }

        return numPatients;
    }

    public void matchPatient(Patient p, String remoteId) {
        try {
            JSONObject recordAsJSON = new JSONObject();
            recordAsJSON.put("fields", getFieldsObject(p));
            CommunicatorResource rs = new CommunicatorResource();
            de.securerecordlinkage.initializer.Config c = de.securerecordlinkage.initializer.Config.instance;
            rs.sendMatchRecord(c.getLocalSELUrl()+"/matchRecord/" + remoteId, recordAsJSON);
        } catch (Exception e) {
            logger.info(e);
        }
    }

    public int updateRecord(String idType, String tmpRef, String linkageId) {
        try {
            logger.info("updatePatient(" + tmpRef + ") -> " + linkageId + ")");

            ID oldId = new SrlID(tmpRef, idType);
            ID newId = IDGeneratorFactory.instance.buildId(idType, linkageId);

            Patient p = Persistor.instance.getPatient(oldId);
            Set<ID> newIds = new HashSet<ID>();
            Set<ID> listIds = p.getIds();
            for (ID id : listIds) {
                if (id.getType().equals(idType)) {
                    newIds.add(newId);
                } else {
                    newIds.add(id);
                }
            }
            p.setIds(newIds);
            Persistor.instance.updatePatient(p);
        } catch (Exception e) {
            logger.info(e);
            return 500;
        }

        return 200;
    }

    public String getRandomID(){
            //TODO: only for testing, use real IDs
            Random rand = new Random();
            int  n = rand.nextInt(50000) + 1;
        return String.valueOf(n);
    }

    public JSONObject getFieldsObject(Patient p) {
        de.pseudonymisierung.mainzelliste.Config config = de.pseudonymisierung.mainzelliste.Config.instance;
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

    private String getRemoteID(String IDType) {
        if (IDType != null && !IDType.isEmpty()) {
            String [] parts = IDType.split("-");
            if (parts.length == 3) {
                return parts[2];
            }
        }
        return "";
    }

    private String getIDType(String localID, String remoteID) {
        return "link-"+localID+"-"+remoteID;
    }
}
