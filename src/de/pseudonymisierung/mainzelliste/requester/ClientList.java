package de.pseudonymisierung.mainzelliste.requester;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;


import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents the list of all requesters with an active Session
 */

public class ClientList {

  private final Map<String, Requester> clients;
  private static final Logger logger = LogManager.getLogger(ClientList.class);

  /**
   * Creates a empty requester list
   */
  public ClientList() {
    this.clients = new HashMap<>();
  }

  /**
   * Creates a List with the given requesters
   *
   * @param requesters List of requesters
   */
  public ClientList(Map<String, Requester> requesters) {
    this.clients = requesters;
  }


  public void add(Requester client) {
    clients.put(client.getId(), client);
  }

  /**
   * Get the size of the requester list
   *
   * @return The size of the requester list
   */
  public int size() {
    return clients.size();
  }

  /**
   * Checks if the list contains a requester given by a key
   *
   * @param key The id of a requester
   * @return true is the requester exist, otherwise false
   */
  public boolean containsKey(String key) {
    return clients.containsKey(key);
  }

  /**
   * Returns a requester by his id
   *
   * @param id The id of the requester
   * @return returns the user if the user exist, otherwise null
   */
  public Requester getUserById(String id) {
    return clients.get(id);
  }

  /**
   * Returns a requester by his name
   *
   * @param name the name of the requester
   * @return returns the requester if it exist, otherwise null
   */
  public Requester getRequesterByName(String name) {

    for (Map.Entry<String, Requester> entry : clients.entrySet()) {
      Requester client = entry.getValue();
      if (client.getName().equals(name)) {
        return client;
      }
    }
    return null;

  }

  /**
   * Get the requester if he could be authenticated, otherwise null
   *
   * @param claims the claims of the requester
   * @return The requester if he could be authenticated otherwise null
   */
  public Requester getRequesterByAuthentication(OIDCCredentials claims) {

    for (Map.Entry<String, Requester> entry : clients.entrySet()) {
      Requester client = entry.getValue();
      if (client.isAuthenticated(claims)) {
        return client;
      }
    }
    logger.info("requester could not been authenticated");
    return null;
  }


  public Collection<Requester> getClients() {
    return clients.values();
  }

  public void removeClient(String key){
    clients.remove(key);
  }
}
