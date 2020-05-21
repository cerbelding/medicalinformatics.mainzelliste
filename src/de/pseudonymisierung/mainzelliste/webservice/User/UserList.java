package de.pseudonymisierung.mainzelliste.webservice.User;

import de.pseudonymisierung.mainzelliste.webservice.Authorizator.OICDAuthorizator;
import de.pseudonymisierung.mainzelliste.webservice.HttpsClient.OICDService;
import de.pseudonymisierung.mainzelliste.webservice.JWTDecoder;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Represents the list of all registered users in the configuration file
 */

public class UserList {
    private final List<User> userList;
    private static final Logger logger = Logger.getLogger(OICDAuthorizator.class);


    public UserList(List<User> userList){
        this.userList = userList;

    }


    /**
     * Returns the user claims provided by the authorization server
     * @param accessToken  the JWT encoded access token
     * @return the user claims provided by the authorization server
     * @throws JSONException throws exception if JWT could not been decoded
     * @throws IOException throws exception if jwt token does not match format
     */
    private Map<String,String> getIOCDIdToken(String accessToken) throws JSONException,IOException{
        JSONObject jwtPayload = JWTDecoder.decode(accessToken);
        String iss = jwtPayload.getString("iss");
        String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
        JSONObject idToken = OICDService.getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
        return OICDPropertiesAdapter.getMappedIdToken(idToken);
    }

    /**
     * Searchs an user with the delivered identification claims
     * @param claims the user identification claims
     * @return true is the founded user has permission to the requested resource, if user was not found or he hasn't permission returns false
     */
    private boolean hasUserPermission(Map<String,String> claims, String permission){

        for (User user : userList) {
            if (user.isAuthenticated(claims)) {
                return user.hasPermission(claims, permission);
            }
        }
        logger.warn("User could not been authenticated");
        return false;
    }

    /**
     * Checks if an user has permission for a requested resource
     * @param accessToken the JWT encoed access token
     * @param permission the requested resource permission
     * @return returns true if the user has permission, otherwise false (even if user is not found)
     */

    public boolean hasOICDPermission(String accessToken, String permission){

        try {
            Map<String, String>  claims = getIOCDIdToken(accessToken);
            return hasUserPermission(claims, permission);

        } catch (JSONException | IOException e) {
           logger.error(e);
            return false;
        }
    }


}
