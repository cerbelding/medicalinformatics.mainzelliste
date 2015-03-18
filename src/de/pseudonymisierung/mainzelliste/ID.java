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
 * A person's identificator. Once created, the ID is guaranteed to be valid. Immutable.
 * 
 * @see IDGenerator to generate IDs.
 */
@Entity
@Table(name="ID", uniqueConstraints=@UniqueConstraint(columnNames={"idString","type"}))
public abstract class ID {
	@Id
	@GeneratedValue
	@JsonIgnore
	protected int idJpaId;
	
	@Basic
	@Index(name="i_id_idstring")
	protected String idString;
	
	@Basic
	protected String type;
	
	@Basic
	protected boolean tentative;
	
	/**
	 * Check whether this ID is tentative, i.e. the patient to which it is assigned
	 * might be a duplicate. 
	 */
	public boolean isTentative() {
		return tentative;
	}

	/**
	 * Set the tentative status of this ID.
	 * @see #isTentative()
	 * @param tentative Whether this ID should be considered tentative (true) or not (false).
	 */
	public void setTentative(boolean tentative) {
		this.tentative = tentative;
	}

	/**
	 * Creates an ID from a given IDString.
	 * 
	 * @param idString String containing a valid ID.
	 * @param type Type as according to config.
	 * @throws InvalidIDException The given IdString is invalid and could not be corrected.
	 */
	public ID(String idString, String type) throws InvalidIDException {
		setType(type);
		setTentative(false);
		if(!getFactory().verify(idString)){
			throw new InvalidIDException();
		}
		setIdString(idString);
	}
	
	/**
	 * String representation of this ID.
	 */
	public abstract String getIdString();
	protected abstract void setIdString(String id) throws InvalidIDException;
	
	/**
	 * Type of this ID according to config.
	 */
	public String getType(){
		return type;
	}
	
	protected void setType(String type){
		this.type = type;
	}
	
	/**
	 * Returns a generator that can be used to create IDs of the same type as this ID.
	 */
	@JsonIgnore
	@Transient
	public IDGenerator<? extends ID> getFactory(){
		return IDGeneratorFactory.instance.getFactory(getType());
	}
	
	@Override
	public String toString() {
		return String.format("%s=%s", getType(), getIdString());
	}
	
// alte Version, behalten für Kompatibilität?	
//	public JSONObject toJSON() throws JSONException {
//		JSONObject result = new JSONObject();
//		result.put("type", this.getType());
//		result.put("idString", this.getIdString());
//		result.put("tentative", this.isTentative());
//		return result;
//	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject ret = new JSONObject();
		ret.put("idType", this.type);
		ret.put("idString", this.idString);
		ret.put("tentative", this.tentative);
		
		return ret;
	}
}
