/*
 * Copyright (C) 2013 Martin Lablans, Andreas Borg, Frank Ückert
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

import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.PlainTextField;


/**
 * Decomposition of first name into components (3 by default)
 */
public class FirstNameDecomposer extends FieldTransformer<PlainTextField, CompoundField<PlainTextField>> {

	/** Delimiters to recognize when decomposing Names as regular expression. */
	private String delimiters = "[ \\.:,;\\-']+";

	private int nComponents = 3;
	
	@Override
	public CompoundField<PlainTextField> transform(PlainTextField input)
	{
		CompoundField<PlainTextField> output = new CompoundField<PlainTextField>(nComponents);
		String substrings[] = input.getValue().split(delimiters, nComponents);
		int i;
		for (i = 0; i < substrings.length; i++)
			output.setValueAt(i, new PlainTextField(substrings[i]));
		// fill remaining fields with empty Strings
		for (;i < nComponents; i++)
			output.setValueAt(i, new PlainTextField(""));
		return output;
	}
	
	@Override
	public Class<PlainTextField> getInputClass()
	{
		return PlainTextField.class;
	}
	
	@Override
	public Class<CompoundField<PlainTextField>> getOutputClass()
	{
		return (Class<CompoundField<PlainTextField>>) new CompoundField<PlainTextField>(3).getClass();
	}
	
	public static void main(String args[])
	{
		FirstNameDecomposer dec = new FirstNameDecomposer();
		System.out.println(dec.getOutputClass());
		
		BloomFilterTransformer bf = new BloomFilterTransformer();
		System.out.println(bf.getOutputClass());
		
	}
}
