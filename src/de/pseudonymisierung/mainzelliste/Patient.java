/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it 
 * with Jersey (https://jersey.java.net) (or a modified version of that 
 * library), containing parts covered by the terms of the General Public 
 * License, version 2.0, the licensors of this Program grant you additional 
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.exceptions.CircularDuplicateRelationException;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;

/**
 * A patient entity identified by at least one ID and described by a number of Fields.
 */
@XmlRootElement
@Entity
public class Patient {

	/**
	 * JSON serialization of a map of fields. Used for persisting fields in the
	 * database.
	 * 
	 * @param fields
	 *            Map of fields to serialize.
	 * @return JSON serialization of the given fields.
	 * @see #stringToFields(String)
	 */
	public static String fieldsToString(Map<String, Field<?>> fields) {
		try {
			JSONObject fieldsJson = new JSONObject();
			for (String fieldName : fields.keySet()) {
				JSONObject thisField = new JSONObject();
				thisField.put("class", fields.get(fieldName).getClass()
						.getName());
				thisField.put("value", fields.get(fieldName).getValueJSON());
				fieldsJson.put(fieldName, thisField);
			}
			return fieldsJson.toString();
		} catch (JSONException e) {
			Logger.getLogger(Patient.class).error("Exception: ", e);
			throw new InternalErrorException();
		}
	}

	/**
	 * Creates a map of fields from its JSON representation. Used to read
	 * persisted fields from the database.
	 * 
	 * @param fieldsJsonString
	 *            JSON string as created by {@link #fieldsToString(Map)}.
	 * @return The map of fields.
	 */
	public static Map<String, Field<?>> stringToFields(String fieldsJsonString) {
		try {
			Map<String, Field<?>> fields = new HashMap<String, Field<?>>();
			JSONObject fieldsJson = new JSONObject(fieldsJsonString);
			Iterator<?> it = fieldsJson.keys();
			while (it.hasNext()) {
				String fieldName = (String) it.next();
				JSONObject thisFieldJson = fieldsJson.getJSONObject(fieldName);
				String fieldClass = thisFieldJson.getString("class");
				String fieldValue = thisFieldJson.getString("value");
				Field<?> thisField = (Field<?>) Class.forName(fieldClass)
						.newInstance();
				thisField.setValue(fieldValue);
				fields.put(fieldName, thisField);
			}
			return fields;
		} catch (JSONException e) {
			Logger.getLogger(Patient.class).error(
					"JSON error while parsing patient fields: "
							+ e.getMessage(), e);
			throw new InternalErrorException();
		} catch (Exception e) {
			Logger logger = Logger.getLogger(Patient.class);
			logger.error("Exception while parsing patient fields from string: "
					+ fieldsJsonString);
			Logger.getLogger(Patient.class)
					.error("Cause: " + e.getMessage(), e);
			throw new InternalErrorException();
		}
	}

	/**
	 * The map of fields of this patient. Field names are map keys, the
	 * corresponding Field objects are the values. The fields are not persisted
	 * by this map (therefore the {@literal @Transient} annotation), as this
	 * leads to poor performance. Instead, fields are serialized as a JSON
	 * object (handled by {@link #setFields(Map)} and de-serialized by
	 * {@link #postLoad()} upon saving to and loading from the database,
	 * respectively.
	 * 
	 * @see #fieldsString
	 */
	@Transient
	private Map<String, Field<?>> fields;

	/**
	 * Serialization of the fields as JSON string for efficient storage in the
	 * database.
	 * 
	 * @see #fields
	 */
	@Column(columnDefinition = "text", length = -1)
	@JsonIgnore
	private String fieldsString;

	/**
	 * Set of IDs for this patient.
	 */
	@OneToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	private Set<ID> ids;

	/**
	 * Input fields as read from form (before transformation). Used to display
	 * field values as they were entered in first place.
	 * 
	 * @see #fields
	 * @see #inputFieldsString
	 */
	@Transient
	private Map<String, Field<?>> inputFields;

