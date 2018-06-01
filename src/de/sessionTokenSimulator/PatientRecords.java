package de.sessionTokenSimulator;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;

// 1. Read all Patients and save in Communicator Format
// 2. Encode the fields with BloomFilter

public class PatientRecords {

    private Logger logger = Logger.getLogger(this.getClass());

    public JSONObject readAllPatients() {
        JSONObject reqObject = new JSONObject();
        JSONArray array = new JSONArray();
        try{
            List<Patient> patientList = Persistor.instance.getPatients();
            for (Patient p : patientList) {
                JSONObject fields = new JSONObject();
                for (String fieldKey : p.getFields().keySet()) {
                    fields.put(fieldKey, p.getFields().get(fieldKey));
                }
                JSONObject ids = new JSONObject();
                for (ID id : p.getIds()) {
                    ids.put(id.getType(), id.getIdString());
                }
                JSONObject tmpObject = new JSONObject();
                tmpObject.put("fields", fields);
                tmpObject.put("ids", ids);
                array.put(tmpObject);
            }
            reqObject.put("records", array);
        } catch (Exception e) {
            logger.info(e);
        }

        return reqObject;
    }
}
