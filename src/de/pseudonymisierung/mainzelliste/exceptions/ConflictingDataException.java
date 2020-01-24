package de.pseudonymisierung.mainzelliste.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Exception for the case of conflicting patient data (e.g. two distinct patients with equal ID).
 */
public class ConflictingDataException extends WebApplicationException {

    private static final long serialVersionUID = 68530323482725432L;
    /** The default error message. */
	private static String defaultMessage = "Conflicting data.";

	/**
	 * Create an instance with a default error message.
	 */
	public ConflictingDataException() {
		this(defaultMessage);
	}
	
	/**
	 * Create an instance with the given error message.
	 * @param message The error message.
	 */
	public ConflictingDataException(String message) {
		super(Response.status(Status.BAD_REQUEST).entity(message).build());
	}
}
