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
import java.text.ParsePosition;

/**
 * Form validation. Validation checks are stored in a Properties object passed
 * to the constructor. Implemented as a singleton object, which can be
 * referenced by Validator.instance. Supported checks:
 *
 * <ul>
 * <li>Check required fields (i.e. not empty):
 * validator.field.<i>fieldname</i>.required marks field <i> fieldname</i> as
 * required.
 * <li>Check format: validator.field.<i>fieldname</i>.format defines a regular
 * expression against which the specified field is checked.
 * <li>
 * </ul>
 */
public enum Validator {

	/** The singleton instance. */
	instance;

	/** List of names of fields that are required for a patient. */
	private Set<String> requiredFields = new HashSet<String>();
	/** Format of field. Keys are field names, values are regular expressions. */
	private Map<String, String> formats = new HashMap<String, String>();
	/** Every entry denotes a set of fields that form a date. */
	private List<List<String>> dateFields = new LinkedList<List<String>>();
	/** For every entry in {@link #formats}, the date format string. */
	private List<String> dateFormat = new LinkedList<String>();
	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Initalize the singleton. Reads validation properties from the
	 * configuration.
	 */
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

	/**
	 * Validate a field.
	 *
	 * @param key
	 *            Field name.
	 * @param value
	 *            Field value.
	 * @throws ValidatorException
	 *             If the field has not the required format.
	 */
	public void validateField(String key, String value) throws ValidatorException {

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

	/**
	 * Check if it is needed to validate Dates.
	 */
	private <T extends Map<?, ?>> boolean hasToValidateDates(T form) {
		for (List<String> date : this.dateFields) {
			for (String fieldName : date) {
				if (form.containsKey(fieldName)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Validates dates in input form according to format definition in
	 * configuration.
	 *
	 * @param form
	 *            Form with input fields as provided by the HTTP request.
	 * @throws ValidatorException
	 *             If form contains an illegal date or a date field is missing.
	 */
	public void validateDates(MultivaluedMap<String, String> form) throws ValidatorException {
		if (hasToValidateDates(form)) {
			// List to collect all dates in the form
			List<String> dateStrings = new LinkedList<String>();
			for (List<String> thisDateFields : this.dateFields) {
				StringBuffer dateString = new StringBuffer();
				for (String fieldName : thisDateFields) {
					if (!form.containsKey(fieldName)) {
						throw new ValidatorException(
								String.format(
										"Field %s is missing in date definition. Dates must be entered and updated in complete form.",
										fieldName));
					}
					dateString.append(form.getFirst(fieldName));
				}
				dateStrings.add(dateString.toString());
			}
			checkDates(this.dateFormat, dateStrings);
		}
	}

	/**
	 * Validates dates in input form according to format definition in
	 * configuration.
	 *
	 * @param form
	 *            Input fields, keys are field names, values the respective field values.
	 * @throws ValidatorException
	 *             If form contains an illegal date or a date field is missing.
	 */
	public void validateDates(Map<String, String> form) throws ValidatorException {
        if (hasToValidateDates(form)) {
            // List to collect all dates in the form
            List<String> dateStrings = new LinkedList<String>();
            for (List<String> thisDateFields : this.dateFields) {
                StringBuffer dateString = new StringBuffer();
                // check date only if it was entered/changed
                boolean checkDate = false;
                for (String fieldName : thisDateFields) {
                    if (form.containsKey(fieldName)) {
                        checkDate = true;
                        break;
                    }
                }
                if (checkDate) {
                    // check date only if it is complete
                    for (String fieldName : thisDateFields) {
                        if (!form.containsKey(fieldName)) {
                            throw new ValidatorException(
                                    String.format(
                                            "Field %s is missing in date definition. Dates must be entered and updated in complete form.",
                                            fieldName));
                        }
                        dateString.append(form.get(fieldName));
                    }
                    dateStrings.add(dateString.toString());
                }
            }
            checkDates(this.dateFormat, dateStrings);
        }
	}

	/**
	 * Validate input form according to the format definitions in the
	 * configuration.
	 *
	 * @param form
	 *            Form with input fields as provided by the HTTP request.
	 * @param checkFieldKeys
	 *            Whether to check if all configured fields are present in
	 *            {@code form}.
	 * @throws ValidatorException
	 *             If the form contains an error.
	 */
	public void validateForm(MultivaluedMap<String, String> form, boolean checkFieldKeys) throws ValidatorException {
		// Check that all required fields are present in form
		if (checkFieldKeys)
			checkFieldKeys(form);
		// Check fields values
		for (String key : form.keySet()) {
			for (String value : form.get(key)) {
				validateField(key, value);
			}
		}
		validateDates(form);
	}

	/**
	 * Validate input form according to the format definitions in the
	 * configuration.
	 *
	 * @param form
	 *            Input fields, keys are field names, values the respective
	 *            field values.
	 * @param checkFieldKeys
	 *            Whether to check if all configured fields are present in
	 *            {@code form}.
	 * @throws ValidatorException
	 *             If the form contains an error.
	 */
	public void validateForm(Map<String, String> form, boolean checkFieldKeys) throws ValidatorException {
		// Check that all fields are present in form
		if (checkFieldKeys)
			checkFieldKeys(form);
		// Check fields values
		for (String key : form.keySet()) {
			validateField(key, form.get(key));
		}
		validateDates(form);
	}

	/**
	 * Check if all configured fields are present in the input.
	 *
	 * @param form
	 *            Input form, either a mapping field name-> value or the
	 *            MultiValuedMap as read from the HTTP request.
	 * @throws ValidatorException
	 *             If a configured field is missing in the map keys.
	 */
	private void checkFieldKeys(Map<String, ?> form) throws ValidatorException {
		for(String s: requiredFields){
			if (!form.containsKey(s)) {
				logger.error("Required field " + s + " not found in input data!");
				throw new ValidatorException("Required field " + s + " not found in input data!");
			}
		}
	}

	/**
	 * Check date strings against format strings.
	 *
	 * @param formatStrings
	 *            Date format strings, e.g. "dd.mm.YYYY".
	 * @param dateStrings
	 *            Date strings, e.g. "30.01.1951".
	 *
	 * @see SimpleDateFormat
	 */
	private void checkDates(Iterable<String> formatStrings, Iterable<String> dateStrings) {
		Iterator<String> formatIt = formatStrings.iterator();
		Iterator<String> dateIt = dateStrings.iterator();
		Date currentDate = new Date();

		while (formatIt.hasNext() && dateIt.hasNext()) {
			String curDateFormat = formatIt.next();
			SimpleDateFormat sdf = new SimpleDateFormat(curDateFormat);
			sdf.setLenient(false);
			String dateString = dateIt.next();
			try {
				ParsePosition position = new ParsePosition(0);
				Date date = sdf.parse(dateString, position);
				if (position.getIndex() != curDateFormat.length()) {
					throw new ParseException(String.format("Unparseable date: %s", dateString), position.getIndex());
				}
				if (date == null) {
					throw new ValidatorException(dateString + " is not a valid date!");
				}
				if (date.after(currentDate))
					throw new ValidatorException(dateString + " is in the future!");
			} catch (ParseException e) {
				throw new ValidatorException(dateString + " is not a valid date!");
			}
		}
	}

    /**
     * Return the names of fields that are configured as required.
     * 
     * @return The set of names of required fields (empty if no required fields
     *         exist).
     */
    public Set<String> getRequiredFields() {
        return requiredFields;
    }
}
