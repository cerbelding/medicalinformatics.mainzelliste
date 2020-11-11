package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.AuthorizationServer;

public class OIDCServer extends AuthorizationServer {

  public OIDCServer(String issuer, String id){
    super(issuer, id, ".well-known/openid-configuration");
  }


}
