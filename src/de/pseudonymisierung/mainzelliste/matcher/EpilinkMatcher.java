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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.log4j.Logger;


import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Validator;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Performs record linkage by using the algorithm of Epilink et al. This is a
 * simple weight based algorithm (similar to the method of Fellegi and Sunter)
 * with support for string metrics.
 *
 * The weight for a record pair (x1,x2) is computed by the formula
 *
 * sum_i (w_1 * s(x1_i, x2_i)) / sum_i w_i where s(x1_i, x2_i) is the value of a
 * string comparison of records x1 and x2 in the i-th field and w_i is a
 * weighting factor computed by
 *
 * w_i = log_2 (1-e_i) / f_i
 *
 * where f_i denotes the average frequency of values and e_i the estimated error
 * rate for field i.
 *
 * @see "P. Contiero et al., The EpiLink record linkage software, in: Methods of Information in Medicine 2005, 44 (1), 66–71."
 *
 *
 */
public class EpilinkMatcher implements Matcher {

	/** Minimum weight for definitive matches. */
	private double thresholdMatch;

	/**
	 * Get the minimum weight for definitive matches.
	 *
	 * @return The minimum weight for matches, a number in the range [0,1].
	 */
	public double getThresholdMatch() {
		return thresholdMatch;
	}

	/**
	 * Get the minimum weight for possible matches.
	 *
	 * @return The minimum weight for possible matches, a number in the range
	 *         [0,1].
	 */
	public double getThresholdNonMatch() {
		return thresholdNonMatch;
	}

	/** The minimum weight for possible matches. */
	private double thresholdNonMatch;

	/** The FieldComparators, by field name. */
	private Map<String, FieldComparator<Field<?>>> comparators;
	/** Mean frequencies of values by field name. */
	private Map<String, Double> frequencies;
	/** Assumed error rates by field name. */
	private Map<String, Double> errorRates;

	/**
	 * Field weights by field name. Calculated from {@link #frequencies} and
	 * {@link #errorRates}
	 */
	private Map<String, Double> weights;

	/** Sets of fields that are exchangeable for matching. */
	private List<List<String>> exchangeGroups;
	/** Buffer for fields not included in any exchange group. */
	private Set<String> nonExchangeFields;

	/** The logging instance. */
	private Logger logger = Logger.getLogger(EpilinkMatcher.class);

	private String blockingSpeedOptimization;
	private String[] blockingFields;

