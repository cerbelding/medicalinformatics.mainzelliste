package de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT;

/**
 * Represents an decoded Json-Web-Token
 */
public interface IDecodedJWT {

  /**
   * Returns the Claim-value given by a key
   *
   * @param key the key for the claim value
   * @return the value as String
   */
  String getKey(String key);

  /**
   * Returns the issuer
   *
   * @return the issuer url
   */
  String getIssuer();

  /**
   * returns the subject
   *
   * @return the subject
   */
  String getSub();

}
