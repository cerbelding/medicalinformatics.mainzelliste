package de.pseudonymisierung.mainzelliste.exceptions;

import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class NoParentServerNameException extends WebApplicationException {

	private Logger logger = Logger.getLogger(NoParentServerNameException.class);

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -9199192230935788168L;

	public NoParentServerNameException() {
		logger.info("token.getParentServerName() == null ");
		logger.info("Cookies are necessary to use this function with this Mainzelliste version. Check if JSESSIONID is used to create token and no proxy clears cookies.");


		Response.status(Status.INTERNAL_SERVER_ERROR).entity("This server is misconfigured or you have to activate cookies. You can find more information in the server log.").build();

	}

}
