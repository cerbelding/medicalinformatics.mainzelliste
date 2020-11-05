package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.AuthorizationServer;

public class OIDCServer extends AuthorizationServer {

  public OIDCServer(String issuer){
    super(issuer, ".well-known/openid-configuration");
  }

}
