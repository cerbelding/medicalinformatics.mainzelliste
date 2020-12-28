package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;

public interface AuthorizationServer {

  String getId();
  String getName();
  boolean authorize(AuthorizationServerCredentials authorizationServer);
}