	@Override
	public void initialize(Properties props) throws InternalErrorException
	{
		// Get error rate (is needed for weight computation below)

		// Initialize internal maps
		this.comparators = new HashMap<String, FieldComparator<Field<?>>>();
		this.frequencies = new HashMap<String, Double>();
		this.errorRates = new HashMap<String, Double>();
		this.weights = new HashMap<String, Double>();

		// Get names of fields from config vars.*
		Pattern p = Pattern.compile("^field\\.(\\w+)\\.type");
		java.util.regex.Matcher m;

		// Build maps of comparators, frequencies, error rates and attribute weights from Properties
		for (Object key : props.keySet())
		{
			m = p.matcher((String) key);
			if (m.find()){
				String fieldName = m.group(1);
				logger.info("Initializing properties for field " + fieldName);
				String fieldCompStr = props.getProperty("field." + fieldName + ".comparator");
				if (fieldCompStr != null)
				{
					fieldCompStr = fieldCompStr.trim();
					try {
						@SuppressWarnings("unchecked")
						Class<FieldComparator<Field<?>>> fieldCompClass = (Class<FieldComparator<Field<?>>>) Class.forName("de.pseudonymisierung.mainzelliste.matcher." + fieldCompStr);
						Constructor<FieldComparator<Field<?>>> fieldCompConstr = fieldCompClass.getConstructor(String.class, String.class);
						FieldComparator<Field<?>> fieldComp = fieldCompConstr.newInstance(fieldName, fieldName);
						comparators.put(fieldName, fieldComp);
					} catch (Exception e) {
						System.err.println(e.getMessage());
						throw new InternalErrorException();
					}
					// set error rate
					double error_rate = Double.parseDouble(props.getProperty("matcher.epilink."+ fieldName + ".errorRate"));
					errorRates.put(fieldName, error_rate);
					// set frequency
					double frequency = Double.parseDouble(props.getProperty("matcher.epilink." + fieldName + ".frequency"));
					frequencies.put(fieldName, frequency);
					// calculate field weights
					// log_2 ((1 - e_i) / f_i)
					// all e_i have same value in this implementation
					double weight = (1 - error_rate) / frequency;
					weight = Math.log(weight) / Math.log(2);
					logger.debug("Field weight for field " + fieldName + ": " + weight);
					weights.put(fieldName, weight);
				}
			}
		}
		// assert that Maps have the same keys
		assert(frequencies.keySet().equals(comparators.keySet()));
		assert(frequencies.keySet().equals(weights.keySet()));

		// load other config vars
		this.thresholdMatch = Double.parseDouble(props.getProperty("matcher.epilink.threshold_match"));
		this.thresholdNonMatch = Double.parseDouble(props.getProperty("matcher.epilink.threshold_non_match"));

		if (props.getProperty("blocking.soundex.type") != null) {
			this.blockingSpeedOptimization = props.getProperty("blocking.soundex.type");
			this.blockingFields = Arrays.stream(props.getProperty("blocking.soundex.fields").split(","))
					.map(String::trim)
					.toArray(String[]::new);
		}

		// initialize exchange groups
		//TODO Mechanismus generalisieren für andere Matcher
		this.nonExchangeFields = new HashSet<String>(this.weights.keySet());
		this.exchangeGroups = new Vector<List<String>>();
		for (int i = 0; props.containsKey("exchangeGroup." + i); i++)
		{
			String exchangeFields[] = props.getProperty("exchangeGroup." + i).split(" *[;,] *");
			for (String fieldName : exchangeFields) {
				this.nonExchangeFields.remove(fieldName);
				fieldName = fieldName.trim();
			}
			this.exchangeGroups.add(new Vector<String>(Arrays.asList(exchangeFields)));
		}
	}


	@Override
	public MatchResult match(Patient patient, Iterable<Patient> patientList) {

		if (blockingSpeedOptimization != null && blockingSpeedOptimization.equalsIgnoreCase("Soundex")) {
			return matchAlgorithmSoundexBlocked(patient, patientList, blockingFields);
		} else {
			return matchAlgorithmClassic(patient, patientList);
		}

	}

	public MatchResult matchAlgorithmClassic(Patient patient, Iterable<Patient> patientList) {


		MatchTempResult tempMatchResult = new MatchTempResult();


		for (Patient patientFromList : patientList)
		{
			// assert that the persons have the fields required for matching
			if (assertPatientHasRequiredMatchingFields(patient, patientFromList))
				continue;

			tempMatchResult = calculateBestMatches(patient, patientFromList, tempMatchResult);

		}

		return getMatchResult(tempMatchResult.getBestMatch(), tempMatchResult.getBestWeight(), tempMatchResult.getPossibleMatches());

	}

	private MatchResult matchAlgorithmSoundexBlocked(Patient patient, Iterable<Patient> patientList, String[] blockingFields) {


		MatchTempResult tempMatchResult = new MatchTempResult();


		////////////////////////////////////////////////////////////////////
		//check equality of soundex codes
		boolean equalClusterId;

		patient.setClusterIdByField(blockingFields);
		for (Patient patientFromList : patientList)
		{
			// assert that the persons have the fields required for matching
			if (assertPatientHasRequiredMatchingFields(patient, patientFromList))
				continue;

			equalClusterId= false;
			patientFromList.setClusterIdByField(blockingFields);

			for(String f :blockingFields)
			{
				if(patient.getClusterIds().get(f).equals(patientFromList.getClusterIds().get(f)))
				{
					equalClusterId= true;
					continue;
				}
			}

			if(!equalClusterId)
			{
				continue;
			}

			tempMatchResult = calculateBestMatches(patient, patientFromList, tempMatchResult);

		}

		return getMatchResult(tempMatchResult.getBestMatch(), tempMatchResult.getBestWeight(), tempMatchResult.getPossibleMatches());
	}

