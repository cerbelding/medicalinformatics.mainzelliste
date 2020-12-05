package de.pseudonymisierung.mainzelliste.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;

public class Auth0JWT implements IDecodedJWT {
  DecodedJWT decodedJWT;

  public Auth0JWT(DecodedJWT  jwt){
    this.decodedJWT = jwt;
  }

  @Override
  public String getKey(String key) {
    return null;
  }

  @Override
  public String getIssuer() {
    return decodedJWT.getIssuer();
  }

  @Override
  public String getSub() {
    return decodedJWT.getSubject();
  }


}
