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

import java.util.Set;
import java.util.HashSet;

import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.PlainTextField;


/**
 * Decomposition of last name into components (3 by default) with recognition of
 * typical German family particles. Any substring that matches the regular
 * expression "[ \\.:,;\\-']+" is regarded as delimiter. Name affixes, such as
 * "von", "Freiherr" etc. are added, in the order of their appearance, to the
 * beginning of the last component.
 */
public class GermanLastNameDecomposer extends FieldTransformer<PlainTextField, CompoundField<PlainTextField>>{

	/** The number of components to split into. */
	private int nCcomponents = 3;

	/** Delimiters to recognize when decomposing Names as regular expression. */
	private String delimiters = "[ \\.:,;\\-']+";

	/** Typical German name affixes. */
	private static String nameParticles[] = {"AL", "AM", "AN", "AUF",
		"D","DA", "DE", "DEL", "DELA", "DEM", "DEN", "DER", "DI", "DOS", "DR", "DU",
		"EL", "EN", "ET",
		"FREIFRAU", "FREIHERR",
		"GRAEFIN", "GRAF",
		"LA", "LE",
		/*"MAC",*/ "MC", "MED",
		"O",
		"PD", "PROF",
		"SR",
		"UND",
		"V", "VAN", "VO", "VOM", "VON",
		"Y",
		"ZU", "ZUM", "ZUR" };

	/** Filled with the members of {@link #nameParticleSet} for efficient access. */
	private Set<String> nameParticleSet;
	/** Number of components to split into. */
	private int nComponents = 3;

	/** Create an instance. */
	public GermanLastNameDecomposer()
	{
		this.nameParticleSet = new HashSet<String>();
		for (String particle : nameParticles)
		{
			nameParticleSet.add(particle);
		}

	}
	@Override
	public CompoundField<PlainTextField> transform(PlainTextField input)
	{
		CompoundField<PlainTextField> output = new CompoundField<PlainTextField>(nComponents);
		String substrings[] = input.getValue().split(delimiters);
		StringBuffer particles = new StringBuffer();
		StringBuffer otherComponents = new StringBuffer(); // collects all components > nComponents

		int i = 0;
		for (String thisSubstr : substrings)
		{
			// collect name particles ("von", "zu") separately
			if (nameParticleSet.contains(thisSubstr.toUpperCase()))
			{
				if (particles.length() > 0) particles.append(" ");
				particles.append(thisSubstr);
				continue;
			}
			// Collect other components
			if (i < nComponents - 1)
			{
				output.setValueAt(i, new PlainTextField(thisSubstr));
				i++;
			} else {
				otherComponents.append(" ");
				otherComponents.append(thisSubstr);
			}
		}
		// fill remaining fields with empty Strings
		for (;i < nComponents - 1; i++)
			output.setValueAt(i, new PlainTextField(""));
		// add particles to last component
		if (otherComponents.length() > 0)
			otherComponents.append(" ");
		otherComponents.append(particles);
		output.setValueAt(nCcomponents - 1, new PlainTextField(otherComponents.toString()));
		return output;

	}

	@Override
	public Class<PlainTextField> getInputClass()
	{
		return PlainTextField.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<CompoundField<PlainTextField>> getOutputClass()
	{
		return (Class<CompoundField<PlainTextField>>) new CompoundField<PlainTextField>(3).getClass();
	}
}
