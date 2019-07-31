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

import java.util.Iterator;
import java.util.List;

import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;

/**
 * Represents a comparison between two input fields (Fields), usually belonging
 * to two Patient objects. Comparison methods, such as string comparison or
 * binary comparison (equal / not equal) are implemented as subclasses of this
 * class. Every concrete comparison (for example: compare first names of input
 * by JaroWinkler string metric) is represented by an object of this class.
 *
 * @param <F>
 *            The type of fields that can be compared. Implementing subclasses
 *            can override this to be more restrictive (e.g. see
 *            {@link StringNormalizer}).
 */
public abstract class FieldComparator<F extends Field<?>> {

	/** The first field to compare. */
	protected String fieldLeft;
	/** The second field to compare. */
	protected String fieldRight;
	/**
	 * Weight to assing for missing fields when comparing CompoundFields.
	 *
	 * @see #compareBackend(CompoundField, CompoundField)
	 */
	protected double missingWeight = 0.0;

	/**
	 * Default constructor. Usually the parametrized constructor should be used,
	 * but the default constructor makes sense for array comparisons, where the
	 * the comparison fields are changed in order to avoid the overhead of
	 * instantiating many FieldComparator objects.
	 *
	 */
	public FieldComparator()
	{
	}

	/**
	 * Instantiate comparison between two specified fields. The field
	 * definitions correspond to indices in the Fields map of the persons
	 * (objects of class Patient) which are compared.
	 *
	 * In many cases, subclasses will define constructors with additional
	 * arguments for setting comparator-specific parameters.
	 *
	 * @param fieldLeft
	 *            Name of comparison field on the left side.
	 * @param fieldRight
	 *            Name of comparison field on the right side.
	 */
	public FieldComparator(String fieldLeft, String fieldRight)
	{
		this.fieldLeft = fieldLeft;
		this.fieldRight = fieldRight;
	}

	/**
	 * Compare two patients on the fields specified by this FieldComparator.
	 *
	 * @param patientLeft
	 *            The left side patient.
	 * @param patientRight
	 *            The right side patient.
	 * @return The comparison result as a real number in the interval [0,1],
	 *         where 1 denotes equality and 0 maximal disagreement.
	 */
	@SuppressWarnings("unchecked")
	public double compare (Patient patientLeft, Patient patientRight)
	{
		Field<?> cLeft = patientLeft.getFields().get(this.fieldLeft);
		Field<?> cRight = patientRight.getFields().get(this.fieldRight);

		// these two methods should be evaluated, because they return different results
//		Field<?> cLeft = patientLeft.getInputFields().get(this.fieldLeft);
//		Field<?> cRight = patientRight.getInputFields().get(this.fieldRight);
		return this.compare((F) cLeft, (F) cRight);
	}

	/**
	 * Get the field of the left hand side patient to use in comparison.
	 *
	 * @return The field name.
	 */
	public String getFieldLeft() {
		return fieldLeft;
	}

	/**
	 * Set the field of the left hand side patient to use in comparison.
	 *
	 * @param fieldLeft
	 *            The field name.
	 */
	public void setFieldLeft(String fieldLeft) {
		this.fieldLeft = fieldLeft;
	}

	/**
	 * Get the field of the right hand side patient to use in comparison.
	 *
	 * @return The field name.
	 */
	public String getFieldRight() {
		return fieldRight;
	}

	/**
	 * Set the field of the right hand side patient to use in comparison.
	 *
	 * @param fieldRight
	 *            The field name.
	 */
	public void setFieldRight(String fieldRight) {
		this.fieldRight = fieldRight;
	}

	/**
	 * This is the workhorse of the comparator. Implementations should implement
	 * or interface their comparison logic (e.g. a string comparison algorithm)
	 * in this method.
	 *
	 * @param fieldLeft
	 *            The left hand side field to compare.
	 * @param fieldRight
	 *            The right hand side field to compare.
	 * @return The comparison result as a real number in the interval [0,1],
	 *         where 1 denotes equality and 0 maximal disagreement.
	 */
	public abstract double compareBackend(F fieldLeft, F fieldRight);