	private boolean assertPatientHasRequiredMatchingFields(Patient patient, Patient b) {
		return !Stream.of(patient, b).allMatch(p -> p.getFields().keySet().containsAll(Validator.instance.getRequiredFields()));
	}

	private MatchTempResult calculateBestMatches(Patient inputPatient, Patient referencePatient, MatchTempResult tempMatchResult) {
		double weight = calculateWeight(inputPatient, referencePatient);

		if (weight > tempMatchResult.getBestWeight())
		{
			tempMatchResult.setBestWeight(weight);
			tempMatchResult.setBestMatch(referencePatient);
		}

		tempMatchResult.setPossibleMatches(addPatientIfPossibleMatch(tempMatchResult.getPossibleMatches(), referencePatient, weight));
		return tempMatchResult;
	}

	private MatchResult getMatchResult(Patient bestMatch, double bestWeight, TreeMap<Double, List<Patient>> possibleMatches) {
		MatchResult result;
		if (bestWeight >= thresholdMatch) {
			result = new MatchResult(MatchResultType.MATCH, bestMatch, bestWeight);
		} else if (bestWeight < thresholdMatch && bestWeight > thresholdNonMatch) {
			result = new MatchResult(MatchResultType.POSSIBLE_MATCH, bestMatch, bestWeight);
		} else {
			result = new MatchResult(MatchResultType.NON_MATCH, null, bestWeight);
		}
		result.setPossibleMatches(possibleMatches.descendingMap());
		return result;
	}

	private TreeMap<Double, List<Patient>> addPatientIfPossibleMatch(TreeMap<Double, List<Patient>> possibleMatches, Patient patientFromList, double weight) {

		if (weight <= thresholdMatch && weight > thresholdNonMatch) {
			if (!possibleMatches.containsKey(weight))
				possibleMatches.put(weight, new LinkedList<Patient>());
			possibleMatches.get(weight).add(patientFromList);

			return possibleMatches;
		}
		else{
			return possibleMatches;
		}
	}


