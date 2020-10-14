package de.pseudonymisierung.mainzelliste.auth.authenticator;

import de.pseudonymisierung.mainzelliste.auth.AuthenticationEum;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Implements the Apiikey authentication
 */

public class ApiKeyAuthenticator implements Authenticator {

    private final String apiKey;
    private final Logger logger = Logger.getLogger(ApiKeyAuthenticator.class);

    /**
     *  Creates a new Api Key Authenticator related to a User or a Server
     * @param apiKey the apiKey to authenticate the reuester
     */

    public ApiKeyAuthenticator(String apiKey) {
        this.apiKey = apiKey;
    }



    @Override
    public boolean isAuthenticated(Map<String, String> tokens) {
        String apiKey = tokens.get(AuthenticationEum.APIKEY.getAuthenticationName());
        boolean isAuthenticated = false;
        if(apiKey != null){
            isAuthenticated = this.apiKey.equals(apiKey);
        }
        if(isAuthenticated){
            logger.info("Requester could be authenticated with api key: " + apiKey);
        }
        else {
            logger.info("Requester could not been authenticated");
        }
       return isAuthenticated;
    }

}
