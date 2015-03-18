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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.pseudonymisierung.mainzelliste.PlainTextField;

//FIXME: Kommentar
public class NGramComparator extends FieldComparator<PlainTextField> {

	private int nGramLength = 2;

	private static Map<String, Set<String>> cacheNGrams = new HashMap<String, Set<String>>(50000);
	
	private Set<String> getNGrams(String input){
		Set<String> cacheResult = cacheNGrams.get(input);
		if (cacheResult != null) return Collections.unmodifiableSet(cacheResult);

		// initialize Buffer to hold input and padding 
		// (nGramLength - 1 spaces on each side)
		StringBuffer buffer = new StringBuffer(input.length() + 2 * (nGramLength - 1));
		// Add leading padding
		for (int i = 0; i < nGramLength - 1; i++)
			buffer.append(" ");
		// add input string
		buffer.append(input);
		// add leading padding
		for (int i = 0; i < nGramLength - 1; i++)
			buffer.append(" ");

		HashSet<String> output = new HashSet<String>(buffer.length() - nGramLength + 1);
		for (int i = 0; i <= buffer.length() - nGramLength; i++)
		{
			output.add(buffer.substring(i, i + nGramLength));
		}
		cacheNGrams.put(new String(input), output);
		return Collections.unmodifiableSet(output);
	}

	public NGramComparator (String fieldLeft, String fieldRight)
	{
		super(fieldLeft, fieldRight);
	}

	@Override
	public double compareBackend(PlainTextField fieldLeft, PlainTextField fieldRight) {
		assert (fieldLeft instanceof PlainTextField);
		assert (fieldRight instanceof PlainTextField);
		
		Set<String> nGramsLeft = getNGrams(fieldLeft.getValue());
		Set<String> nGramsRight = getNGrams(fieldRight.getValue());
		
		int nLeft = nGramsLeft.size();
		int nRight = nGramsRight.size();
		
		int nCommon = 0;
		Set<String> smaller;
		Set<String> larger;
		
		if (nLeft < nRight) {
			smaller = nGramsLeft;
			larger = nGramsRight;			
		} else {
			smaller = nGramsRight;
			larger = nGramsLeft;
		}
		
		for (String str : smaller) {
			if (larger.contains(str)) nCommon++;
		}
		
		return 2.0 * nCommon / (nLeft + nRight);
	}

}
