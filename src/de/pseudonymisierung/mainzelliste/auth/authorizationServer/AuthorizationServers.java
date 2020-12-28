package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;
import java.util.Set;

/**
 * Represents a Set of AuthorizationServers
 */
public class AuthorizationServers {

  private final Set<AuthorizationServer> oidcServerSet;

  public AuthorizationServers(Set<AuthorizationServer> oidcServerSet) {
    this.oidcServerSet = oidcServerSet;
  }

  public AuthorizationServer getAuthorizationServerByName(String id) {
    return oidcServerSet.stream().filter(el -> el.getName().equals(id)).findFirst()
        .orElse(null);
  }

  public boolean validate(AuthorizationServerCredentials authorizationServerCredentials) {
    for (AuthorizationServer oidcServer : oidcServerSet) {
      if (oidcServer.authorize(authorizationServerCredentials)) {
        return true;
      }
    }
    return false;
  }

}
