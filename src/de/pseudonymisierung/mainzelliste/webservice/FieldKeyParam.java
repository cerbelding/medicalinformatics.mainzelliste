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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.pseudonymisierung.mainzelliste.Config;

/**
 * Realization of {@link AbstractParam} for field names.
 */
public class FieldKeyParam extends AbstractParam<String> {

	/**
	 * Create an instance from the given field name.
	 *
	 * @param s
	 *            Name of a valid (i.e. configured) field.
	 */
	public FieldKeyParam(String s) {
		super(s);
	}

	@Override
	protected String parse(String s) throws WebApplicationException {
		if(!Config.instance.getFieldKeys().contains(s)){
			throw new WebApplicationException(Response
				.status(Status.BAD_REQUEST)
				.entity("There is no Field key called " + s + ".")
				.build()
			);
		}
		return s;
	}
}
