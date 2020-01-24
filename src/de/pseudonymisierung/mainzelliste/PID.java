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

import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

/**
 * A PID as proposed by Faldum and Pommerening. Also known as Faldum-Code.
 *
 * @see PIDGenerator
 */
@Entity
public class PID extends ID {

	/**
	 * Create an instance with the given ID string and type.
	 *
	 * @param PIDString
	 *            A valid PID string.
	 * @param type
	 *            ID type as set in the configuration.
	 * @throws InvalidIDException
	 *             If PIDString is not a valid PID or the given type is unknown.
	 */
	public PID(String PIDString, String type) throws InvalidIDException {
		super(PIDString, type);
	}

	@Override
	public boolean equals(Object arg0) {
		if(!(arg0 instanceof PID))
			return false;

		PID other = (PID)arg0;
		return other.idString.equals(idString);
	}

	@Override
	public String getIdString() {
		return idString;
	}

	@Override
	protected void setIdString(String id) throws InvalidIDException {
		if(!getFactory().verify(id))
			throw new InvalidIDException();

		idString = id;
	}
}
