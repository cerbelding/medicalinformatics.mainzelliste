package de.pseudonymisierung.mainzelliste.auth.authenticator;

import de.pseudonymisierung.mainzelliste.auth.oidc.JWTDecoder;
import de.pseudonymisierung.mainzelliste.httpsClient.OICDService;
import java.io.IOException;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import org.codehaus.jettison.json.JSONString;

/**
 * Adapter which formates OICD Properties to an authorizatiors conform format
 */
public class OIDCPropertiesAdapter {

  private static final Logger logger = Logger.getLogger(OIDCPropertiesAdapter.class);

  public static Map<String, String> getMappedIdToken(JSONObject idToken) {
    Map<String, String> userClaims = new HashMap<>();
    Iterator<String> keys = idToken.keys();

    while (keys.hasNext()) {
      try {
        String key = keys.next();
        if (idToken.get(key) instanceof JSONArray) {
          JSONArray array = idToken.getJSONArray(key);
          for (int arrayIndex = 0; arrayIndex < array.length(); arrayIndex++) {
            String value = array.getString(arrayIndex);
            if (value != null) {
              userClaims.put(key + "." + arrayIndex, value);
            }
          }
        } else if (idToken.get(key) instanceof String) {
          String value = idToken.getString(key);
          userClaims.put(key, value);
        }
      } catch (JSONException e) {
        logger.error("Error while ecoding IdToken: " + e);
        logger.info(userClaims.toString());
        continue;
      }

    }
    return userClaims;
  }

  /**
   * Returns the user claims provided by the authorization server
   *
   * @param accessToken the JWT encoded access token
   * @return the user claims provided by the authorization server
   * @throws JSONException throws exception if JWT could not been decoded
   * @throws IOException   throws exception if jwt token does not match format
   */
  public static ClaimMap getIdToken(String accessToken) {
    try {
      logger.debug("Try to decode access token: " + accessToken);
      JSONObject jwtPayload = JWTDecoder.decode(accessToken);
      String iss = jwtPayload.getString("iss");
      logger.info("Issuer of the access token is: " + iss);
      String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
      JSONObject idToken = OICDService
          .getIdTokenFromUserInfoEndpoint(accessToken, userInfoEndpointUrl);
      return new ClaimMap(idToken, iss);
    } catch (JSONException jsonException) {
      return new ClaimMap();
    } catch (IOException ioException) {
      return new ClaimMap();
    }

  }
}
