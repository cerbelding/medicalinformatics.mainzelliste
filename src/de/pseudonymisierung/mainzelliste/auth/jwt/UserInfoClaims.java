package de.pseudonymisierung.mainzelliste.auth.jwt;

import de.pseudonymisierung.mainzelliste.auth.jwt.decodedJWT.IDecodedJWT;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class UserInfoClaims {
  JSONObject claims;
  IDecodedJWT decodedJWT;

  public UserInfoClaims(JSONObject claims, IDecodedJWT jwt){
    this.claims = claims;
    this.decodedJWT = jwt;
  }

  public String getIss() {
   return decodedJWT.getIssuer();
  }

  public String get(String key) {
    try {
    if (claims.get(key) instanceof String) {
      String value = claims.getString(key);
      return value;
    }
    else{
        return "";
      }
  }
  catch(JSONException e) {
    return "";
    }
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
    }
    catch (JSONException json){
    }
    return values;
  }


}
