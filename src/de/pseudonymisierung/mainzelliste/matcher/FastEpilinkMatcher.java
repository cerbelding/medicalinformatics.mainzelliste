package de.pseudonymisierung.mainzelliste.matcher;

import java.util.List;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;
import java.util.AbstractMap;
import java.util.ArrayList;

/**
 * Represent a Matcher that is used for record linkage. The calculation of
 * the exchange groups are optimized.
 */
public class FastEpilinkMatcher extends EpilinkMatcher
{
    private class SimilarityRelation
    {
        public String fieldnameA;
        public Field<?> fieldvalueA;
        public String fieldnameB;
        public Field<?> fieldvalueB;
        public double similarity;

        public SimilarityRelation(String fieldnameA, Field<?> fieldvalueA, String fieldnameB, Field<?> fieldvalueB, double simiarity)
        {
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
    public double calculateWeight(Patient left, Patient right)
    {
        double weightSum = 0;
        double totalWeight = 0;

        for (List<String> exchangeGroup : getExchangeGroups())
        {
            int exchangeGroupSize = exchangeGroup.size();

            List<AbstractMap.SimpleEntry> unassignedPatientAFields = new ArrayList<AbstractMap.SimpleEntry>();
            for (int i = 0; i < exchangeGroupSize; ++i)
            {
                String fieldname = exchangeGroup.get(i);
                unassignedPatientAFields.add(new AbstractMap.SimpleEntry(fieldname, right.getFields().get(fieldname)));
            }

            List<SimilarityRelation> assignedPatientFields = new ArrayList<SimilarityRelation>();

            for (int i = 0; i < exchangeGroupSize; ++i)
            {
                String fieldname = exchangeGroup.get(i);

                double bestComp = 0.0;
                AbstractMap.SimpleEntry ref = unassignedPatientAFields.get(0);
                int bestField = 0;

                for (int j = 0; j < unassignedPatientAFields.size(); ++j)
                {
                    double tmpComp = getComparators().get(exchangeGroup.get(i)).compare(left.getFields().get(fieldname), right.getFields().get((String) unassignedPatientAFields.get(j).getKey()));

                    if (tmpComp > bestComp)
                    {
                        bestComp = tmpComp;
                        ref = unassignedPatientAFields.get(j);
                        bestField = j;
                    }
                }

                assignedPatientFields.add(new SimilarityRelation(fieldname, left.getFields().get(fieldname), (String) ref.getKey(), (Field<?>) ref.getValue(), bestComp));
                unassignedPatientAFields.remove(bestField);
            }

            for (int i = 0; i < assignedPatientFields.size(); ++i)
            {
                SimilarityRelation ref = assignedPatientFields.get(i);

                String patAFieldName = ref.fieldnameA;
                String patBFieldName = ref.fieldnameB;

                Field<?> fieldA = ref.fieldvalueA;
                Field<?> fieldB = ref.fieldvalueB;

                if (fieldA.isEmpty() || fieldB.isEmpty())
                    continue;

                double meanFieldWeight = 0.5 * (getWeights().get(patAFieldName) + getWeights().get(patBFieldName));
                totalWeight += ref.similarity * meanFieldWeight;
                                
                weightSum += meanFieldWeight;
            }
        }

        // Uebernommen aus EpilinkMatcher
        for (String fieldName : getNonExchangeFields())
        {
            // Ignore empty fields
            if (left.getFields().get(fieldName).isEmpty() || right.getFields().get(fieldName).isEmpty())
            {
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