	/**
	 * Calculate the matching weight for two records.
	 * @param left The left hand side record.
	 * @param right The right hand side record.
	 * @return The matching weight, a number in the range [0,1].
	 */
	public double calculateWeight(Patient left, Patient right)
	{

		double weightSum = 0; // holds sum of field weights
		double totalWeight = 0; // holds total weight

		// process exchange groups
		for (List<String> exchangeGroup : this.exchangeGroups)
		{
			/*
			 * Remove empty fields from both sides until one side has
			 * no more empty fields
			 */
			// Make copies of field maps for manipulation
			Map<String, Field<?>> fieldsToCompareRight = new HashMap<String, Field<?>>();
			Map<String, Field<?>> fieldsToCompareLeft = new HashMap<String, Field<?>>();
			for (String fieldName : exchangeGroup) {
				fieldsToCompareRight.put(fieldName, right.getFields().get(fieldName));
				fieldsToCompareLeft.put(fieldName, left.getFields().get(fieldName));
			}
			Iterator<Map.Entry<String, Field<?>>> itRight = fieldsToCompareRight.entrySet().iterator();
			Iterator<Map.Entry<String, Field<?>>> itLeft = fieldsToCompareLeft.entrySet().iterator();

			// process fields (left and right) until end is reached on one side
			while (itRight.hasNext() && itLeft.hasNext()) {
				// Search for next empty field on right side
				if (isEmptyOrNull(itRight.next().getValue())) {
					// Now go to next empty field on the left
					while (itLeft.hasNext()) {
						if (isEmptyOrNull(itLeft.next().getValue())) {
							// If empty fields have been found on each side,
							// remove them and continue with search on right side
							itRight.remove();
							itLeft.remove();
							break;
						}
					}
				}
			}

			List<List<String>> permutations = permutations(new LinkedList<String>(fieldsToCompareRight.keySet()));

			double bestPermWeight = Double.NEGATIVE_INFINITY;
			double bestPermWeightSum = 0.0;
			double bestPermWeightRatio = 0.0;
			for (List<String> permutation : permutations)
			{
				double thisPermWeight = 0.0;
				double thisPermWeightSum = 0;
				Iterator<String> fieldIterator = fieldsToCompareLeft.keySet().iterator();
				for (String fieldNamePerm : permutation)
				{
					String fieldName = fieldIterator.next();
					// Do not consider empty fields
					if (isEmptyOrNull(fieldsToCompareLeft.get(fieldName))
							|| isEmptyOrNull(fieldsToCompareRight.get(fieldNamePerm)))
						continue;

					// account mean value of field weights
					double meanFieldWeight = 0.5 * (weights.get(fieldName) + weights.get(fieldNamePerm));
					thisPermWeight += comparators.get(fieldName).compare(fieldsToCompareLeft.get(fieldName),
							fieldsToCompareRight.get(fieldNamePerm)) * meanFieldWeight;
					thisPermWeightSum += meanFieldWeight;
				}
				double thisPermWeightRatio = thisPermWeight / thisPermWeightSum;
				if (thisPermWeightRatio >= bestPermWeightRatio) {
					bestPermWeight = thisPermWeight;
					bestPermWeightSum = thisPermWeightSum;
					bestPermWeightRatio = thisPermWeightRatio;
				}
			}
			totalWeight += bestPermWeight;
			weightSum += bestPermWeightSum;
		}

		for (String fieldName : nonExchangeFields)
		{
			// Ignore empty fields
			if (isEmptyOrNull(left.getFields().get(fieldName)) || isEmptyOrNull(right.getFields().get(fieldName)))
				continue;

			double fieldWeight = weights.get(fieldName);
			weightSum += fieldWeight;
			double thisCompWeight = comparators.get(fieldName).compare(left, right) * fieldWeight;
			logger.debug("Weighted comparison for field " + fieldName + ": " + thisCompWeight);
			totalWeight += thisCompWeight;
		}
		totalWeight /= weightSum;
		return totalWeight;
	}

	/**
	 * Backend function for calculating permutations. Called recursively.
	 *
	 * @param result
	 *            Result list to which permutations are added.
	 * @param prefix
	 *            Prefix to add to the resulting permutations (fixed part) for
	 *            this run.
	 * @param elements
	 *            Elements that are permuted.
	 */
	private static void permutationWorker(List<List<String>> result, List<String> prefix, List<String> elements) {
		LinkedList<String> workingCopy;
		if (elements.size() == 1)
		{
			List<String> thisPerm = new LinkedList<String>(prefix);
			thisPerm.add(elements.get(0));
			result.add(thisPerm);
		} else {
			for (String elem : elements)
			{
				List<String> prefixClone = new LinkedList<String>(prefix);
				prefixClone.add(elem);
				workingCopy = new LinkedList<String>();
				for (String copyElem : elements)
					if (copyElem!=elem) workingCopy.add(copyElem);
				permutationWorker(result, prefixClone, workingCopy);
			}
		}
	}

	/**
	 * Get all permutations of a list of Strings.
	 *
	 * @param elements
	 *            A list of strings.
	 * @return The permutations.
	 */
	private static List<List<String>> permutations(List<String> elements)
	{
		LinkedList<List<String>> result = new LinkedList<List<String>>();
		if(elements.size() == 0) return result;
		permutationWorker(result, new LinkedList<String>(), elements);
		return result;
	}

	private boolean isEmptyOrNull(Field<?> f) {
		return (f== null || f.isEmpty());
	}
}