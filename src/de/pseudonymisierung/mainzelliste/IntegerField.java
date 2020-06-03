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

import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;

/**
 * A Field consisting of an Integer.
 */
@Entity
public class IntegerField extends Field<Integer> {

	/** The value of this field. */
	private Integer value;

	/**
	 * Creates an instance with the given value.
	 * 
	 * @param value
	 *            The value to set.
	 */
	public IntegerField(Integer value) {
		this.value = value;
	}

	/**
	 * Creates an instance from a String.
	 * 
	 * @param value
	 *            A String that can be parsed to an Integer.
	 */
	public IntegerField(String value) {
		this.setValue(value);
	}

	@Override
	public Integer getValue() {
		return this.value;
	}

	@Override
	public void setValue(Integer value) {
		this.value = value;
	}

	@Override
	public void setValue(String s) {
	    if (StringUtils.isEmpty(s) || "null".equals(s))
	        this.value = null;
	    else
	        this.value = Integer.parseInt(s);
	}

	@Override
	public Object getValueJSON() {
		return this.value == null ? JSONObject.NULL : this.value;
	}

	@Override
	public IntegerField clone() {
		return new IntegerField(this.value);
	}
	
	@Override
	public String toString() {
	    if (value == null)
	        return "";
	    else
	        return String.format("%02d", value);
	}
}
