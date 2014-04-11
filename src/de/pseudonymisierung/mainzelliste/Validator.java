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
package de.pseudonymisierung.mainzelliste;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ws.rs.core.MultivaluedMap;
import org.apache.log4j.Logger;


import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.ValidatorException;

/**
 * Form validation.
 * Validation checks are stored in a Properties object passed to the constructor.
 * Supported checks:
 * 
 * <ul>
 * 	<li> Check required fields (i.e. not empty): validator.field.<i>fieldname</i>.required marks
 * 		field <i> fieldname</i> as required.
 *  <li> Check format: validator.field.<i>fieldname</i>.format defines a regular expression against
 *  	which the specified field is checked.
 *  <li>
 * </ul>
 */
public enum Validator {

	instance;

	private Set<String> requiredFields = new HashSet<String>();
	private Map<String, String> formats = new HashMap<String, String>();
	private List<List<String>> dateFields = new LinkedList<List<String>>();
	private List<String> dateFormat = new LinkedList<String>();
	private Logger logger = Logger.getLogger(this.getClass());
	
	private Validator() {

		Properties props = Config.instance.getProperties();
		
		Pattern pRequired = Pattern.compile("^validator\\.field\\.(\\w+)\\.required");
		Pattern pFormat = Pattern.compile("^validator\\.field\\.(\\w+)\\.format");
		Pattern pDateFields = Pattern.compile("^validator\\.date\\.(\\d+).fields");
		java.util.regex.Matcher m;
		
		for (Object thisPropKeyObj : props.keySet()) {
			String thisPropKey = (String) thisPropKeyObj;
			
			// Look for required fields
			m = pRequired.matcher(thisPropKey);
			if (m.find())
			{
				requiredFields.add(m.group(1).trim());
			}
			
			// Look for format definitions
			m = pFormat.matcher(thisPropKey);
			
			if (m.find())
			{
				String fieldName = m.group(1);
				String format = props.getProperty(thisPropKey).trim();
				// Check if format is a valid regular expression
				try {
					Pattern.compile(format);
				} catch (PatternSyntaxException e) {
					throw new InternalErrorException(e);
				}				
				formats.put(fieldName, format);
			}

			// Look for format definitions
			m = pDateFields.matcher(thisPropKey);
			if (m.find())
			{
				try {
					int dateInd = Integer.parseInt(m.group(1));
					List<String> theseFields = new LinkedList<String>();
					for (String thisFieldName : props.getProperty("validator.date." + dateInd + ".fields").split(",")) {
						theseFields.add(thisFieldName.trim());
					}
					dateFields.add(theseFields);
					dateFormat.add(props.getProperty("validator.date." + dateInd + ".format").trim());
					} catch (NumberFormatException e) {
					throw new InternalErrorException(e);
				}
			}
			
		}		
	}
	
	public void validateField(String key, String value) {
		
		if (requiredFields.contains(key)) {
			if (value == null || value.equals("")) {
				throw new ValidatorException("Field " + key + " must not be empty!");
			}
		}

		if (formats.containsKey(key)) {
			String format = formats.get(key);
			if (value != null && !value.equals("") && !Pattern.matches(format, value)) {
				throw new ValidatorException("Field " + key + 
						" does not conform to the required format" + format);
			}
		}
	}
	
	public void validateDates(MultivaluedMap<String, String> form) {
		assert dateFields.size() == dateFormat.size();
		Iterator<List<String>> fieldIt = dateFields.iterator();
		Iterator<String> formatIt = dateFormat.iterator();
		
		while (fieldIt.hasNext()) {
			SimpleDateFormat sdf = new SimpleDateFormat(formatIt.next());
			sdf.setLenient(false);
			StringBuffer dateString = new StringBuffer();
			for (String dateElement : fieldIt.next()) {
				dateString.append(form.getFirst(dateElement));
			}
			try {
				Date date = sdf.parse(dateString.toString()); 
				if (date == null)
					throw new ValidatorException(dateString + " is not a valid date!");
			} catch (ParseException e) {
				throw new ValidatorException(dateString + " is not a valid date!");
			}
		}
		
	}
	
	public void validateForm(MultivaluedMap<String, String> form) {
		
		// Check if all fields are present as values (whether they are empty or not)
		for(String s: Config.instance.getFieldKeys()){
			if (!form.containsKey(s)) {
				logger.error("Field " + s + " not found in input data!");
				throw new ValidatorException("Field " + s + " not found in input data!");
			}
		}
		
		for (String key : form.keySet()) {
			for (String value : form.get(key)) {
				validateField(key, value);
			}			
		}
		validateDates(form);
		
	}
}
