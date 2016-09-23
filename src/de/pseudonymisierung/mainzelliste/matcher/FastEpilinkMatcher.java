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

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;
import java.util.AbstractMap;
import java.util.ArrayList;

/**
 * Represents a Matcher that is used for record linkage. The calculation of the
 * exchange groups are optimized.
 */
public class FastEpilinkMatcher extends EpilinkMatcher {

    private class SimilarityRelation {

        public String fieldnameA;
        public Field<?> fieldvalueA;
        public String fieldnameB;
        public Field<?> fieldvalueB;
        public double similarity;

        public SimilarityRelation(String fieldnameA, Field<?> fieldvalueA, String fieldnameB, Field<?> fieldvalueB, double simiarity) {
            this.fieldnameA = fieldnameA;
            this.fieldvalueA = fieldvalueA;
            this.fieldnameB = fieldnameB;
            this.fieldvalueB = fieldvalueB;
            this.similarity = simiarity;
        }
    }

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

        for (List<String> exchangeGroup : getExchangeGroups()) {
            int exchangeGroupSize = exchangeGroup.size();

            List<AbstractMap.SimpleEntry> unassignedPatientAFields = new ArrayList<AbstractMap.SimpleEntry>();
            for (int i = 0; i < exchangeGroupSize; ++i) {
                String fieldname = exchangeGroup.get(i);
                unassignedPatientAFields.add(new AbstractMap.SimpleEntry(fieldname, right.getFields().get(fieldname)));
            }

            List<SimilarityRelation> assignedPatientFields = new ArrayList<SimilarityRelation>();

            for (int i = 0; i < exchangeGroupSize; ++i) {
                String fieldname = exchangeGroup.get(i);

                double bestComp = 0.0;
                AbstractMap.SimpleEntry ref = unassignedPatientAFields.get(0);
                int bestField = 0;

                for (int j = 0; j < unassignedPatientAFields.size(); ++j) {
                    double tmpComp = getComparators().get(exchangeGroup.get(i)).compare(left.getFields().get(fieldname), right.getFields().get((String) unassignedPatientAFields.get(j).getKey()));

                    if (tmpComp > bestComp) {
                        bestComp = tmpComp;
                        ref = unassignedPatientAFields.get(j);
                        bestField = j;
                    }
                }

                assignedPatientFields.add(new SimilarityRelation(fieldname, left.getFields().get(fieldname), (String) ref.getKey(), (Field<?>) ref.getValue(), bestComp));
                unassignedPatientAFields.remove(bestField);
            }

            for (int i = 0; i < assignedPatientFields.size(); ++i) {
                SimilarityRelation ref = assignedPatientFields.get(i);

                String patAFieldName = ref.fieldnameA;
                String patBFieldName = ref.fieldnameB;

                Field<?> fieldA = ref.fieldvalueA;
                Field<?> fieldB = ref.fieldvalueB;

                if (fieldA.isEmpty() || fieldB.isEmpty()) {
                    continue;
                }

                double meanFieldWeight = 0.5 * (getWeights().get(patAFieldName) + getWeights().get(patBFieldName));
                totalWeight += ref.similarity * meanFieldWeight;

                weightSum += meanFieldWeight;
            }
        }

        // Uebernommen aus EpilinkMatcher
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
