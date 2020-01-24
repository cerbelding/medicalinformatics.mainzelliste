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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ElementCollection;
import javax.persistence.MapKeyClass;

import de.pseudonymisierung.mainzelliste.dto.Persistor;

/**
 * Contains state information for an IDGenerator in key-value-form. A typical
 * use case is to hold a counter that is incremented on every ID generation. For
 * every ID generator, an instance of this class is persisted in the database.
 */
@Entity
public class IDGeneratorMemory {

	/** Database id */
	@Id
	@GeneratedValue
	protected int fieldJpaId;

	/** The persisted properties. */
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@MapKeyClass(String.class)
	protected Map<String, String> mem = new HashMap<String, String>();

	/** The id type for which this instance holds information. */
	protected String idType;

	/**
	 * Create an instance for the given ID type.
	 *
	 * @param idType
	 *            The ID type.
	 */
	public IDGeneratorMemory(String idType) {
		this.idType = idType;
	}

	/**
	 * Set a property.
	 *
	 * @param key
	 *            The property key.
	 * @param value
	 *            The property value.
	 */
	public synchronized void set(String key, String value) {
		mem.put(key, value);
	}

	/**
	 * Get a property.
	 *
	 * @param key
	 *            The property key.
	 * @return The property value.
	 */
	public synchronized String get(String key) {
		return mem.get(key);
	}

	/**
	 * Save properties to database.
	 *
	 * @see Persistor#updateIDGeneratorMemory(IDGeneratorMemory)
	 */
	public synchronized void commit() {
		Persistor.instance.updateIDGeneratorMemory(this);
	}
}
