package de.pseudonymisierung.mainzelliste.auth.requester;

import de.pseudonymisierung.mainzelliste.auth.authenticator.OICDPropertiesAdapter;
import de.pseudonymisierung.mainzelliste.auth.httpsClient.OICDService;
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

  private final Map<String, UserGroup> users;
  private static final Logger logger = Logger.getLogger(UserList.class);


  /**
   * Creates a empty UserList
   */
  public UserList() {
    this.users = new HashMap<>();
  }

  /**
   * Creates a List of all Users
   *
   * @param users List of Users
   */
  public UserList(Map<String, UserGroup> users) {
    this.users = users;
  }


  /**
   * Add User to User List
   *
   * @param userGroup User to be add
   */
  public void add(UserGroup userGroup) {
    users.put(userGroup.id, userGroup);
  }

  /**
   * Get the size of the User List
   *
   * @return The size of the User List
   */
  public int size() {
    return users.size();
  }

  /**
   * Checks if a User contains in the List
   *
   * @param key The id of a User
   * @return true is a User exist, otherwise false
   */
  public boolean containsKey(String key) {
    return users.containsKey(key);
  }

  /**
   * Returns a User by his id
   *
   * @param id The id of the user
   * @return returns the user if the user exist, otherwise null
   */
  public UserGroup getUserById(String id) {
    return users.get(id);
  }

  /**
   * Returns a User by his name
   *
   * @param name the name of the User
   * @return returns the user if it exist, otherwise null
   */
  public UserGroup getUserByName(String name) {
    for (Map.Entry<String, UserGroup> entry : users.entrySet()) {
      UserGroup userGroup = entry.getValue();
      if (userGroup.getName().equals(name)) {
        return userGroup;
      }
    }
    return null;
  }


  /**
   * Returns the user claims provided by the authorization server
   *
   * @param accessToken the JWT encoded access token
   * @return the user claims provided by the authorization server
   * @throws JSONException throws exception if JWT could not been decoded
   * @throws IOException   throws exception if jwt token does not match format
   */
  private Map<String, String> getIOCDIdToken(String accessToken) throws JSONException, IOException {
    logger.debug("Try to decode access token: " + accessToken);
    JSONObject jwtPayload = JWTDecoder.decode(accessToken);
    String iss = jwtPayload.getString("iss");
    logger.info("Issuer of the access token is: " + iss);
    String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
    JSONObject idToken = OICDService
        .getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
    return OICDPropertiesAdapter.getMappedIdToken(idToken);
  }


  /**
   * Get the user if he could be authenticated, otherwise null
   *
   * @param claims the claims of the user
   * @return The User if he could be authenticated otherwise null
   */
  private UserGroup getUserByAuthentication(Map<String, String> claims) {
    for (Map.Entry<String, UserGroup> entry : users.entrySet()) {
      UserGroup userGroup = entry.getValue();
      if (userGroup.isAuthenticated(claims)) {
        return userGroup;
      }
    }
    logger.info("User could not been authenticated");
    return null;
  }


  /**
   * Return the User with the requested accessToken
   *
   * @param accessToken the JWT encoed access token
   * @return returns the User if it could be found, otherwise null
   */
  public UserGroup getUserByOICD(String accessToken) {

    try {
      Map<String, String> claims = getIOCDIdToken(accessToken);
      return getUserByAuthentication(claims);

    } catch (JSONException | IOException e) {
      logger.error(e);
      return null;
    }
  }


}
