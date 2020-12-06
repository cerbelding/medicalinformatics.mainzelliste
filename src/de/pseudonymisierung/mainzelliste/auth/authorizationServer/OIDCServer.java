package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;

/**
 * Represents the OIDC-Server
 */
public class OIDCServer extends OAuthAuthorizationServer implements IAuthorization {

  public OIDCServer(String issuer, String id) {
    super(issuer, id);
  }

  @Override
  public boolean authorize(OIDCCredentials oidcCredentials) {
    return this.issuer.equals(oidcCredentials.getIss());
  }
}
