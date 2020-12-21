package de.pseudonymisierung.mainzelliste;

import java.util.BitSet;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class ControlNumberField extends HashedField {

    /**
     * Identifier of the key that was used to encrypt this Controlnumber
     */
    private String keyId;

    public static final String JSON_KEY_ID = "keyId";

    public static final String JSON_VALUE = "value";

    /* (non-Javadoc)
     * @see de.pseudonymisierung.mainzelliste.HashedField#clone()
     */
    @Override
    public ControlNumberField clone() {
        return new ControlNumberField(this.keyId, isEmpty() ? null : (BitSet) this.getValue().clone());
    }

    public ControlNumberField() {
        super();
        this.keyId = "";
    }

    /**
     * @return the keyId
     */
    public String getKeyId() {
        return keyId;
    }

    private ControlNumberField(String keyId, BitSet bitSet) {
        super(bitSet);
        this.keyId = keyId;
    }

    public ControlNumberField(String json) {
        Pair<String, String> jsonData = parseJSONString(json);
        this.keyId = jsonData.getKey();
        super.setValue(bitStringToBitSet(jsonData.getValue()));
    }

    @Override
    public void setValue(String json) {
        Pair<String, String> jsonData = parseJSONString(json);
        this.keyId = jsonData.getKey();
        super.setValue(jsonData.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ControlNumberField))
            return false;
        ControlNumberField compareTo = (ControlNumberField) o;
        // TODO: Exception, falls keyId nicht passt?
        return (compareTo.getKeyId().equals(this.getKeyId())
                && compareTo.getValue().equals(this.getValue()));
    }

    @Override
    public String getValueJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(JSON_KEY_ID, this.keyId);
        o.put(JSON_VALUE, super.getValueJSON());
        return o.toString();
    }

    private Pair<String, String> parseJSONString(String json) {
        if (StringUtils.isBlank(json)) {
            return Pair.of("","");
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            return Pair.of(jsonObject.has(JSON_KEY_ID) ? jsonObject.getString(JSON_KEY_ID) : "",
                jsonObject.has(JSON_VALUE) ? jsonObject.getString(JSON_VALUE) : "");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON representation " + json, e);
        }
    }
}
