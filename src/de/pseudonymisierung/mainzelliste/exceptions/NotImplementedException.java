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
package de.pseudonymisierung.mainzelliste.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Signals that a function is not yet implemented. HTTP status code 503 (Service
 * Unavailable) will be returned to the client if the exception is not caught.
 */
public class NotImplementedException extends WebApplicationException {

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = 4679822464201106826L;

	/** The default error message. */
	private static final String defaultMessage = "Functionality not implemented yet.";

	/** Create an instance with the default error message. */
	public NotImplementedException() {
		super(Response.status(Status.SERVICE_UNAVAILABLE).entity(defaultMessage).build());
	}

	/** Create an instance with the default message, detailed in an detailMessage. */
	public NotImplementedException(String detailMessage) {
        super(Response.status(Status.SERVICE_UNAVAILABLE).entity(defaultMessage + ": " + detailMessage).build());
    }
}
