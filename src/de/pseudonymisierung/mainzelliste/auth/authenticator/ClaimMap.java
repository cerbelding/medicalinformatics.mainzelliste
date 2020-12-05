package de.pseudonymisierung.mainzelliste.auth.authenticator;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ClaimMap {
  JSONObject claims;
  String iss;

  public ClaimMap(){
    this.claims = new JSONObject();
    this.iss = "";
  }

  public ClaimMap(JSONObject claims, String iss){
    this.claims = claims;
    this.iss = iss;
  }

  public String getIss() {
    try {
      if (claims.get("iss") instanceof String) {
        return iss;
      }
      else{
        return this.iss;
      }
    } catch (JSONException jsonException) {
      return this.iss;
    }
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
