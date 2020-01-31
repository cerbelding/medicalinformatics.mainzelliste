package de.pseudonymisierung.mainzelliste.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Signals that a token is invalid.
 */
public class InvalidTokenException extends WebApplicationException {

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -9199090030935788168L;

	/** The default error message. */
	private String message;

	/**
	 * Create an instance with the given error message.
	 *
	 * @param message
	 *            The error message.
	 */
	public InvalidTokenException(String message) {
		super(Response.status(Status.BAD_REQUEST).entity(message).build());
		this.message = message;
	}

	/**
	 * Create an instance with the given error message and HTTP status code.
	 *
	 * @param message
	 *            The error message.
	 * @param statusCode
	 *            The HTTP status code to return.
	 */
	public InvalidTokenException(String message, Status statusCode) {
		super(Response.status(statusCode).entity(message).build());
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
