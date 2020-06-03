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

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;

/**
 * CompoundField represents a field that is composed of several subfields. For example,
 * a name with several components can be modeled as CompoundField<PlainTextField>.
 *
 * @param <T> The class of the components (a subclass of Field<?>).
 */
@Entity
@XmlRootElement
public class CompoundField<T extends Field<?>> extends Field<List<T>> {


	/** The value of a compound field is a list of fields of a common type */
	@OneToMany(cascade={CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch=FetchType.EAGER, targetEntity = Field.class)
	private List<T> value;

	/**
	 * Get the number of components. This is the number of Fields this CompoundField can hold, some
	 * of which can be empty at a given time. To get the number of non-empty fields,
	 * call getSize() - nEmptyFields.
	 *
	 * @return The number of components.
	 */
	public int getSize() {
		return value.size();
	}

	/**
	 * Construct a CompoundField from a list of fields.
	 * @param value A list of Fields with matching type.
	 */
	public CompoundField(List<T> value)
	{
		super(value);
	}

	/** Construct a CompoundField with the given number of components.
	 *
	 * @param size The number of components.
	 */
	public CompoundField(int size)
	{
		super(new Vector<T>(size));
		for (int i = 0; i < size; i++)
		{
			value.add(null);
		}
	}

	@Override
	public List<T> getValue()
	{
		return this.value;
	}

	/**
	 * Get the i-th component.
	 * @param i Index of the component to get.
	 * @return The i-th component.
	 */
	public T getValueAt(int i)
	{
		return this.value.get(i);
	}

	@Override
	public void setValue(List<T> value)
	{
		this.value = value;
	}

	/**
	 * Set value from a String. Required format: A JSON array whose member are JSON representations of fields,
	 * as returned by {@link Field#toJSON()}.
	 * @param s A JSON array of the fields to set as components.
	 */
	@Override
	public void setValue(String s) {
		try {
			JSONArray arr = new JSONArray(s);
			this.value = new LinkedList<T>();
			for (int fieldInd = 0; fieldInd < arr.length(); fieldInd++) {
				JSONObject obj = arr.getJSONObject(fieldInd);
				@SuppressWarnings("unchecked")
				T thisField = (T) Class.forName(obj.getString("class")).newInstance();
				thisField.setValue(obj.getString("value"));
				this.value.add(thisField);
			}
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).error("Exception:", e);
			throw new InternalErrorException();
		}
	}

	/**
	 * Set the i-th component.
	 * @param i The index of the component to set.
	 * @param value The new value to set.
	 */
	public void setValueAt(int i, T value)
	{
		this.value.set(i,  value);
	}

	/**
	 * Get the number of currently empty components. If a component is empty is
	 * determined by calling its isEmpty() method.
	 * @return The number of components c for which c.isEmpty() is true.
	 */
	public int nEmptyFields()
	{
		int result = 0;
		for (T thisField : this.value)
		{
			if (thisField.isEmpty()) result++;
		}
		return result;
	}

	@Override
	/**
	 * Check if this CompoundField is empty.
	 * @return true if all components of this CompoundField are empty.
	 */
	public boolean isEmpty()
	{
		if (this.nEmptyFields() == this.getSize())
			return true;
		else
			return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Creates a copy of this CompoundField. The components are copied by calling
	 * their clone() methods, respectively.
	 * @return The cloned CompoundField.
	 */
	public CompoundField<T> clone()
	{
		List<Field<?>> copies = new Vector<Field<?>>(3);
		for (T field : this.value)
		{
			copies.add(field.clone());
		}
		return new CompoundField<T>((List<T>) copies);
	}

	@Override
	public JSONArray getValueJSON() throws JSONException {
		JSONArray obj = new JSONArray();
		for (T field : this.value) {
			obj.put(field.toJSON());
		}
		return obj;
	}



}
