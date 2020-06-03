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
import java.util.NavigableMap;

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import de.pseudonymisierung.mainzelliste.Patient;

/**
 * Represents the result of matching a (usually new) patient against a list of
 * existing ones. The result contains the matching patient, if there is one.
 */
@Embeddable
public class MatchResult {

	/**
	 * Encodes record linkage results.
	 */
	public enum MatchResultType {
		/**
		 * A definitive match, i.e. the queried record matches another without a
		 * doubt.
		 */
		MATCH,
		/**
		 * A definitve non-match, i.e. the queried record matches no other
		 * without a doubt.
		 */
		NON_MATCH,
		/**
		 * A possible match, i.e. there is at least one record that might be a
		 * match, but this cannot be determined for sure.
		 */
		POSSIBLE_MATCH,
		/**
		 * Ambigous case, used to signal conflicts (several records are matching
		 * when only one is expected).
		 */
		AMBIGOUS;
	};

	/** The type of this match result. */
	@Basic
	private MatchResultType type;

	/**
	 * The best (i.e. highest) matching weight found.
	 */
	private double bestMatchedWeight;

	/**
	 * Get the best (i.e. highest) matching weight that occured in the record
	 * linkage process. The corresponding patient can be retreived by
	 * {@link #getBestMatchedPatient()}.
	 *
	 * @return The best matching weight.
	 */
	public double getBestMatchedWeight() {
		return bestMatchedWeight;
	}

	/**
	 * The best matching patient (i.e. for which comparison produced the highest
	 * weight).
	 */
	@ManyToOne
	private Patient bestMatchedPatient;

	/**
	 * All possible matches, indexed by matching weight. Values are lists of
	 * patients because multiple comparisons can yield the same weight.
	 */
	@Transient
	private NavigableMap<Double, List<Patient>> possibleMatches;

	/**
	 * Get all possible matches, indexed and ordered descending by matching
	 * weight. Values are lists of patients because multiple comparisons can
	 * yield the same weight.
	 * 
	 * @return The map of possible matches.
	 */
	public NavigableMap<Double, List<Patient>> getPossibleMatches() {
		return possibleMatches;
	}

	/**
	 * Set possible matches. Stores a descending view of the given map.Values
	 * are lists of patients because multiple comparisons can yield the same
	 * weight.
	 * 
	 * @param possibleMatches
	 *            The map of possible matches, indexed by matching weight.
	 */
	public void setPossibleMatches(NavigableMap<Double, List<Patient>> possibleMatches) {
		this.possibleMatches = possibleMatches.descendingMap();
	}

	/**
	 * Get the best matching patient. This the one for which comparison to the
	 * input patient yielded the highes matching weight.
	 *
	 * @return The best matching patient.
	 */
	public Patient getBestMatchedPatient() {
		return this.bestMatchedPatient;
	}

	/**
	 * Get the match result type of this result.
	 *
	 * @return The match result, see {@link MatchResultType} for details.
	 */
	public MatchResultType getResultType() {
		return this.type;
	}

	/**
	 * Create an instance with the given values.
	 *
	 * @param type
	 *            The match result type.
	 * @param patient
	 *            The best matching patient.
	 * @param bestMatchedWeight
	 *            The best matching weight.
	 */
	public MatchResult(MatchResultType type, Patient patient,
			double bestMatchedWeight) {
		this.type = type;
		this.bestMatchedPatient = patient;
		if (Double.isInfinite(bestMatchedWeight)
				|| Double.isNaN(bestMatchedWeight))
			this.bestMatchedWeight = -Double.MAX_VALUE;
		else
			this.bestMatchedWeight = bestMatchedWeight;
	}
}
