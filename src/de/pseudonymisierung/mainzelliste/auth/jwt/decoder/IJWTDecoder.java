package de.pseudonymisierung.mainzelliste.auth.jwt.decoder;

import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;

/**
 * Represents an JWT-Decoder
 */
public interface IJWTDecoder {

  /**
   * Decode the JWT encoded access token
   *
   * @param accessToken the access token
   * @return the decoded JWT
   */
  IDecodedJWT decode(String accessToken);
}
