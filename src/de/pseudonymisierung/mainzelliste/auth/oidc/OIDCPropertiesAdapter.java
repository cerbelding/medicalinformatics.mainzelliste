package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import de.pseudonymisierung.mainzelliste.auth.jwt.decoder.Auth0JWTDecoder;
import de.pseudonymisierung.mainzelliste.httpsClient.OICDService;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * Adapter which formats OIDC Properties to an authenticator conform format
 */
public class OIDCPropertiesAdapter {

  private static final Logger logger = Logger.getLogger(OIDCPropertiesAdapter.class);

  /**
   * Returns the user claims provided by the authorization server
   *
   * @param accessToken the JWT encoded access token
   * @return the user claims provided by the authorization server
   */
  public static OIDCCredentials getIdToken(String accessToken) {
    try {
      logger.debug("Try to decode access token: " + accessToken);
      IDecodedJWT jwtPayload = new Auth0JWTDecoder().decode(accessToken);
      String iss = jwtPayload.getIssuer();
      if (!Config.instance.getOidcServers().validateIssuer(iss)) {
        logger.info("Issuer could not been verified: " + iss);
        return null;
      }

      String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
      JSONObject idToken = OICDService
          .getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
      return new OIDCCredentials(idToken, jwtPayload);
    } catch (IOException ioException) {
      return null;
    }
  }
}
