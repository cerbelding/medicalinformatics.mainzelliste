package de.pseudonymisierung.mainzelliste.auth;

import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;

public interface IAuthorization {

  String getId();
  boolean authorize(ClaimMap claimMap);

}
