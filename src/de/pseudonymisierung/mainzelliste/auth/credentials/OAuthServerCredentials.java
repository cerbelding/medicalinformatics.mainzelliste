package de.pseudonymisierung.mainzelliste.auth.credentials;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.OAuthAuthorizationServer;

public class OAuthServerCredentials implements AuthorizationServerCredentials {

  private OAuthAuthorizationServer oAuthAuthorizationServer;

  public OAuthServerCredentials(OAuthAuthorizationServer oAuthAuthorizationServer){
    this.oAuthAuthorizationServer = oAuthAuthorizationServer;
  }

  @Override
  public String getServerId() {
    return oAuthAuthorizationServer.getId();
  }
}
