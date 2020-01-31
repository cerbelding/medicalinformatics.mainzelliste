package de.pseudonymisierung.mainzelliste;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class ControlNumberField extends HashedField {

    /**
     * Identifier of the key that was used to encrypt this Controlnumber
     */
    private String keyId;

    /* (non-Javadoc)
     * @see de.pseudonymisierung.mainzelliste.HashedField#clone()
     */
    @Override
    public ControlNumberField clone() {
        return new ControlNumberField(this.keyId, this.value);
    }

    public ControlNumberField() {
        this.keyId = null;
        this.value = null;
    }


    /**
     * @return the keyId
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * @param keyId the keyId to set
     */
    protected void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public ControlNumberField(String keyId, String value) {
        super(value);
        this.keyId = keyId;
    }

    public ControlNumberField(String json) throws JSONException {
        super();
        try {
            this.setValue(json);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    /* (non-Javadoc)
     * @see de.pseudonymisierung.mainzelliste.Field#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return (this.value == null || this.value.equals(""));
    }

    @Override
    public void setValue(String s) {
        if (s == null || s.equals("")) {
            this.keyId = null;
            this.value = null;
            return;
        }
        // FIXME throws-Klausel zu Field.setValue hinzufügen und Exception werfen
        try {
            JSONObject o = new JSONObject(s);
            this.keyId = o.has("keyId") ? o.getString("keyId") : null;
            this.value = o.has("value") ? o.getString("value") : null;
        } catch (JSONException e) {
            throw new Error("Error while parsing JSON string " + s);
        }
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
    public String getValueJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("keyId", this.keyId);
            o.put("value", this.value);
            return o.toString();
        } catch (JSONException e) {
            throw new Error(e);
        }
    }
}