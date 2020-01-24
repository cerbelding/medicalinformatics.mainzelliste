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
package de.pseudonymisierung.mainzelliste.webservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidFieldException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;

/**
 * Authorizes to edit a patient's IDAT.
 */
public class EditPatientToken extends Token {

	/**
	 * ID of the patient that can be edited with this token.
	 */
	private ID patientId;

	/**
	 * Names of fields that can be changed with this token. If null,
	 * all fields can be changed.
	 */
	private Set<String> fields;

    /**
     * External IDs that can be changed with this token. If null,
     * all external IDs can be changed.
     */
    private Set<String> ids;

    /**
	 * URL to redirect to after using the token.
	 */
	private URL redirect = null;

	/**
	 * Get ID of the patient that can be edited with this token. The patient is
	 * identified by the ID that was provided on token creation.
	 *
	 * @return The patient ID.
	 */
	public ID getPatientId() {
		return patientId;
	}

	/**
	 * Get the fields that can be changed with this token. If null, all fields
	 * can be changed.
	 *
	 * @return Set of field names.
	 */
	public Set<String> getFields() {
		return fields;
	}

    /**
     * Get the external IDs that can be changed with this token. If null, all external IDs
     * can be changed.
     *
     * @return Set of id names.
     */
    public Set<String> getIds() {
        return ids;
    }

	/**
	 * Return the URL to which the user should be redirected after the token has
	 * been used.
	 *
	 * @return The redirect URL or null if none is set.
	 */
	public URL getRedirect() {
		return this.redirect;
	}

	@Override
	public void setData(Map<String, ?> data) {
		super.setData(data);

		// Read patient id from token and check if valid
		Map<String, ?> idJSON = this.getDataItemMap("patientId");
		if (!idJSON.containsKey("idString") || !idJSON.containsKey("idType"))
			throw new InvalidIDException("Please provide a valid patient id as data item 'patientId'!");

		this.patientId = IDGeneratorFactory.instance.buildId(
				idJSON.get("idType").toString(), idJSON.get("idString").toString());

		if (!Persistor.instance.patientExists(patientId))
			throw new InvalidIDException("No patient exists with id " + patientId.toString());

		// Read redirect URL
		String redirectURLString = this.getDataItemString("redirect");
		if (redirectURLString != null) {
			try {
				this.redirect = new URL(redirectURLString);
			} catch (MalformedURLException e) {
				throw new InvalidTokenException("Redirect URL " + redirectURLString + " is not a valid URL.");
			}
		}

		// Read field list (if present) from data and check if valid
		List<?> fieldsJSON = this.getDataItemList("fields");
		if (fieldsJSON != null) {
			this.fields = new HashSet<String>();
			for (Object thisField : fieldsJSON) {
				String fieldName = thisField.toString();
				if (!Config.instance.fieldExists(fieldName))
					throw new InvalidFieldException("No field '" + fieldName + "' defined!");
				this.fields.add(fieldName);
			}
		}

		// Read ID list (external IDs), if present, and check if valid
        List<?> idsJSON = this.getDataItemList("ids");
        if (idsJSON != null) {
            this.ids = new HashSet<String>();
            Set<String> validExternalIds = IDGeneratorFactory.instance.getExternalIdTypes();
            for (Object thisId : idsJSON) {
                String idType = thisId.toString();
                if (!validExternalIds.contains(idType))
                    throw new InvalidIDException("Invalid external Id type '" + idType + "' in token.");
                this.ids.add(idType);
            }
        }
	}

}
