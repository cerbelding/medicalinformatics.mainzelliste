package de.pseudonymisierung.mainzelliste.webservice.commons;

import com.sun.jersey.api.uri.UriTemplate;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

public class Redirect {

    private Logger logger = Logger.getLogger(Redirect.class);


    private Map<String, String> tokenId;
    private Map<String, String> mappedIdTypesdAndIds;
    private UriTemplate uriTemplate;
    private URI redirectURI;

    private Map<String, String> allMappedValues;

    public Redirect(Map<String, String> tokenId, Map<String, String> mappedIdTypesdAndIds, UriTemplate uriTemplate, URI redirectURI, Map<String, String> allMappedValues) {
        this.tokenId = tokenId;
        this.mappedIdTypesdAndIds = mappedIdTypesdAndIds;
        this.uriTemplate = uriTemplate;
        this.redirectURI = redirectURI;
        this.allMappedValues = allMappedValues;
    }

    public Response execute(){
        logger.info("execute() " + "redirectURI: " + this.redirectURI);
        return Response.status(javax.ws.rs.core.Response.Status.SEE_OTHER)
                .location(this.redirectURI)
                .build();
    }


}
