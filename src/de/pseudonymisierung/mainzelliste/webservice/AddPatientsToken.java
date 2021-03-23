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
package de.pseudonymisierung.mainzelliste.webservice;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Servers.ApiVersion;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authorizes to add a patient to the database by his IDAT and receive an ID
 * (pseudonym) back.
 */
public class AddPatientsToken extends Token {
	/** The ID types that should be returned when making the ID request. */
	private final Set<String> requestedIdTypes;
	private boolean showPossibleMatches = false;

	public AddPatientsToken() {
		super("addPatients", 1);
		this.requestedIdTypes = new HashSet<>();
	}

	@Override
	public void setData(Map<String, ?> data) {
		super.setData(data);
		showPossibleMatches = Boolean.TRUE.equals(this.getData().get("showPossibleMatches"));
		this.getDataItemList("idTypes").forEach( o -> requestedIdTypes.add(o.toString()));
	}

	@Override
	public void checkValidity(ApiVersion apiVersion){
		if (this.hasDataItem("idTypes")) {
			this.checkIdTypes((List<String>) this.getDataItemList("idTypes"));
		}
		if (Config.instance.auditTrailIsOn()) {
			checkAuditTrail();
		}
	}

	/**
	 * Get the ID types that should be returned when making the ID request.
	 *
	 * @return The set of requested ID types.
	 */
	public Set<String> getRequestedIdTypes() {
		return this.requestedIdTypes;
	}
	
	/**
	 * Query whether this token permits to return possible matches for an unsure
	 * record linkage result. I.e., when POST /patients is performed with this
	 * token and the record linkage returns
	 * {@link MatchResultType#POSSIBLE_MATCH}, the IDs of patients that are
	 * similar to the requested patient are returned in the response.
	 * 
	 * @return True if possible matches are returned.
	 */
	public boolean showPossibleMatches() {
		return showPossibleMatches;
	}
}
