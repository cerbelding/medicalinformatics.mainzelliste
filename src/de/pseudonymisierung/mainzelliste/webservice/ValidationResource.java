package de.pseudonymisierung.mainzelliste.webservice;

import de.pseudonymisierung.mainzelliste.Servers;
import org.apache.http.HttpStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/validate")
public class ValidationResource {

    private static Logger logger = LogManager.getLogger(ValidationResource.class);

    @GET
    @Path("/token")
    public Response validateToken(@QueryParam("tokenId") String tokenId){
        logger.info("@GET on validateToken endpoint");
        if(tokenId == null || tokenId.equals("")){
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Token Id is empty. Can't check Token for empty tokenId !!!").build();
        }
        Token token = Servers.instance.getTokenByTid(tokenId);
        if(token == null){
            return Response.status(HttpStatus.SC_NOT_FOUND).entity("Couldn't find a valid token with the requested tokenId").build();
        }
        return Response.ok().build();
    }

}
