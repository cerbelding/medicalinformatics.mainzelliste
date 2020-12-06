package de.pseudonymisierung.mainzelliste.auth.credentials;

import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Represents the OIDC-Authentication
 */
public class OIDCCredentials implements Credentials {

  JSONObject claims;
  IDecodedJWT decodedJWT;
  private final Logger logger = Logger.getLogger(OIDCCredentials.class);


  public OIDCCredentials(JSONObject claims, IDecodedJWT jwt) {
    this.claims = claims;
    this.decodedJWT = jwt;
  }

  public String getIss() {
    return decodedJWT.getIssuer();
  }

  public String get(String key) {
    try {
      if (claims.get(key) instanceof String) {
        return claims.getString(key);
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
      if (claims.get(key) instanceof JSONArray) {
        JSONArray array = claims.getJSONArray(key);
        for (int arrayIndex = 0; arrayIndex < array.length(); arrayIndex++) {
          String value = array.getString(arrayIndex);
          values.add(value);
        }
      } else if (claims.get(key) instanceof String) {
        String value = claims.getString(key);
        values.add(value);
      }
    } catch (JSONException json) {
      logger.error("Key could not been parsed");
    }
    return values;
  }
}
