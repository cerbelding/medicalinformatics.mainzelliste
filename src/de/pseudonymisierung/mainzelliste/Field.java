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
 * This abstraction allows for different data types and corresponding matching algorithms,
 * e.g. plaintext vs. hashed Fields. Subclasses should set the type parameter to a fixed class,
 * see for example {@link PlainTextField}.
 *
 * @param <T> The the class of the embedded value.
 */
@Entity
@XmlRootElement
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Field<T> {

	/** Database id */
	@Id
	@GeneratedValue
	@JsonIgnore
	protected int fieldJpaId;

	/**
	 * Get the value of this Field.
	 * @return The value of this Field.
	 */
	public abstract T getValue();

	/**
	 * Set the value of this Field.
	 * @param value The new value.
	 */
	public abstract void setValue(T value);

	/**
	 * Create a copy of this Field.
	 * @return A copy of this Field.
	 */
	@Override
	public abstract Field<T> clone();

	/**
	 * Create an empty instance. Used by subclasses.
	 */
	public Field()
	{
	}

	/**
	 * Create an instance with the given value. Delegates to {@link Field#setValue(Object)} for this purpose.
	 * @param s The value with which to initialize.
	 */
	public Field(T s) {
		setValue(s);
	}

	/**
	 * Set the value of this Field from a String.
	 * @param s A String representation of the value to set.
	 */
	public abstract void setValue(String s);

	/**
	 * Get a JSON representation of this Field. This is a JSON object with the following members:
	 * <ul>
	 *   <li>class: The class as returned by this.getClass().getName().
	 *   <li>value: The value as returned by {@link Field#getValueJSON()}.
	 * </ul>
	 *
	 * @return JSON representation of this object.
	 * @throws JSONException If a JSON error occurs.
	 */
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
	 * This method is not designed to return a JSON representation of the Field. See {@link Field#toJSON()}
	 * for this purpose.
	 *
	 * @return An object compatible with JSONObject.put. Possible classes are:
	 * Boolean, Double, Integer, JSONArray, JSONObject, Long, String. Null is represented
	 * by the JSONObject.NULL object.

	 * @throws JSONException If a JSON error occurs.
	 */
	public abstract Object getValueJSON() throws JSONException;

	/**
	 * Create an instance of a configured field.
	 * @param charKey A valid field name as defined in the configuration.
	 * @param o An object that is suitable as value for the configured field.
	 * @return An instance of the configured field type (i.e. subclass of field) with the given value.
	 */
	public static Field<?> build(String charKey, Object o){
		return build(Config.instance.getFieldType(charKey), o);
	}

	/**
	 * Compares this Field to an object.
	 * @return true if obj is an instance of Field and this.getValue().equals(obj.getValue()).
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Field<?>)
				return this.getValue().equals(((Field<?>) obj).getValue());
		else
			return false;
	}

	/**
	 * Get a string representation of this Field. Delegates to this.getValue.toString().
	 */
	@Override
	public String toString() {
		return this.getValue().toString();
	}

	/**
	 * Check whether this field is empty.
	 * @return true if this.getValue() is null.
	 */
	public boolean isEmpty()
	{
		return this.getValue() == null;
	}

	/**
	 * Create an instance of the given realization and with the given value.
	 * @param t A non-abstract subclass of Field.
	 * @param o A object that is suitable as value for t.
	 * @return An instance of t, initialized with value o.
	 */
	public static Field<?> build(Class<? extends Field<?>> t, Object o){
		try {
			if (o == null) {
				return t.newInstance();
			}
			Constructor<? extends Field<?>> c = t.getConstructor(o.getClass());
			return c.newInstance(o);
		} catch (Exception e) {
				e.printStackTrace();
				throw new NotImplementedException();
		}
	}
}
