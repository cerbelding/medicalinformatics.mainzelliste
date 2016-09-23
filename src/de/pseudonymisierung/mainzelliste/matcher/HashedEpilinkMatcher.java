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

import java.util.Properties;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.matcher.hasher.Hasher;

/**
 * Represents a Matcher for record linkage. The calculation is based on the
 * EpilinkMatcher. Whether two patients are compared by their attributes,
 * depends on the comparison of their hashes.
 *
 * For using this Matcher, a threshold must be specified in the configuration
 * file. If no Hasher is specified, the behaviour is like the EpilinkMatcher.
 */
public class HashedEpilinkMatcher extends EpilinkMatcher {

    /**
     * Specified Threshold
     */
    private int thresholdHashes;

    /**
     * Hasher which is used for a comparison of two hashes
     */
    private Hasher comparatorHash;

    /**
     * Returns the specified threshold.
     *
     * @return Specified threshold
     */
    public int getThresholdHash() {
        return thresholdHashes;
    }

    /**
     * Returns the instance of the used Hasher. If no Hasher is specified, NULL
     * is returned.
     *
     * @return Instance of the Hasher or NULL, if no Hasher is specified.
     */
    public Hasher getHasher() {
        return comparatorHash;
    }

    @Override
    public void initialize(Properties props) throws InternalErrorException {
        super.initialize(props);

        String thresholdHashesProperty = props.getProperty("hash.threshold");
        thresholdHashes = Integer.parseInt((thresholdHashesProperty != null) ? thresholdHashesProperty : "-1000");

        String hashUseProperty = props.getProperty("hasher.use");
        if (hashUseProperty != null && hashUseProperty.equals("true")) {
            try {
                Class<?> hasherClass = Class.forName("de.pseudonymisierung.mainzelliste.matcher.hasher." + props.getProperty("hash.hasher"));
                comparatorHash = (Hasher) hasherClass.newInstance();
            } catch (Exception e) {
                throw new InternalErrorException();
            }
        }
    }

    @Override
    public MatchResult match(Patient a, Iterable<Patient> patientList) {
        if (comparatorHash == null) {
            return super.match(a, patientList);
        }

        Patient bestMatch = null;
        double bestWeight = Double.NEGATIVE_INFINITY;

        for (Patient b : patientList) {
            int comparedHashes = comparatorHash.compare(a.getHash(), b.getHash());

            if (comparedHashes >= thresholdHashes) {
                // assert that the persons have the same Fields 
                assert (a.getFields().keySet().equals(b.getFields().keySet()));
                double weight = calculateWeight(a, b);

                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestMatch = b;
                }
            }
        }

        if (bestWeight >= getThresholdMatch()) {
            return new MatchResult(MatchResultType.MATCH, bestMatch, bestWeight);
        } else if (bestWeight < getThresholdMatch() && bestWeight > getThresholdNonMatch()) {
            return new MatchResult(MatchResultType.POSSIBLE_MATCH, bestMatch, bestWeight);
        } else {
            return new MatchResult(MatchResultType.NON_MATCH, null, bestWeight);
        }
    }
}
