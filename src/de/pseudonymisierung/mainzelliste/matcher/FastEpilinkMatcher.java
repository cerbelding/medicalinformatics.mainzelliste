/*
 * Copyright (C) 2013-2016 Martin Lablans, Andreas Borg, Frank Ückert and contributors
 * (see Git commit history for individual contributions)
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
import java.util.Map;

import blogspot.software_and_algorithms.stern_library.optimization.HungarianAlgorithm;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;

/**
 * Represents a Matcher that is used for record linkage. The calculation of the
 * exchange groups are optimized.
 */
public class FastEpilinkMatcher extends EpilinkMatcher {

    /**
     * Returns the similarity of two given patients in interval [0,1]. The used
     * exchange groups are optimized.
     *
     * @param left First patient
     * @param right Second patient
     *
     * @return Similarity of two patients in Interval [0,1]
     */
    @Override
    public double calculateWeight(Patient left, Patient right) {
        double weightSum = 0;
        double totalWeight = 0;

        Map<String, Field<?>> leftFields = left.getFields();
        Map<String, Field<?>> rightFields = right.getFields();
        
        for (List<String> exchangeGroup : getExchangeGroups()) {
            int exchangeGroupSize = exchangeGroup.size();

			/*
			 * Find optimal assignment of fields within exchange group by using
			 * the Hungarian algorithm. The cost of assigning field A to field B
			 * is 0 minus the result of comparing field A of the left hand
			 * patient with field B of the right hand patient.
			 */
            // The cost matrix
        	double costMatrix [][] = new double[exchangeGroupSize][exchangeGroupSize];
        	/* The comparison weight for any combination of fields. */
        	double permWeights [][] = new double[exchangeGroupSize][exchangeGroupSize];
        	for (int leftIndex = 0; leftIndex < exchangeGroupSize; leftIndex++) {
        		for (int rightIndex = leftIndex; rightIndex < exchangeGroupSize; rightIndex++) {
        			if (leftIndex == rightIndex)
        				// Assigning a field to itself -> keep the field weight
        				permWeights[leftIndex][rightIndex] = getWeights().get(exchangeGroup.get(leftIndex));
        			else {
        				// Assigning a field to another: Use mean value of individual weights
        				permWeights[leftIndex][rightIndex] = 0.5 * (getWeights().get(exchangeGroup.get(leftIndex))
        						+ getWeights().get(exchangeGroup.get(rightIndex)));
        				permWeights[rightIndex][leftIndex] = permWeights[leftIndex][rightIndex];
        			}
        		}
        	}
        	
        	// Build cost matrix
        	for (int leftIndex = 0; leftIndex < exchangeGroupSize; leftIndex++) {
        		for (int rightIndex = 0; rightIndex < exchangeGroupSize; rightIndex++) {
        			String leftFieldName = exchangeGroup.get(leftIndex);
        			String rightFieldName = exchangeGroup.get(rightIndex);
        			Field<?> leftField = leftFields.get(leftFieldName);
        			Field<?> rightField = rightFields.get(rightFieldName);
        			
        			double thisAssignmentCost;
        			// Prefer matching fields that are both empty by assigning maximum comparison weight 
        			if (leftField.isEmpty() && rightField.isEmpty())
        				thisAssignmentCost = permWeights[leftIndex][rightIndex];
        			// Discourage assignments where only one field is empty by assigning zero 
        			else if (leftField.isEmpty() || rightField.isEmpty())
        				thisAssignmentCost = 0;
        			else {        				
        				// Otherwise, assign the weighted comparison
        				thisAssignmentCost = getComparators().get(leftFieldName)
        						.compare(leftFields.get(leftFieldName), rightFields.get(rightFieldName)) * permWeights[leftIndex][rightIndex];
        			}
        			// use negative comparison value because Hungarian Algorithm minimizes
        			costMatrix[leftIndex][rightIndex] = -thisAssignmentCost;
        		}
        	}
        	
        	// Execute Hungarian algorithm
        	HungarianAlgorithm hungAlg = new HungarianAlgorithm(costMatrix);
        	int assignment[] = hungAlg.execute();
        	
        	for (int leftIndex = 0; leftIndex < exchangeGroupSize; leftIndex++) {
        		// Do not count fields with empty values
        		if (leftFields.get(exchangeGroup.get(leftIndex)).isEmpty() || rightFields.get(exchangeGroup.get(assignment[leftIndex])).isEmpty())
        			continue;
        		double thisAssignmentWeight = permWeights[leftIndex][assignment[leftIndex]];
        		double thisAssignmentComp = -costMatrix[leftIndex][assignment[leftIndex]];
        		
        		totalWeight += thisAssignmentComp;
        		weightSum += thisAssignmentWeight;        
        	}
        }

        // Adopted from EpilinkMatcher
        for (String fieldName : getNonExchangeFields()) {
            // Ignore empty fields
            if (left.getFields().get(fieldName).isEmpty() || right.getFields().get(fieldName).isEmpty()) {
                continue;
            }

            double fieldWeight = getWeights().get(fieldName);
            weightSum += fieldWeight;
            double thisCompWeight = getComparators().get(fieldName).compare(left, right) * fieldWeight;
            totalWeight += thisCompWeight;
        }
        totalWeight /= weightSum;
        return totalWeight;
    }
}
