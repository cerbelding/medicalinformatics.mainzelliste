package de.pseudonymisierung.mainzelliste.auth.jwt.decoder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.pseudonymisierung.mainzelliste.auth.jwt.Auth0JWT;
import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;

public class Auth0JWTDecoder implements IJWTDecoder {

  /**
   * Decodes the JWT
   *
   * @param jwtToken the JWT encoded token
   * @return the decoded JWT Interface
   */
  public IDecodedJWT decode(String jwtToken) {
    try {
      DecodedJWT jwt = JWT.decode(jwtToken);
      return new Auth0JWT(jwt);
    } catch (JWTDecodeException exception) {
      return null;
    }
  }

  @Override
  public boolean verify(String accessToken, String algorithm) {
    // TODO: 05.12.2020 Verify Access token 
    throw new NotImplementedException();
  }
}
