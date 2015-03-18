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

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import de.pseudonymisierung.mainzelliste.Patient;

@Embeddable
public class MatchResult {
	
	public enum MatchResultType {
		MATCH, NON_MATCH, POSSIBLE_MATCH, AMBIGOUS;
	};
	
	@Basic
	private MatchResultType type;
	
	private double bestMatchedWeight; 

	/**
	 * @return the bestMatchedWeight
	 */
	public double getBestMatchedWeight() {
		return bestMatchedWeight;
	}

	@ManyToOne
	private Patient bestMatchedPatient;
	
	public Patient getBestMatchedPatient()
	{
		return this.bestMatchedPatient;
	}
	
	/**
	 * Get the match result type of this result.
	 * 
	 * @return <ul>
	 * 	<li>MATCH: A sure match is found. getPatient() retreives the 
	 * 	matching bestMatchedPatient.
	 * 	<li>POSSIBLE_MATCH: An unsure match is found. getPatient() 
	 * retreives the best matching bestMatchedPatient.
	 * 	<li> NON_MATCH: No matching bestMatchedPatient was found. getPatient
	 * returns null.
	 * </ul>
	 * 
	 */
	public MatchResultType getResultType()
	{
		return this.type;
	}

	public MatchResult(MatchResultType type, Patient patient, double bestMatchedWeight)
	{
		this.type = type;
		this.bestMatchedPatient = patient;
		if (Double.isInfinite(bestMatchedWeight) || Double.isNaN(bestMatchedWeight))
			this.bestMatchedWeight = -Double.MAX_VALUE;
		else
			this.bestMatchedWeight = bestMatchedWeight;
	}	
}
