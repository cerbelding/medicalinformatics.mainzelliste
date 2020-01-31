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
package de.pseudonymisierung.mainzelliste.matcher;

import java.util.Map;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;

/**
 * BinaryFieldComparator implements a simple equal/unequal-comparison on fields.
 * Comparison is performed with the field's .equals method. For equal field, 1.0
 * is returned as comparison value, 0 otherwise.
 *
 * @author borg
 *
 */
public class BinaryFieldComparator extends FieldComparator<Field<?>> {

	/**
	 * Instantiate comparison between two specified fields. The field
	 * definitions correspond to indices in the Fields map of the persons
	 * (objects of class Patient) which are compared.
	 *
	 * @param fieldLeft
	 *            Name of comparison field on the left side.
	 * @param fieldRight
	 *            Name of comparison field on the right side.
	 */
	public BinaryFieldComparator (String fieldLeft, String fieldRight)
	{
		super(fieldLeft, fieldRight);
	}

	@Override
	public double compare(Patient patientLeft, Patient patientRight) {
		// TODO: Fall, dass geforderte Charakteristiken nicht vorhanden sind
		Map<String, Field<?>> cLeft = patientLeft.getFields();
		Map<String, Field<?>> cRight = patientRight.getFields();
		return compare(cLeft.get(this.fieldLeft), cRight.get(this.fieldRight));
	}

	/**
	 * @return 1.0 if fieldLeft is not null and fieldLeft.equals(fieldRight), 0
	 *         otherwise.
	 */
	@Override
	public double compareBackend(Field<?> fieldLeft, Field<?> fieldRight)
	{
		if (fieldLeft != null && fieldLeft.equals(fieldRight))
			return 1.0;
		else
			return 0.0;

	}
}
