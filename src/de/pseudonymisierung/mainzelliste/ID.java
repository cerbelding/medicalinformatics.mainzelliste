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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.openjpa.persistence.jdbc.Index;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

/**
 * A person's identifier. Once created, the ID is guaranteed to be valid.
 * Immutable.
 *
 * Every ID consists of the identifying string ("ID string") and a type
 * ("ID type"). For one patient, multiple IDs with different types can be used
 * to identify the patient within different domains or namespaces. The ID
 * type is only a name and has no relation to the class of the respective
 * ID objects.
 *
 * All valid ID types for an instance must be configured (see section
 * "IDGenerators" in the configuration file).
 *
 * @see IDGenerator to generate IDs.
 */
@Entity
@Table(name="ID", uniqueConstraints=@UniqueConstraint(columnNames={"idString","type"}))
public abstract class ID {

	/** Database id. */
	@Id
	@GeneratedValue
	@JsonIgnore
	protected int idJpaId;

	/** The ID string. */
	@Basic
	@Index(name="i_id_idstring")
	protected String idString;

	/** The type (a.k.a. domain) of the ID. */
	@Basic
	protected String type;

	/**
	 * Whether this ID is tentative, i.e. the patient to which it is assigned
	 * might be a duplicate.
	 */
	@Basic
	protected boolean tentative;

	/**
	 * Check whether this ID is tentative, i.e. the patient to which it is
	 * assigned might be a duplicate.
	 *
	 * @return true if this ID is tentative.
	 */
	public boolean isTentative() {
		return tentative;
	}

	/**
	 * Set the tentative status of this ID.
	 *
	 * @see #isTentative()
	 * @param tentative
	 *            Whether this ID should be considered tentative (true) or not
	 *            (false).
	 */
	public void setTentative(boolean tentative) {
		this.tentative = tentative;
	}

	/**
	 * Creates an ID with a given ID string and type.
	 *
	 * @param idString
	 *            String containing a valid ID.
	 * @param type
	 *            Type as according to configuration.
	 * @throws InvalidIDException
	 *             If the given id type is not known or the given ID string is
	 *             invalid and could not be corrected.
	 */
	public ID(String idString, String type) throws InvalidIDException {
		setType(type);
		setTentative(false);
		IDGenerator<?> generator = getFactory();
		if (generator == null)
			throw new InvalidIDException("ID type " + type + " is unknown.");
		if (!generator.verify(idString)){
			throw new InvalidIDException("ID " + idString + " is invalid for type " + type + ".");
		}
		setIdString(idString);
	}

	/**
	 * Compare this ID with another. Two ID objects are considered equal if they
	 * belong to the same class (subclass of ID) and have equal values for ID
	 * type ({@link #getType()}) and ID string ({@link #getIdString()}).
	 * 
	 * @return true if obj is not null and equal to this according to the stated
	 *         definition, false otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this.getClass() != obj.getClass())
			return false;

		ID idToCompareWith = (ID) obj;
		return (this.getType().equals(idToCompareWith.getType())
				&& this.getIdString().equals(idToCompareWith.getIdString()));
	}

	/**
	 * Gets the ID string.
	 *
	 * @return The ID string.
	 */
	public abstract String getIdString();

	/**
	 * Sets the ID string to the given value.
	 *
	 * @param id
	 *            A String valid as ID string for this ID type.
	 * @throws InvalidIDException
	 *             If id is not valid as an ID string for this ID type.
	 * */
	protected abstract void setIdString(String id) throws InvalidIDException;

	/**
	 * Gets the ID type.
	 * @return The ID type.
	 */
	public String getType(){
		return type;
	}

	/**
	 * Sets the ID type.
	 * @param type The new ID type.
	 */
	protected void setType(String type){
		this.type = type;
	}

	/**
	 * Returns a generator that can be used to create IDs of the same type as
	 * this ID.
	 *
	 * @return The ID generator or null if none exists for the type of this ID.
	 */
	@JsonIgnore
	@Transient
	public IDGenerator<? extends ID> getFactory(){
		return IDGeneratorFactory.instance.getFactory(getType());
	}

	/**
	 * Returns a string representation of this ID, mainly for display in log files etc.
	 * @return A string of the format "{idType}={idString}".
	 */
	@Override
	public String toString() {
		return String.format("%s=%s", getType(), getIdString());
	}

	/**
	 * The hash code of an ID is computed as hash code of its String representation as returned by
	 * {@link ID#toString()}.
	 * @return The hash code of this ID.
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns a JSON representation of this object.
	 *
	 * @return A JSON object with fields "idString", "idType" (both String) and "tentative" (Boolean).
	 */
	public JSONObject toJSON() {
		try {
			JSONObject ret = new JSONObject();
			ret.put("idType", this.type);
			ret.put("idString", this.idString);
			ret.put("tentative", this.tentative);

			return ret;
		} catch (JSONException e) {
			// If an exception occurs here, it indicates a bug
			throw new Error(e);
		}
	}
}
