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

import de.pseudonymisierung.mainzelliste.matcher.BloomFilterTransformer;
import de.pseudonymisierung.mainzelliste.matcher.DiceFieldComparator;
import java.util.Base64;
import java.util.BitSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;

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
public class HashedField extends Field<BitSet> {

	/**	The bit array is stored as the base64 encoded binary representation of the {@link BitSet}. */
	@Column(length = BloomFilterTransformer.hashLength)
	protected String value;

	/** BitSet reference to prevent repeated instantiations from base64 encoded binary data */
	@Transient
	private BitSet bitSet;

	// Helpers

	/**
	 * Conversion of the Base64 encoded binary representation of a BitSet to a BitSet.
	 * This encoding is used internally as it is much faster to parse than bit strings.
	 *
	 * @param b64 Base64 String
	 * @return A BitSet
	 */
	public static BitSet base64ToBitSet(String b64) {
		return BitSet.valueOf(Base64.getDecoder().decode(b64));
	}

	/**
	 * Conversion of a BitSet to a Base64 encoded String representation
	 * @param bs A BitSet
	 * @return Base64 String
	 */
	public static String bitSetToBase64(BitSet bs) {
		return Base64.getEncoder().encodeToString(bs.toByteArray());
	}

	/**
	 * Conversion of the String representation of a bit string to a BitSet.
	 *
	 * @param b String of the format [01]*
	 * @return A BitSet with all bit i set for which b[i].equals("1").
	 */
	public static BitSet bitStringToBitSet(String b) {
		if (StringUtils.isBlank(b))
			return null;
		BitSet bs = new BitSet(b.length());
		for (int i = 0; i < b.length(); i++) {
			switch (b.charAt(i)) {
				case '1' :
					bs.set(i);
				case '0' :
					break;
				default : // illegal value
					throw new IllegalArgumentException(
							"Failed to parse bit string '" + b + "'. Character " + b.charAt(i) + " at " + i
									+ " isn't bit.");
			}
		}
		return bs;
	}

	/**
	 * Conversion of a BitSet to a String representation.
	 *
	 * @param bs A BitSet
	 * @return A String of length bs.size() where the i-th position is set to
	 *         "1" if the i-th bit of bs is set and "0" otherwise.
	 */
	public static String bitSetToBitString(BitSet bs) {
		StringBuffer result = new StringBuffer(bs.size());
		for (int i = 0; i < bs.length(); i++) {
			if (bs.get(i))
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
		this.bitSet = null;
		this.value = "";
	}

	/**
	 * Create an instance from a BitSet.
	 * @param b A BitSet.
	 */
	public HashedField(BitSet b) {
		setValue(b);
	}

	/**
	 * Create an instance from a bit string
	 * @param s String of the format [01]*
	 */
	public HashedField(String s) {
		this(bitStringToBitSet(s));
	}

	@Override
	public BitSet getValue() {
		return this.bitSet;
	}

	@Override
	public Object getValueJSON() throws JSONException {
		return this.value == null ? "" : this.value;
	}

	@Override
	public void setValue(BitSet b) {
		this.bitSet = b;
		this.value = b == null ? "" : bitSetToBase64(b);
	}

	/**
	 * Set the value of this Field from a Base64 encoded binary representation of a BitSet
	 *
	 * Attention: This is different from the constructor above, where the String is a bit string!
	 *
	 * @param s Base64 string
	 */
	@Override
	public void setValue(String s) {
		this.value = StringUtils.trimToEmpty(s);
		this.bitSet = value.isEmpty() ? null : base64ToBitSet(s);
	}

	/**
	 * Return the binary representation ob BitSet
	 * @return
	 */
	@Override
	public String toString() {
		return value;
	}

	@Override
	public HashedField clone() {
		return new HashedField((BitSet) this.getValue().clone());
	}
}