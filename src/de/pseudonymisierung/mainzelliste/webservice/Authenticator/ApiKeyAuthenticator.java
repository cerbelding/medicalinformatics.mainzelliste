package de.pseudonymisierung.mainzelliste.webservice.Authenticator;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

public class ApiKeyAuthenticator implements Authenticator {

    private final String apiKey;
    private final Logger logger = Logger.getLogger(OICDAuthenticator.class);

    public ApiKeyAuthenticator(String apiKey, Set<String> permissions) {
        this.apiKey = apiKey;
    }


    @Override
    public boolean isAuthenticated(Map<String, String> tokens) {
        String apiKey = tokens.get("apiKey");
        boolean isAuthenticated = false;
        if(apiKey != null){
            isAuthenticated = this.apiKey.equals(apiKey);
        }
       return isAuthenticated;
    }

}
