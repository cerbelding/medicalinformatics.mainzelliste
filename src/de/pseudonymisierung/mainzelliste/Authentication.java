package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;
import de.pseudonymisierung.mainzelliste.webservice.Requester.Requester;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks if the Requester could be authenticated
 */
public final class Authentication {
    private final static Logger logger = Logger.getLogger(Authentication.class);

    private Authentication(){
    }

    public static Map<AuthenticationEum,String> getAuthenticationHeader(HttpServletRequest req){
        Map<AuthenticationEum,String> authenticationMap = new HashMap<>();
        try {
            String apiKey = req.getHeader("mainzellisteApiKey");
            if (apiKey == null) // Compatibility to pre 1.0 (needed by secuTrial interface)
                apiKey = req.getHeader("mzidApiKey");

            if(apiKey != null){
                authenticationMap.put(AuthenticationEum.APIKEY, apiKey);
            }
            String authorizationHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
            if(authorizationHeader != null){
                String token =  authorizationHeader.split(" ")[1];
                if(token != null) authenticationMap.put(AuthenticationEum.ACCESS_TOKEN, token);
            }
        } catch (Exception e) {
            logger.error(e);
            return authenticationMap;
        }
        return  authenticationMap;
    }

    public static Requester  authenticate(Map<AuthenticationEum, String> httpHeader){
        if(httpHeader.containsKey(AuthenticationEum.APIKEY)){
            String apiKey = httpHeader.get(AuthenticationEum.APIKEY);
            Requester requester = Servers.instance.getRequesterByAPIKey(apiKey);
            return requester;
        }
        else if(httpHeader.containsKey(AuthenticationEum.ACCESS_TOKEN)){
            String accessToken = httpHeader.get(AuthenticationEum.ACCESS_TOKEN);
            Requester requester = Servers.instance.getRequesterByAccessToken(accessToken);
            return requester;
        }
        else {
            return null;
        }
    }
}