	/**
	 * Method to compare two fields. This method (a frontend to compareBackend)
	 * is necessary because Java uses compile-time-types of arguments for method
	 * dispatching. This method checks if the input fields are CompoundField or
	 * simple fields and calls the corresponding version of compareBackend. The
	 * former implementation with compare(F, F) and
	 * compare(CompoundField&lt;F&gt;, CompoundField&lt;F&gt;) lead to an
	 * exception in the following case, because the compare for simple fields
	 * would be called based on the compile-time-types Field&lt;?&gt; for the
	 * fields:
	 *
	 * <pre>
	 * {@code
	 * Field<?> field1 = new CompoundField<PlainTextField> (...); Field<?>;
	 * field1 = new CompoundField<PlainTextField>(...);
	 * comparator1.compare(field1, field2);
	 * }
	 * </pre>
	 *
	 * @param fieldLeft
	 *            The left hand side field to compare.
	 * @param fieldRight
	 *            The right hand side field to compare.
	 * @return The comparison result as a real number in the interval [0,1],
	 *         where 1 denotes equality and 0 maximal disagreement.
	 *
	 * @see "http://stackoverflow.com/questions/1572322/overloaded-method-selection-based-on-the-parameters-real-type"
	 */
	@SuppressWarnings("unchecked")
	public double compare(F fieldLeft, F fieldRight) {
		/* If one of the fields is empty, consider them as unequal
		 * Justification: Sariyar M, Borg A. Missing values in deduplication of electronic patient data.
		 * J Am Med Inform Assoc. 2012 Jun 1;19(e1):e76-e82.*/
		if (fieldLeft.isEmpty() || fieldRight.isEmpty())
			return 0.0;
		if (fieldLeft instanceof CompoundField<?> && fieldRight instanceof CompoundField<?>)
			return compareBackend((CompoundField<F>) fieldLeft, (CompoundField<F>) fieldRight);
		else
			return compareBackend(fieldLeft, fieldRight);

	}

	/**
	 * Default method for comparison of CompoundField. An implementatino of the
	 * algorithm for array comparisons used by Automatch and its successor
	 * QualityStage. See: Ascential QualityStage. Mathing Concepts and Reference
	 * Guide. Version 7.5, 5/19-5/20.
	 *
	 * @param fieldLeft
	 *            The left hand side field to compare.
	 * @param fieldRight
	 *            The right hand side field to compare.
	 * @return The comparison result as a real number in the interval [0,1],
	 *         where 1 denotes equality and 0 maximal disagreement.
	 */
	public double compareBackend(CompoundField<F> fieldLeft, CompoundField<F> fieldRight)
	{

		int nNonEmptyLeft = fieldLeft.getSize() - fieldLeft.nEmptyFields();
		int nNonEmptyRight = fieldLeft.getSize() - fieldRight.nEmptyFields();

		// let fieldsA be the array with less non-missing fields
		List<F> fieldsA;
		List<F> fieldsB;

		if (nNonEmptyLeft <= nNonEmptyRight)
		{
			fieldsA = fieldLeft.clone().getValue();
			fieldsB = fieldRight.clone().getValue();
		} else {
			fieldsB = fieldLeft.clone().getValue();
			fieldsA = fieldRight.clone().getValue();

		}
		double highestWeight;
		double numerator = 0.0;
		double denominator = Math.min(nNonEmptyLeft, nNonEmptyRight);
		F fieldWithMaxWeight = null;
		for (F oneFieldA : fieldsA)
		{
			if (oneFieldA.isEmpty()) continue;
			highestWeight = this.missingWeight;
			Iterator<F> fieldBIt = fieldsB.iterator();
			while (fieldBIt.hasNext())
			{
				F oneFieldB = fieldBIt.next();
				// do not consider empty fields
				if (oneFieldB.isEmpty())
				{
					fieldBIt.remove();
					continue;
				}
				double thisWeight = this.compare(oneFieldA, oneFieldB);
				if (thisWeight > highestWeight)
				{
					highestWeight = thisWeight;
					fieldWithMaxWeight = oneFieldB;
				}
			}
			if (highestWeight > this.missingWeight)
			{
				numerator += highestWeight;
				fieldsB.remove(fieldWithMaxWeight);
			}
		}
		return numerator / denominator;
	}
}
