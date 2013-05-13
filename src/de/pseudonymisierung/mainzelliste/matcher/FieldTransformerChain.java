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

import java.util.List;
import java.util.Vector;


import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.exceptions.IncompatibleFieldTypesException;


/** 
 * Implements a chain of several FieldTransformers applied one after another
 */
public class FieldTransformerChain {
	
	private List<FieldTransformer> transformers;
	
	public FieldTransformerChain()
	{
		this.transformers = new Vector<FieldTransformer>();
	}

	public Class<?> getInputClass()
	{
		if (this.transformers.size() == 0)
			return null;
		
		return this.transformers.get(0).getInputClass();
	}
	
	public Class<?> getOutputClass()
	{
		if (this.transformers.size() == 0)
			return null;
		
		return this.transformers.get(this.transformers.size() - 1) .getInputClass();
	}

	public void add(FieldTransformer<Field<?>, Field<?>>... toAdd) throws IncompatibleFieldTypesException
	{
		for (FieldTransformer transformer : toAdd)
		{
			// output class of a transformer must be a subclass of input class of the
			// next transformer
			if (this.transformers.size() != 0 && !transformer.getInputClass().isAssignableFrom(this.getOutputClass()))
			{
				throw new IncompatibleFieldTypesException(this.transformers.get(this.transformers.size() - 1), 
						transformer);
			} else
			{
				this.transformers.add(transformer);
			}
		}
	}
	
	public Field<?> transform(Field<?> input)
	{
		Field<?> result = input.clone();
		for (FieldTransformer<Field<?>, Field<?>> transformer : this.transformers)
		{
			if (result instanceof CompoundField)
			{
				CompoundField cf = transformer.transform((CompoundField) result);
				result = cf;
			} else {
				result = transformer.transform(result);
			}
		}
		
		return result;
	}

}
