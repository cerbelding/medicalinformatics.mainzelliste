package de.pseudonymisierung.mainzelliste.requester;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;
import de.pseudonymisierung.mainzelliste.auth.authenticator.OIDCPropertiesAdapter;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Represents the list of all registered users in the configuration file
 */

public class ClientList {

  private final Map<String, Requester> clients;
  private static final Logger logger = Logger.getLogger(ClientList.class);


  /**
   * Creates a empty UserList
   */
  public ClientList() {
    this.clients = new HashMap<>();
  }

  /**
   * Creates a List of all Users
   *
   * @param users List of Users
   */
  public ClientList(Map<String, Requester> users) {
    this.clients = users;
  }


  /**
   * Add User to User List
   *
   * @param client User to be add
   */
  public void add(Requester client) {
    clients.put(client.getId(), client);
  }

  /**
   * Get the size of the User List
   *
   * @return The size of the User List
   */
  public int size() {
    return clients.size();
  }

  /**
   * Checks if a User contains in the List
   *
   * @param key The id of a User
   * @return true is a User exist, otherwise false
   */
  public boolean containsKey(String key) {
    return clients.containsKey(key);
  }

  /**
   * Returns a User by his id
   *
   * @param id The id of the user
   * @return returns the user if the user exist, otherwise null
   */
  public Requester getUserById(String id) {
    return clients.get(id);
  }

  /**
   * Returns a User by his name
   *
   * @param name the name of the User
   * @return returns the user if it exist, otherwise null
   */
  public Requester getUserByName(String name) {

    for (Map.Entry<String, Requester> entry : clients.entrySet()) {
      Requester client = entry.getValue();
      if (client.getName().equals(name)) {
        return client;
      }
    }
    return null;

  }


  /**
   * Get the user if he could be authenticated, otherwise null
   *
   * @param claims the claims of the user
   * @return The User if he could be authenticated otherwise null
   */
  private Requester getUserByAuthentication(UserInfoClaims claims) {

    for (Map.Entry<String, Requester> entry : clients.entrySet()) {
      Requester client = entry.getValue();
      if (client.isAuthenticated(claims)) {
        return client;
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
  public Requester getUserByAccessToken(String accessToken) {
      UserInfoClaims claims = OIDCPropertiesAdapter.getIdToken(accessToken);
      if(claims != null){
        return getUserByAuthentication(claims);
      }
      return null;

  }

  public Collection<Requester> getClients() {
    return clients.values();
  }

  public void removeClient(String key){
    clients.remove(key);
  }
}
