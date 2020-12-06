package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;

/**
 * Represents an Authorization-Server (e.g OIDC-Server,..)
 */
public interface IAuthorization {

  boolean authorize(OIDCCredentials userInfoClaims);

  String getId();
}
