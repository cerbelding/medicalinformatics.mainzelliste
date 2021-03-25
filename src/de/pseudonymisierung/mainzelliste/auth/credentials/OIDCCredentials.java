package de.pseudonymisierung.mainzelliste.auth.credentials;

import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEum;
import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Represents the OIDC-Authentication
 */
public class OIDCCredentials implements ClientCredentials, AuthorizationServerCredentials {

  JSONObject userInfo;
  IDecodedJWT decodedJWT;
  private final Logger logger = LogManager.getLogger(OIDCCredentials.class);


  public OIDCCredentials(JSONObject userInfo, IDecodedJWT jwt) {
    this.userInfo = userInfo;
    this.decodedJWT = jwt;
  }

  public String getServerId() {
    return decodedJWT.getIssuer();
  }

  public String get(String key) {
    try {
      if (userInfo.get(key) instanceof String) {
        return userInfo.getString(key);
      } else {
        return "";
      }
    } catch (JSONException e) {
      return "";
    }
  }

  @Override
  public String getId() {
    return decodedJWT.getSub();
  }

  public List<String> getValuesByKey(String key) {
    List<String> values = new ArrayList<>();
    try {
      if (userInfo.get(key) instanceof JSONArray) {
        JSONArray array = userInfo.getJSONArray(key);
        for (int arrayIndex = 0; arrayIndex < array.length(); arrayIndex++) {
          String value = array.getString(arrayIndex);
          values.add(value);
        }
      } else if (userInfo.get(key) instanceof String) {
        String value = userInfo.getString(key);
        values.add(value);
      }
    } catch (JSONException json) {
      logger.error("Key could not been parsed");
    }
    return values;
  }

  @Override
  public AuthenticationEum getAuthEnum() {
    return AuthenticationEum.ACCESS_TOKEN;
  }
}
