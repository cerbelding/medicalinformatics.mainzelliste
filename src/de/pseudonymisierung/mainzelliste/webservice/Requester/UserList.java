package de.pseudonymisierung.mainzelliste.webservice.Requester;

import de.pseudonymisierung.mainzelliste.webservice.Authenticator.OICDPropertiesAdapter;
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
    private final Map<String, User> users;
    private static final Logger logger = Logger.getLogger(UserList.class);


    /**
     * Creates a empty UserList
     */
    public UserList(){
        this.users = new HashMap<>();
    }

    /**
     * Creates a List of all Users
     * @param users List of Users
     */
    public UserList(Map<String,User> users){
        this.users = users;
    }


    /**
     * Add User to User List
     * @param user User to be add
     */
    public void add(User user){
        users.put(user.id, user);
    }

    /**
     * Get the size of the User List
     * @return The size of the User List
     */
    public int size(){
        return users.size();
    }

    /**
     * Checks if a User contains in the List
     * @param key The id of a User
     * @return true is a User exist, otherwise false
     */
    public boolean containsKey(String key){
        return users.containsKey(key);
    }

    /**
     * Returns a User by his id
     * @param id The id of the user
     * @return returns the user if the user exist, otherwise null
     */
    public User getUserById(String id){
        return users.get(id);
    }

    /**
     * Returns a User by his name
     * @param name the name of the User
     * @return returns the user if it exist, otherwise null
     */
    public User getUserByName(String name){
        for (Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if(user.getName().equals(name)){
                return user;
            }
        }
        return null;
    }



    /**
     * Returns the user claims provided by the authorization server
     * @param accessToken  the JWT encoded access token
     * @return the user claims provided by the authorization server
     * @throws JSONException throws exception if JWT could not been decoded
     * @throws IOException throws exception if jwt token does not match format
     */
    private Map<String,String> getIOCDIdToken(String accessToken) throws JSONException,IOException{
        logger.debug("Try to decode access token: "+accessToken);
        JSONObject jwtPayload = JWTDecoder.decode(accessToken);
        String iss = jwtPayload.getString("iss");
        logger.info("Issuer of the access token is: " + iss);
        String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
        JSONObject idToken = OICDService.getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
        return  OICDPropertiesAdapter.getMappedIdToken(idToken);
    }


    /**
     * Get the user if he could be authenticated, otherwise null
     * @param claims the claims of the user
     * @return The User if he could be authenticated otherwise null
     */
    private User getUserByAuthentication(Map<String,String> claims){
        for (Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            if(user.isAuthenticated(claims)) return user;
        }
        logger.info("User could not been authenticated");
        return null;
    }


    /**
     * Return the User with the requested accessToken
     * @param accessToken the JWT encoed access token
     * @return returns the User if it could be found, otherwise null
     */
    public User getUserByOICD(String accessToken){

        try {
            Map<String, String>  claims = getIOCDIdToken(accessToken);
            return getUserByAuthentication(claims);

        } catch (JSONException | IOException e) {
            logger.error(e);
            return null;
        }
    }


}
