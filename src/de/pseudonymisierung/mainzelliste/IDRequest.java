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

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.webservice.AddPatientToken;

/**
 * Represents a request to add a patient, consisting of information like input
 * fields, match result, type of requested ID.
 */
@Entity
@Table(name = "IDRequest")
public class IDRequest {

	/** Database id. */
	@Id
	@GeneratedValue
	@JsonIgnore
	private int idRequestJpaId;

	/** Map of fields as provided by the input form. */
	@OneToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	private Map<String, Field<?>> inputFields;

	/** Type of the requested ID */
	@ElementCollection
	private Set<String> requestedIdTypes;

	/** The match result, including the matched patient */
	@Embedded
	private MatchResult matchResult;

	/** The token that was used to make the request */
	@Transient
	private AddPatientToken token;
	
	/**
	 * The patient object that was actually assigned. In case of a match this is
	 * usually equal to matchResult.bestMatchedPatient.
	 */
	@ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	private Patient assignedPatient;

	/** Date and time of this request. */
	private Date timestamp;

	/**
	 * Creates a new IDRequest instance.
	 *
	 * @param inputFields
	 *            The input fields as provided by the input form with field
	 *            values as keys and respective inputs as values.
	 * @param idTypes
	 *            The requested ID types.
	 * @param matchResult
	 *            The match result as returned by the matcher.
	 * @param assignedPatient
	 *            The assigned patient object, i.e. the patient whose IDs are
	 *            returned. This is the newly created patient if no matching
	 *            patient was found.
	 * @param token
	 *            The token that was used to make this request.
	 */
	public IDRequest(Map<String, Field<?>> inputFields, Set<String> idTypes,
			MatchResult matchResult, Patient assignedPatient, AddPatientToken token) {
		super();
		this.inputFields = inputFields;
		this.requestedIdTypes = idTypes;
		this.matchResult = matchResult;
		this.assignedPatient = assignedPatient;
		this.timestamp = new Date();
		this.token = token;
	}

	/**
	 * Get the assigned patient, i.e. he patient whose IDs are returned.
	 *
	 * @return The assigned patient.
	 */
	public Patient getAssignedPatient() {
		return assignedPatient;
	}

	/**
	 * Get the input fields as provided by the input form.
	 *
	 * @return Map with field values as keys and respective inputs as values.
	 */
	public Map<String, Field<?>> getInputFields() {
		return inputFields;
	}

	/**
	 * Get the requested ID types.
	 *
	 * @return The requested ID types.
	 */
	public Collection<String> getRequestedIdTypes() {
		return requestedIdTypes;
	}

	/**
	 * Get the match result for this request as returned by the matcher.
	 *
	 * @return The assigned match result.
	 */
	public MatchResult getMatchResult() {
		return matchResult;
	}

	/**
	 * Get the requested IDs, i.e. the IDs of the assigned patient.
	 *
	 * @return The requested IDs.
	 * @see IDRequest#getAssignedPatient()
	 */
	public Set<ID> getRequestedIds() {

		if (this.assignedPatient == null)
			return null;

		LinkedList<ID> idList = new LinkedList<ID>();

		for (String thisType : this.requestedIdTypes) {
			idList.add(this.assignedPatient.getOriginal().getId(thisType));
		}
		return new CopyOnWriteArraySet<ID>(idList);
	}

	/**
	 * Gets the date and time of this request.
	 *
	 * @return The date and time of this request.
	 */
	Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Get the token that was used to make this request.
	 * 
	 * @return The token that was used to make this request.
	 */
	public AddPatientToken getToken() {
		return this.token;
	}
}
