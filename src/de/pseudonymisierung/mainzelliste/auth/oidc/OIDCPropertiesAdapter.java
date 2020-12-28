package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.OIDCServer;
import de.pseudonymisierung.mainzelliste.auth.credentials.OAuthServerCredentials;
import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import de.pseudonymisierung.mainzelliste.auth.jwt.decoder.Auth0JWTDecoder;
import de.pseudonymisierung.mainzelliste.httpsClient.OICDService;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * Adapter which formats OIDC Properties to an authenticator conform format
 */
public class OIDCPropertiesAdapter {

  private static final Logger logger = LogManager.getLogger(OIDCPropertiesAdapter.class);

  /**
   * Returns the user claims provided by the authorization server
   *
   * @param accessToken the JWT encoded access token
   * @return the user claims provided by the authorization server
   */
  public static OIDCCredentials getOIDCInformationsByAccessToken(String accessToken) {
    try {
      logger.debug("Try to decode access token: " + accessToken);
      IDecodedJWT jwtPayload = new Auth0JWTDecoder().decode(accessToken);
      String iss = jwtPayload.getIssuer();
      OAuthServerCredentials oAuthServerCredentials = new OAuthServerCredentials(new OIDCServer(iss, iss));
      if (!Config.instance.getAuthorizationServers().validate(oAuthServerCredentials)) {
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
