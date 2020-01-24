package de.pseudonymisierung.mainzelliste.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class IllegalUsedCharacterException extends WebApplicationException {

    @SuppressWarnings("javadoc")
    private static final long serialVersionUID = -3243941736508044418L;

    /** The default error message. */
    private static String defaultMessage = "Illegal character.";


    public IllegalUsedCharacterException(){
        super(Response.status(Response.Status.BAD_REQUEST).entity(defaultMessage).build());
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
