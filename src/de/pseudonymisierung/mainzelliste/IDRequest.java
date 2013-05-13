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

import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.pseudonymisierung.mainzelliste.matcher.MatchResult;

/** 
 * Data structure for an ID request: Input fields, match result, type of ID.
 */
@Entity
@Table(name="IDRequest")
public class IDRequest {
	@Id
	@GeneratedValue
	@JsonIgnore
	private int idRequestJpaId;
	
	/** Map of fields as provided by the input form. */
	@OneToMany(cascade={CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.EAGER)
	private Map<String, Field<?>> inputFields;
	
	/** Type of the requested ID */
	@Basic
	private String requestedIdType;
	
	/** The match result, including the matched patient */
	@Embedded
	private MatchResult matchResult;
	
	/** The patient object that was actually assigned. In case of a match 
	 * this is usually equal to matchResult.patient.
	 */
	@ManyToOne(cascade={CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.EAGER)
	private Patient assignedPatient;

	public IDRequest(Map<String, Field<?>> inputFields, String idType,
			MatchResult matchResult, Patient assignedPatient) {
		super();
		this.inputFields = inputFields;
		this.requestedIdType = idType;
		this.matchResult = matchResult;
		this.assignedPatient = assignedPatient;
	}

	public Patient getAssignedPatient() {
		return assignedPatient;
	}

	public Map<String, Field<?>> getInputFields() {
		return inputFields;
	}

	public String getRequestedIdType() {
		return requestedIdType;
	}

	public MatchResult getMatchResult() {
		return matchResult;
	}
}
