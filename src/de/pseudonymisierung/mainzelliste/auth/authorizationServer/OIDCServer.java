package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;
import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;

/**
 * Represents the OIDC-Server
 */
public class OIDCServer extends OAuthAuthorizationServer implements AuthorizationServer {

  public OIDCServer(String issuer, String name) {
    super(issuer, name);
  }


}
