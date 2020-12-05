package de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT;

public interface IDecodedJWT {

  String getKey(String key);
  String getIssuer();
  String getSub();

}
