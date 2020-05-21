package de.pseudonymisierung.mainzelliste.webservice.User;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter which formates OICD Properties to an authorizatiors conform format
 */
public class OICDPropertiesAdapter {

    public static Map<String, String>  getMappedIdToken(JSONObject idToken){
        Map<String, String> userClaims = new HashMap<>();
        String sub;
        JSONArray roles;
        try {
            sub = idToken.getString("sub");
            userClaims.put("sub", sub);
            roles = idToken.getJSONArray("roles");

            for(int roleIndex = 0; roleIndex < roles.length(); roleIndex++){
                String role = roles.getString(roleIndex);
                if(role != null){
                    userClaims.put("role."+roleIndex,role);
                }
            }
        }
        catch (JSONException e) {
            userClaims.put("roles", "");
            userClaims.put("sub", "");
        }
        return userClaims;
    }
}
