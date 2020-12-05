package de.pseudonymisierung.mainzelliste.auth.jwt.decoder;

import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;

public interface IJWTDecoder {

  IDecodedJWT decode(String accessToken);
  boolean verify(String accessToken, String algorithm);

}
