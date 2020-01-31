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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.pseudonymisierung.mainzelliste.PlainTextField;

/**
 * Performs the following normalization on strings:
 * <ul>
 *   <li> Trailing delimiters such as blanks, colons, dashes etc. are removed.</li>
 *   <li> German umlauts are replaced: ä -> ae, ö -> oe etc., ß -> ss </li>
 *   <li> Conversion to uppercase. </li>
 * </ul>
 *
 */
public class StringNormalizer extends FieldTransformer<PlainTextField, PlainTextField> {

	/** Delimiters to remove from start and end. */
	private static char delimiterChars[] = {' ', '.', ':', ',', ';', '-', '\''};

	/** Characters which to replace (umlauts)
	 * {ä, Ä, ö, Ö, ü, Ü, ß}
	 * */
	private static char umlauts[] = {'\u00e4', '\u00c4', '\u00f6', '\u00d6', '\u00fc', '\u00dc', '\u00df'};

	/** Replacement for umlauts */
	private static String umlautReplacement[] = {"ae", "AE", "oe", "OE", "ue", "UE", "ss"};

	/** Delimiters to recognize when decomposing Names as Set.
	 * Used internally for efficient access to delimiters.
	 */
	private Set<Character> delimiters;

	/** Mapping between umlauts and their replacement */
	private Map<Character, String> umlautReplacementMap;


	/**
	 * Create an instance.
	 */
	public StringNormalizer()
	{
		int i;
		this.delimiters = new HashSet<Character>();
		for (i = 0; i < delimiterChars.length; i++)
		{
			delimiters.add(new Character(delimiterChars[i]));
		}

		this.umlautReplacementMap = new HashMap<Character, String>();
		for (i = 0; i < umlauts.length; i++ )
		{
			umlautReplacementMap.put(new Character(umlauts[i]), umlautReplacement[i]);
		}
	}


	/**
	 * Normalize a PlainTextField. Normalization includes:
	 * <ul>
	 * 	<li> removal of leading and trailing delimiters,
	 *  <li> conversion of Umlauts.
	 * 	<li> conversion to upper case,
	 * <ul>
	 */
	@Override
	public PlainTextField transform(PlainTextField input)
	{
		if (input == null) return null;
		if (input.getValue() == null) return new PlainTextField(null);
		if (input.getValue().length() == 0) return new PlainTextField("");

		// TODO: ungültige Zeichen filtern
		String inputStr = input.toString();
		StringBuffer resultString;

		// Copy into new String, omitting leading and trainling delimiters
		int start, end;
		for (start = 0; start < inputStr.length() && delimiters.contains(inputStr.charAt(start)); start++);
		for (end = inputStr.length() - 1; end >= start && delimiters.contains(inputStr.charAt(end)); end--);

		resultString = new StringBuffer(inputStr.substring(start,  end + 1));

		// if resultString is empty, nothing more to do
		if (resultString.length() == 0) return new PlainTextField("");

		// convert umlauts
		Character thisChar;
		for (int pos = 0; pos < resultString.length(); pos++)
		{
			thisChar = new Character(resultString.charAt(pos));
			if (umlautReplacementMap.containsKey(thisChar))
			{
				resultString.replace(pos,  pos + 1, umlautReplacementMap.get(thisChar));
			}
		}

		// convert to uppercase
		PlainTextField output = new PlainTextField(resultString.toString().toUpperCase());
		return output;

	}

	@Override
	public Class<PlainTextField> getInputClass()
	{
		return PlainTextField.class;
	}

	@Override
	public Class<PlainTextField> getOutputClass()
	{
		return PlainTextField.class;
	}
}
