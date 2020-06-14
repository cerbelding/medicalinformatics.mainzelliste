package de.pseudonymisierung.mainzelliste.webservice.Requester;

import de.pseudonymisierung.mainzelliste.webservice.Authenticator.OICDAuthenticator;
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


    public UserList(){
        this.userList = new HashMap<>();
    }

    public UserList(Map<String,User> userList){
        this.userList = userList;
    }


    public void add(User user){

        userList.put(user.id, user);
    }

    public int size(){
        return userList.size();
    }
    public boolean containsKey(String key){
        return userList.containsKey(key);
    }

    public User getUserById(String id){
        return userList.get(id);
    }

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
        logger.debug("Try to decoe access token: "+accessToken);
        JSONObject jwtPayload = JWTDecoder.decode(accessToken);
        String iss = jwtPayload.getString("iss");
        String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
        JSONObject idToken = OICDService.getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
        return  OICDPropertiesAdapter.getMappedIdToken(idToken);
    }



    /**
     *
     * @param claims
     * @return
     */
    @org.jetbrains.annotations.Nullable
    private User getaUserByAuthentication(Map<String,String> claims){
        for (Map.Entry<String, User> entry : userList.entrySet()) {
            User user = entry.getValue();
            if(user.isAuthenticated(claims)) return user;
        }
        logger.warn("User could not been authenticated");
        return null;
    }

    /**
     * Searchs an user with the delivered identification claims
     * @param claims the user identification claims
     * @return the permissions of the founded user
     */
    private Set<String> getUserPermissions(Map<String,String> claims){

        User user =  getaUserByAuthentication(claims);
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
            return getaUserByAuthentication(claims);

        } catch (JSONException | IOException e) {
            logger.error(e);
            return null;
        }
    }


}
