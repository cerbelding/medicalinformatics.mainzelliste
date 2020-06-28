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
    private final Map<String, User> userList;
    private static final Logger logger = Logger.getLogger(UserList.class);


    /**
     * Creates a empty UserList
     */
    public UserList(){
        this.userList = new HashMap<>();
    }

    /**
     * Creates a List of all Users
     * @param userList List of Users
     */
    public UserList(Map<String,User> userList){
        this.userList = userList;
    }


    /**
     * Add User to User List
     * @param user User to be add
     */
    public void add(User user){
        userList.put(user.id, user);
    }

    /**
     * Get the size of the User List
     * @return The size of the User List
     */
    public int size(){
        return userList.size();
    }

    /**
     * Checks if a User contains in the List
     * @param key The id of a User
     * @return true is a User exist, otherwise false
     */
    public boolean containsKey(String key){
        return userList.containsKey(key);
    }

    /**
     * Returns a User by his id
     * @param id The id of the user
     * @return returns the user if the user exist, otherwise null
     */
    public User getUserById(String id){
        return userList.get(id);
    }

    /**
     * Returns a User by his name
     * @param name the name of the User
     * @return returns the user if it exist, otherwise null
     */
    public User getUserByName(String name){
        for (Map.Entry<String, User> entry : userList.entrySet()) {
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
        for (Map.Entry<String, User> entry : userList.entrySet()) {
            User user = entry.getValue();
            if(user.isAuthenticated(claims)) return user;
        }
        logger.info("User could not been authenticated");
        return null;
    }

    /**
     * Returns the permissions if the user could be authenticated, otherwise a empty Set
     * @param claims the user identification claims
     * @return if the user could be authenticated the permissions, otherwise a empty Set
     */
    private Set<String> getUserPermissions(Map<String,String> claims){

        User user =  getUserByAuthentication(claims);
        if(user != null){
            return user.getPermissions();
        }
        else {
            return new HashSet<>();
        }
    }


    /**
     * Checks if an user has permission for a requested resource
     * @param accessToken the JWT encoed access token
     * @return returns true if the user has permission, otherwise false (even if user is not found)
     */
    public Set<String> getOICDPermission(String accessToken){

        try {
            Map<String, String>  claims = getIOCDIdToken(accessToken);
            return getUserPermissions(claims);

        } catch (JSONException | IOException e) {
            logger.error(e);
            return new HashSet<>();
        }
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
