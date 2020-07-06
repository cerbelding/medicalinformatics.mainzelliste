package de.pseudonymisierung.mainzelliste.webservice.Authenticator;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter which formates OICD Properties to an authorizatiors conform format
 */
public class OICDPropertiesAdapter {
    private static final Logger logger = Logger.getLogger(OICDPropertiesAdapter.class);

    public static Map<String, String>  getMappedIdToken(JSONObject idToken){
        Map<String, String> userClaims = new HashMap<>();
        String sub;
        JSONArray roles;
        try {
            sub = idToken.getString("sub");
            userClaims.put("sub", sub);
            if(idToken.has("http://mainzelliste.de/roles")){
                roles = idToken.getJSONArray("http://mainzelliste.de/roles");
                for(int roleIndex = 0; roleIndex < roles.length(); roleIndex++) {
                    String role = roles.getString(roleIndex);
                    if (role != null) {
                        userClaims.put("role." + roleIndex, role);
                    }
                }
            }
        }
        catch (JSONException e) {
            logger.error("Error while ecoding IdToken: " +e);
            logger.info(userClaims.toString());
            return userClaims;
        }
        logger.info("IdToken successfully decoded " + userClaims.toString());
        return userClaims;
    }
}
