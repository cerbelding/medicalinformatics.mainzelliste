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

import java.lang.reflect.Constructor;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;

/**
 * A Field describing a person, e.g. name, date of birth, ...
 * This abstraction allows for different matching of plaintext and hashed Fields.
 */
@Entity
@XmlRootElement
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Field<T> {
	@Id
	@GeneratedValue
	@JsonIgnore
	protected int fieldJpaId;
	
	public abstract T getValue();
	public abstract void setValue(T value);
	
	@Override
	public abstract Field<T> clone();
	
	/** Empty constructor. Used by subclasses. */ 
	public Field()
	{		
	}
	
	public Field(T s) {
		setValue(s);
	}
	
	public abstract void setValue(String s);
	
	public JSONObject toJSON() throws JSONException {
		JSONObject ret = new JSONObject();
		ret.put("class", this.getClass().getName());
		ret.put("value", this.getValueJSON());
		return ret;
	}
	
	/** 
	 * Retrieves the value as an object compatible with JSONObject.put.
	 * This method is used to embed the field value in a JSONObject, which is used for storing
	 * patients in the database. 
	 * 
	 * This method is not designed to return a JSON-String. See 
	 * toJSON for this purpose.
	 * 
	 * @return An object compatible with JSONObject.put. Possible classes are:
	 * Boolean, Double, Integer, JSONArray, JSONObject, Long, String. Null is represented
	 * by the JSONObject.NULL object. 
	 */
	public abstract Object getValueJSON() throws JSONException;

	public static Field<?> build(String charKey, Object o){
		return build(Config.instance.getFieldType(charKey), o);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Field<?>)
				return this.getValue().equals(((Field<?>) obj).getValue());
		else
			return false;
	}

	@Override
	public String toString() {
		return this.getValue().toString();
	}
	
	public boolean isEmpty()
	{
		return this.getValue() == null;
	}
	
	public static Field<?> build(Class<? extends Field<?>> t, Object o){
		try {
			Constructor<? extends Field<?>> c = t.getConstructor(o.getClass());
			return c.newInstance(o);
		} catch (Exception e) {
				e.printStackTrace();
				throw new NotImplementedException();
		}
	}
}