	/**
	 * Serialization of the input fields as JSON string for efficient storage in
	 * the database.
	 * 
	 * @see #inputFields
	 */
	@Column(length = 4096)
	@JsonIgnore
	private String inputFieldsString;

	/**
	 * True if this patient is suspected to be a duplicate.
	 */
	private boolean isTentative = false;

	/** The logging instance. */
	@Transient
	@JsonIgnore
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * The patient of which this patient is a duplicate of. If p.original is not
	 * null, p is considered a duplicate of p.original.
	 */
	@ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JsonIgnore
	private Patient original = null;

	/** Database id. */
	@Id
	@GeneratedValue
	@JsonIgnore
	private int patientJpaId;

	/**
	 * Construct an empty patient object.
	 */
	public Patient() {
	}

	/**
	 * Construct a patient object with the specified ids and fields.
	 * 
	 * @param ids
	 *            A set of ID objects that identify the patient.
	 * @param c
	 *            The fields of the patient. A map with field names as keys and
	 *            the corresponding Field objects as values.
	 */
	public Patient(Set<ID> ids, Map<String, Field<?>> c) {
		this.ids = ids;
		this.setFields(c);
	}

	/**
	 * Get the fields of this patient.
	 * 
	 * @return An unmodifiable map with field names as keys and corresponding
	 *         field objects as values. Although the map itself is unmodifiable,
	 *         modifications of its members affect the patient object.
	 */
	public Map<String, Field<?>> getFields() {
		return Collections.unmodifiableMap(fields);
	}

	/**
	 * Get the ID of the specified type from this patient.
	 * 
	 * @param type
	 *            The ID type. See {@link ID} for the general structure of an
	 *            ID.
	 * @return This patient's ID of the given type or null if no such ID is
	 *         defined.
	 */
	public ID getId(String type) {
		for (ID thisId : ids) {
			if (thisId.getType().equals(type))
				return thisId;
		}
		return null;
	}

	/**
	 * Get the set of IDs of this patient.
	 * 
	 * @return The IDs of the patient as unmodifiable set. While the set itself
	 *         is unmodifiable, modification of the elements (ID objects) affect
	 *         the patient object.
	 */
	public Set<ID> getIds() {
		return Collections.unmodifiableSet(ids);
	}

	/**
	 * Returns the input fields, i.e. as they were transmitted in the last
	 * request that modified this patient, before transformations. Used for
	 * displaying the field values as they had been entered in the first place.
	 * 
	 * @return Map with field names as keys and the corresponding Field objects
	 *         as values.
	 */
	public Map<String, Field<?>> getInputFields() {
		return inputFields;
	}

	/**
	 * Gets the original of this patient, i.e. the patient of which this patient
	 * is a duplicate. More precisely: A patient p_1 is the original of a
	 * patient p_n if either p_1 and p_n are the same or if there exists a chain
	 * p_1, p_2, ... , p_n of patients where p_k is a duplicate of p_k+1 for
	 * 1<=k<n.
	 * 
	 * PID requests that find p as the best matching patient should return the
	 * PID of p.getOriginal().
	 * 
	 * @return The original of this patient, returns this if this patient is not
	 *         a duplicate.
	 * 
	 * @see #setOriginal(Patient)
	 */
	public Patient getOriginal() {
		Patient p = this;
		while (p.original != null && p.original != p)
			p = p.original;
		return p;
	}

	/**
	 * Returns the internal ID of the persistency engine. Needed to determine if
	 * two Patient object refer to the same database entry.
	 * 
	 * @return the patientJpaId
	 */
	public int getPatientJpaId() {
		return patientJpaId;
	}

	/**
	 * Check whether this patient is the duplicate of another.
	 * 
	 * @return True if this patient is the duplicate of another.
	 * @see #getOriginal()
	 * @see #setOriginal(Patient)
	 */
	public boolean isDuplicate() {
		return (this.original != null);
	}

