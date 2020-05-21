package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import de.pseudonymisierung.mainzelliste.webservice.Authorizator.OICDAuthorizator;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;

public class OICDService {

    private static final String USERINFOENDPOINT = "userinfo_endpoint";
    private static final Logger logger = Logger.getLogger(OICDAuthorizator.class);

    /**
     * Return the UserInformation provided by the Userinfo endpoint from the openId Provider
     * @return Userinformation as JSONObject
     */
    private static JSONObject getUserInformations(String accessToken, String userInfoEndpointUrl) {
        JSONObject userInfo = new JSONObject();
        if (userInfoEndpointUrl.isEmpty()) {
            return userInfo;
        } else {
            HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
            httpHeader.setAuthorization(HttpClientAuthorization.createBearerAuthentication(accessToken));
            userInfo = new HttpsClient(userInfoEndpointUrl).request(httpHeader, new HttpUrlParameterBuilder());
            return userInfo;
        }
    }


    /**
     * Returns the User Claims as a Map
     * @param accessToken The acessToken of the User
     * @param userInfoEndpointUrl  The url of authorization's servers userinfo_endpoint
     * @return The sub, roles Claims of the User stored in a Map
     */

    public static Map<String, String> getUserClaims(String accessToken, String userInfoEndpointUrl){
        JSONObject userInfo = getUserInformations(accessToken, userInfoEndpointUrl);
        Map<String, String> userClaims = new HashMap<>();
        String sub;
        String roles;
        try {
            sub = userInfo.getString("sub");
            roles = userInfo.getString("roles");
            userClaims.put("roles", roles);
            userClaims.put("sub", sub);
        }
        catch (JSONException e) {
            userClaims.put("roles", "");
            userClaims.put("sub", "");
        }
        return userClaims;
    }

    /**
     * Retrieves the Userinfo endpoint Url from the OpenId Configuration
     * @return the Url to the Userinfo endpoint
     */
    public static String getUserInfoEndPointURL(String iss){
        try {
            HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
            JSONObject json = new HttpsClient(iss).request(httpHeader, new HttpUrlParameterBuilder());
            return  json.getString(USERINFOENDPOINT);
        } catch(JSONException e){
            logger.error(e);
            return "";
        }
    }
}
