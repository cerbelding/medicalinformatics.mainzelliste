package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import java.util.Set;

/**
 * Represents a Set of AuthorizationServers
 */
public class OIDCServers {

  private final Set<OIDCServer> oidcServerSet;

  public OIDCServers(Set<OIDCServer> oidcServerSet) {
    this.oidcServerSet = oidcServerSet;
  }

  public Set<OIDCServer> getOidcServerSet() {
    return oidcServerSet;
  }

  public boolean validateIssuer(String iss) {
    for (OIDCServer oidcServer : oidcServerSet) {
      if (oidcServer.getIssuer().equals(iss)) {
        return true;
      }
    }
    return false;
  }

  public String getIdByIssuer(String iss) {
    for (OIDCServer oidcServer : oidcServerSet) {
      if (oidcServer.getIssuer().equals(iss)) {
        return oidcServer.id;
      }
    }
    return "";
  }
}
