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

/**
 * Represents a pair of Strings. Used for implementation
 * of 2D maps as Map<StringPair, ?>.
 */
public class StringPair {

	/** The first string. */
	private String str1;
	/** The second string. */
	private String str2;

	/**
	 * Create an instance with the given values.
	 * @param str1 First string to assign.
	 * @param str2 Second string to assign.
	 */
	public StringPair(String str1, String str2) {
		this.str1 = str1;
		this.str2 = str2;
	}

	/**
	 * Computes the hash code by concatenating the members, separated by ",",
	 * and computing the hash code on the result.
	 */
	@Override
	public int hashCode()
	{
		String hashStr = str1 + "," + str2;
		return hashStr.hashCode();
	}

	/**
	 * Two StringPairs are equal if the respective members are equal (checked by .equals).
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof StringPair))
			return false;

		StringPair other = (StringPair)obj;
		return str1.equals(other.str1) && str2.equals(other.str2);
	}
}
