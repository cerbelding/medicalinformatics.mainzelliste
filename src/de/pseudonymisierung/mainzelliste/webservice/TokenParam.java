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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jettison.json.JSONObject;

/**
 * Realization of {@link AbstractParam} for getting tokens by their JSON representation.
 */
public class TokenParam extends AbstractParam<Token> {

	/**
	 * Create an instance from a JSON string.
	 *
	 * @param param
	 *            JSON representation of a token.
	 */
	public TokenParam(String param) {
		super(param);
	}

	@Override
	protected Token parse(String param) throws WebApplicationException {

		try {
			JSONObject jsob = new JSONObject(param);
			String tokenType = jsob.optString("type");
			Token t;
			if (tokenType.equals("addPatient"))
				t = new AddPatientToken();
			else if (tokenType.equals("editPatient"))
				t = new EditPatientToken();
			else
				t = new Token();
			if(!("".equals(jsob.optString("id"))))
				t.setId(jsob.getString("id"));
			t.setType(jsob.getString("type"));
			HashMap<String, Object> data = new ObjectMapper().readValue (jsob.getString("data"), new TypeReference<HashMap<String, Object>>() {});

			// compatibility fix: "idtypes" -> "idTypes"
			HashMap<String, Object> changedItems = new HashMap<String, Object>();
			for (Iterator<Entry<String, Object>> itDataItem = data.entrySet().iterator(); itDataItem.hasNext();) {
				Entry<String, Object> dataEntry = itDataItem.next();
				if (dataEntry.getKey().toLowerCase().equals("idtypes")) {
					itDataItem.remove();
					changedItems.put("idTypes", dataEntry.getValue());
				}
			}
			data.putAll(changedItems);
			t.setData(data);

			return t;
		} catch (WebApplicationException e) {
			throw (e);
		} catch (Exception e) {
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Invalid input: " + e.getMessage())
					.build()
				);
		}
	}
}
