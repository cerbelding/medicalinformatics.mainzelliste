package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;

public interface IAuthorization {

  String getId();
  boolean authorize(UserInfoClaims userInfoClaims);

}
