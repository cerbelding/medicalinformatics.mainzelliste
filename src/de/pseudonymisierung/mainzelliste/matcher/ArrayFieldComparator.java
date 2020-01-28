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

import java.util.Hashtable;
import java.util.Map;

import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.StringPair;


/**
 * Implements an array comparison between sets of fields.
 * I.e., a set of fields in the data of one patient is compared
 * to a set of fields in another patient's data. This allows
 * taking transposition (such as swapping of name components) into
 * account.
 *
 * NOTE: This class is not used currently and might be removed in a future release.
 *
 */
public class ArrayFieldComparator {

	/** The names of fields to compare in the first patient's data */
	private String fieldListLeft[];
	/** The names of fields to compare in the second patient's data */
	private String fieldListRight[];
	/** The FieldComparator used to compare the subfields. */
	private FieldComparator<?> comparator;

	/**
	 * Instantiates an ArrayFieldComparator.
	 *
	 * @param fieldListLeft
	 *            The names of fields to compare in the first patient's data.
	 * @param fieldListRight
	 *            The names of fields to compare in the second patient's data.
	 * @param comparator
	 *            The comparator to use to compare individual subfields.
	 */
	public ArrayFieldComparator(String fieldListLeft[], String fieldListRight[],
			FieldComparator<?> comparator)
	{
		super();
		this.fieldListLeft = fieldListLeft;
		this.fieldListRight = fieldListRight;
		this.comparator = comparator;
	}

	/**
	 * Compares two patient on the configured array of fields.
	 * @param patientLeft The left hand side patient.
	 * @param patientRight The right hand side patient.
	 * @return A map with every field combination mapped to the comparison value.
	 */
	public Map<StringPair, Object> compare(Patient patientLeft, Patient patientRight)
	{
		Hashtable<StringPair, Object> result = new Hashtable<StringPair, Object>();
		for (String fieldLeft : fieldListLeft)
		{
			comparator.setFieldLeft(fieldLeft);
			for (String fieldRight : fieldListRight)
			{
				comparator.setFieldRight(fieldRight);
				Object value = comparator.compare(patientLeft, patientRight);
				result.put(new StringPair(fieldLeft, fieldRight), value);
			}
		}

		return result;
	}

}
