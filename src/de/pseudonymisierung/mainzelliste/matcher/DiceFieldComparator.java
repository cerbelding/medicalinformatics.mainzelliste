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

import java.util.BitSet;

import de.pseudonymisierung.mainzelliste.HashedField;

/**
 * Compares two fields that were encrypted as bloom filters.
 * See {@link BloomFilterTransformer} for details.
 */
public class DiceFieldComparator extends FieldComparator<HashedField> {

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
	public DiceFieldComparator (String fieldLeft, String fieldRight)
	{
		super(fieldLeft, fieldRight);
	}

	@Override
	public double compareBackend(HashedField fieldLeft, HashedField fieldRight)
	{

		assert (fieldLeft instanceof HashedField);
		assert (fieldRight instanceof HashedField);

		HashedField hLeft = fieldLeft;
		HashedField hRight = fieldRight;
		BitSet bLeft = hLeft.getValue();
		BitSet bRight = hRight.getValue();

		int nLeft = bLeft.cardinality();
		int nRight = bRight.cardinality();
		bLeft.and(bRight);
		return (2.0 * bLeft.cardinality() / (nLeft + nRight));
	}
}