	/**
	 * Check if this patient is suspected to be a duplicate of another one.
	 * 
	 * @return True if this patient is suspected to be a duplicate of another.
	 */
	public boolean isTentative() {
		return isTentative;
	}

	/**
	 * De-serializes fields and input fields from their JSON representation.
	 * Automatically called by the JPA engine right after loading the object
	 * from the database. Fields that are missing in the database due to a later
	 * change of configuration are added with empty values.
	 */
	@PostLoad
	public void postLoad() {
		this.fields = stringToFields(this.fieldsString);
		this.inputFields = stringToFields(this.inputFieldsString);
		for (String thisFieldKey : Config.instance.getFieldKeys()) {
			if (!this.fields.containsKey(thisFieldKey))
				this.fields.put(thisFieldKey, Field.build(thisFieldKey, ""));
			if (!this.inputFields.containsKey(thisFieldKey))
				this.inputFields.put(thisFieldKey,
						Field.build(thisFieldKey, ""));
		}
	}

	/**
	 * Check whether p and this patient are the same in the database (i.e. their
	 * patientJpaId values are equal).
	 * 
	 * @param p
	 *            A patient object.
	 * @return true if this and p refer to the same database entry.
	 */
	public boolean sameAs(Patient p) {
		return (this.getPatientJpaId() == p.getPatientJpaId());
	}

	/**
	 * Set the fields of this patient.
	 * 
	 * @param Fields
	 *            A map with field names as keys and corresponding Field objects
	 *            as values. The map is copied by reference.
	 */
	public void setFields(Map<String, Field<?>> Fields) {
		this.fields = Fields;
		this.fieldsString = fieldsToString(this.fields);
	}

	/**
	 * Set the IDs for this patient.
	 * 
	 * @param ids
	 *            Set of IDs. The set is copied by reference.
	 */
	public void setIds(Set<ID> ids) {
		this.ids = ids;
	}

	/**
	 * Set the input fields. Whenever a request modifies this patient object (or
	 * upon creation) the input fields as transmitted in the request, before
	 * transformation, should be set with this method. This allows redisplaying
	 * them in the admin interface or to users by means of a "readPatients"
	 * token.
	 * 
	 * @param inputFields
	 *            Map with field names as keys and corresponding Field objects
	 *            as values.
	 */
	public void setInputFields(Map<String, Field<?>> inputFields) {
		this.inputFields = inputFields;
		this.inputFieldsString = fieldsToString(inputFields);
	}

	/**
	 * Set the original of a patient (see {@link #getOriginal() for a
	 * definition}. This effectively marks this patient as a duplicate of the
	 * argument.
	 * 
	 * @param original
	 *            The patient which is to be set as the original of this. Can be
	 *            null, which means that this patient is not a duplicate at all.
	 */
	public void setOriginal(Patient original) {
		if (original == null || original.sameAs(this)) {
			this.original = null;
			return;
		}
		// Check if operation would lead to a circular relation
		// (setting a as duplicate of b when b is a duplicate of a)
		if (original.getOriginal().sameAs(this)) {
			CircularDuplicateRelationException e = new CircularDuplicateRelationException(
					this.getId("pid").getIdString(), original.getId("pid")
							.getIdString());
			logger.error(e.getMessage());
			throw e;
		}
		this.original = original;
	}

	/**
	 * Sets the "tentative" status of this patient, i.e. if it is suspected that
	 * the patient is a duplicate of another.
	 * 
	 * @param isTentative
	 *            The new tentative status.
	 */
	public void setTentative(boolean isTentative) {
		this.isTentative = isTentative;
		for (ID id : this.ids) {
			id.setTentative(isTentative);
		}
	}

	/**
	 * Returns a string representation of this patient by calling toString on
	 * the map of fields, i.e. the result looks like "[vorname=Peter, nachname=Meier, ...]".
	 */
	@Override
	public String toString() {
		return fields.toString();
	}
}