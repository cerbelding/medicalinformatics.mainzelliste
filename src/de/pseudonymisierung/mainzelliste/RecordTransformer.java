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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.*;

/**
 * A RecordTransformer applies all configured FieldTranformers to the fields of
 * an input record.
 */
public class RecordTransformer {

	/**
	 * Map of field transformers. Keys are the field names, values the
	 * corresponding FieldTransformer objects.
	 */
	private Map<String, FieldTransformerChain> fieldTransformers;

	/**
	 * Create an instance from the configuration.
	 *
	 * @param props
	 *            The configuration of the Mainzelliste instance as provides by
	 *            {@link Config}.
	 *
	 * @throws InternalErrorException
	 *             If an error occurs during initalization. A typical cause is
	 *             when a configured FieldTransformer class cannot be found on
	 *             the class path.
	 */
	@SuppressWarnings("unchecked")
	public RecordTransformer(Properties props) throws InternalErrorException {
		fieldTransformers = new HashMap<String, FieldTransformerChain>();

		// Get names of fields from config vars.*
		Pattern p = Pattern.compile("^field\\.(\\w+)\\.type");
		java.util.regex.Matcher m;

		// Build map of comparators and map of frequencies from Properties
		for (Object key : props.keySet()) {
			m = p.matcher((String) key);
			if (m.find()) {
				String fieldName = m.group(1);
				String transformerProp = props.getProperty("field." + fieldName + ".transformers");
				if (transformerProp != null)
				{
					String transformers[] = transformerProp.split(",");
					FieldTransformerChain thisChain = new FieldTransformerChain();
					for (String thisTrans : transformers) {
						thisTrans = thisTrans.trim();
						try {
							FieldTransformer<Field<?>, Field<?>> tr = (FieldTransformer<Field<?>, Field<?>>) Class.forName("de.pseudonymisierung.mainzelliste.matcher." + thisTrans).newInstance();
							thisChain.add(tr);
						} catch (Exception e)
						{
							System.err.println(e.getMessage());
							throw new InternalErrorException();
						}
					}
					this.fieldTransformers.put(fieldName, thisChain);
				}
			}
		}
	}

	/**
	 * Transforms a patient by transforming all of its fields. Fields for which
	 * no transformer is found (i.e. the field name is not in .keySet()) are
	 * passed unchanged, as well as IDs.
	 * @param input The record to transform.
	 * @return The transformed record.
	 */
	public Patient transform(Patient input) {
		Map<String, Field<?>> inFields = input.getFields();
		Patient output = new Patient();
		output.setIds(new HashSet<>(input.getIds()));
		HashMap<String, Field<?>> outFields = new HashMap<String, Field<?>>();
		/* iterate over input fields and transform each */
		for (String fieldName : inFields.keySet()) {
			if (this.fieldTransformers.containsKey(fieldName))
				outFields.put(fieldName, this.fieldTransformers.get(fieldName).transform(inFields.get(fieldName)));
			else
				outFields.put(fieldName, inFields.get(fieldName).clone());
		}
		output.setFields(outFields);
		return output;
	}
}
