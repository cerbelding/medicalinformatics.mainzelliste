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
 * Signals an internal application error. Will be reported to the requester as
 * an HTTP 500 error, if not caught.
 */
public class InternalErrorException extends WebApplicationException {

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -3828755806557209546L;

	/** The default error message. */
	private static String message = "Internal server error.";

	/** Create an instance with default error message. */
	public InternalErrorException() {
		super(Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
	}

	/**
	 * Create an instance with the given error message.
	 *
	 * @param message
	 *            The error message.
	 */
	public InternalErrorException(String message) {
		super(Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
	}

	/**
	 * Create an instance with the given cause.
	 *
	 * @param cause
	 *            The underlying cause.
	 */
	public InternalErrorException(Throwable cause) {
		super(cause, Status.INTERNAL_SERVER_ERROR);
	}

	@Override
	public String getMessage() {
		return message;
	}
}
