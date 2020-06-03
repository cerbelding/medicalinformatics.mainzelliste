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

import java.util.BitSet;

import javax.persistence.Column;
import javax.persistence.Entity;

import de.pseudonymisierung.mainzelliste.matcher.BloomFilterTransformer;
import de.pseudonymisierung.mainzelliste.matcher.DiceFieldComparator;

/**
 * Hashed fields for error-tolerant matching. The value of a hashed field
 * represents a bloom filter, represented by a bit string, that encodes the set
 * of n-grams of a given character string. This allows for error-tolerant,
 * privacy preserving record linkage according to the method of Schnell,
 * Bachteler and Reiher.
 *
 * @see DiceFieldComparator
 */
@Entity
public class HashedField extends Field<BitSet>{

	/**	The value is encoded as a String to allow storage in a database. */
	@Column(length = BloomFilterTransformer.hashLength)
	protected String value;

	/**
	 * Conversion of the String representation of a bit string to a BitSet.
	 *
	 * @param b
	 *            String of the format [01]*
	 * @return A BitSet with all bit i set for which b[i].equals("1").
	 */
	private static BitSet String2BitSet(String b)
	{
		if (b == null)
			return null;
		BitSet bs = new BitSet(b.length());
		for (int i = 0; i < b.length(); i++)
		{
			switch (b.charAt(i))
			{
			case '1' :
				bs.set(i);
			case '0' :
				break;
			default : // illegal value
				return null;
			}
		}
		return bs;
	}

	/**
	 * Conversion of a BitSet to a String representation.
	 *
	 * @param hash
	 *            A BitSet
	 * @return A String of length hash.size() where the i-th position is set to
	 *         "1" if the i-th bit of hash is set and "0" otherwise.
	 */
	private static String BitSet2String(BitSet hash)
	{
		StringBuffer result = new StringBuffer(hash.size());
		for (int i = 0; i < hash.length(); i++)
		{
			if (hash.get(i))
				result.append("1");
			else
				result.append("0");
		}
		return result.toString();
	}

	/**
	 * Create an empty instance.
	 */
	public HashedField() {
		this.value = "";
	}

	/**
	 * Create an instance from a BitSet.
	 * @param b A BitSet.
	 */
	public HashedField(BitSet b) {
		this.value = BitSet2String(b);
	}

	/** Create an instance from a String representation of a bit string.
	 *
	 * @param b A String of the format [01]*.
	 */
	public HashedField(String b)
	{
		this.value = b;
	}

	@Override
	public BitSet getValue() {
		return String2BitSet(this.value);
	}

	@Override
	public String getValueJSON() {
		return this.value;
	}

	@Override
	public void setValue(BitSet hash) {
		this.value = BitSet2String(hash);
	}

	@Override
	public void setValue(String s) {
		this.value = s;
	}

	@Override
	public HashedField clone()
	{
		HashedField result = new HashedField(new String(this.value));
		return result;
	}
}
