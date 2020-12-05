package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;

public class OIDCServer extends OAuthAuthorizationServer implements IAuthorization {

  public OIDCServer(String issuer, String id) {
    super(issuer, id);
  }

  @Override
  public boolean authorize(UserInfoClaims userInfoClaims) {
    return this.issuer.equals(userInfoClaims.getIss());
  }
}
