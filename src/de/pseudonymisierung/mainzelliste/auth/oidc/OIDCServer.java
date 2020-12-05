package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.IAuthorization;
import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import de.pseudonymisierung.mainzelliste.auth.OAuthAuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;

public class OIDCServer extends OAuthAuthorizationServer implements IAuthorization {

  public OIDCServer(String issuer, String id) {
    super(issuer, id, ".well-known/openid-configuration");
  }

  @Override
  public boolean authorize(ClaimMap claimMap) {
    return this.issuer.equals(claimMap.getIss());
  }
}
