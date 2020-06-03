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
package de.pseudonymisierung.mainzelliste.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;

/**
 * Provides a default way to report JSON errors
 *
 */
public class InvalidJSONException extends WebApplicationException {

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -4979455015089015791L;

	/** The default error message. */
	private static String defaultMessage = "A JSON error occured.";

	/** Create an instance with the default error message. */
	public InvalidJSONException() {
		this(defaultMessage);
	}

	/**
	 * Create an instance with the given exception as cause.
	 *
	 * @param e
	 *            The JSONException that is the cause of this exception.
	 * */
	public InvalidJSONException(JSONException e) {
		this(defaultMessage + " " + e.getMessage());
	}

	/**
	 * Create an instance with the given error message.
	 *
	 * @param message
	 *            The error message.
	 */
	public InvalidJSONException(String message) {
		super(Response.status(Status.BAD_REQUEST).entity(message).build());
	}

	@Override
	public String getMessage() {
		return defaultMessage;
	}
}
