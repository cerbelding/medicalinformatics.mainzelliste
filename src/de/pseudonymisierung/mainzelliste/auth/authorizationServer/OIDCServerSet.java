package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.OIDCServer;
import java.util.Set;

public class OIDCServerSet {
  private final Set<OIDCServer> oidcServerSet;

  public OIDCServerSet(Set<OIDCServer> oidcServerSet){
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
}
