package de.pseudonymisierung.mainzelliste.auth.oicd;

import de.pseudonymisierung.mainzelliste.auth.AuthorizationServer;

public class OICDServer extends AuthorizationServer {

  public OICDServer(String issuer){
    super(issuer, ".well-known/openid-configuration");
  }

}
