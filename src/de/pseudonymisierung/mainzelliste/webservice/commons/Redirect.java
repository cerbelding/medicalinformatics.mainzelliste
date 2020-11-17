package de.pseudonymisierung.mainzelliste.webservice.commons;

import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Redirect {

    private Logger logger = LogManager.getLogger(Redirect.class);


    private Map<String, String> tokenId;
    private Map<String, String> mappedIdTypesdAndIds;
    private Map<String, String> similarityScores;
    private UriTemplate uriTemplate;
    private URI redirectURI;

    private Map<String, String> allMappedValues;

    public Redirect(Map<String, String> tokenId, Map<String, String> mappedIdTypesdAndIds, Map<String, String> similarityScores, UriTemplate uriTemplate, URI redirectURI, Map<String, String> allMappedValues) {
        this.tokenId = tokenId;
        this.mappedIdTypesdAndIds = mappedIdTypesdAndIds;
        this.similarityScores = similarityScores;
        this.uriTemplate = uriTemplate;
        this.redirectURI = redirectURI;
        this.allMappedValues = allMappedValues;
    }

    public Response execute(){
        logger.info("execute() redirectURI: {}", this.redirectURI);
        return Response.seeOther(this.redirectURI)
                .build();
    }

    /**
     * Returns the redirect {@link URI}
     * @return {@link URI} - the redirect URI
     */
    public URI getRedirectURI(){
        return this.redirectURI;
    }
    
    /**
     * Returns all redirect parameters
     */
    public MultivaluedMap<String, String> getRedirectParams(){
        return UriComponent.decodeQuery(this.redirectURI, true);
    }


}
