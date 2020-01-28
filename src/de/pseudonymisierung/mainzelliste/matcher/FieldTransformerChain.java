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

import java.util.List;
import java.util.Vector;


import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.exceptions.IncompatibleFieldTypesException;


/**
 * Implements a chain of several FieldTransformers applied one after another.
 * This is used to combine for each field the field tranformations defined in
 * the configuration.
 */
public class FieldTransformerChain {

	/** The chain of FieldTransformers in the order they are applied. */
	private List<FieldTransformer<Field<?>, Field<?>>> transformers;

	/**
	 * Creates an empty instance.
	 */
	public FieldTransformerChain()
	{
		this.transformers = new Vector<FieldTransformer<Field<?>, Field<?>>>();
	}

	/**
	 * Get the input type of this FieldTransformerChain. This is always a
	 * subclass of Field and equal to the input type of the first
	 * FieldTransformer in this chain.
	 *
	 * @return A Class object that represents the input type of this
	 *         FieldTransformerChain or null if no FieldTransformers are
	 *         defined.
	 */
	public Class<Field<?>> getInputClass()
	{
		if (this.transformers.size() == 0)
			return null;

		return this.transformers.get(0).getInputClass();
	}

	/**
	 * Get the output type of this FieldTransformerChain. This is always a
	 * subclass of Field and equal to the output type of the last
	 * FieldTransformer in this chain.
	 *
	 * @return A Class object that represents the output type of this
	 *         FieldTransformerChain or null if no FieldTransformers are
	 *         defined.
	 */
	public Class<?> getOutputClass()
	{
		if (this.transformers.size() == 0)
			return null;

		return this.transformers.get(this.transformers.size() - 1) .getInputClass();
	}

	/**
	 * Add a FieldTransformer to this chain. It will be appended to the end of
	 * the chain.
	 *
	 * @param toAdd
	 *            The FieldTransformer to add.
	 * @throws IncompatibleFieldTypesException
	 *             If the input field of toAdd does not match the output type of
	 *             this chain.
	 */
	public void add(FieldTransformer<Field<?>, Field<?>>... toAdd) throws IncompatibleFieldTypesException
	{
		for (FieldTransformer<Field<?>, Field<?>> transformer : toAdd)
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

	/**
	 * Transform a Field with this chain. The input field is copied by calling
	 * {@code input.clone()} before applying the transformations.
	 *
	 * @param input
	 *            The field to transform.
	 * @return The transformed field or a copy of the input, if no
	 *         FieldTransformers are defined in this chain.
	 */
	public Field<?> transform(Field<?> input)
	{
		Field<?> result = input.clone();
		for (FieldTransformer<Field<?>, Field<?>> transformer : this.transformers)
		{
			if (result instanceof CompoundField)
			{
				@SuppressWarnings("unchecked")
				CompoundField<Field<?>> cf = transformer.transform((CompoundField<Field<?>>) result);
				result = cf;
			} else {
				result = transformer.transform(result);
			}
		}

		return result;
	}

}
