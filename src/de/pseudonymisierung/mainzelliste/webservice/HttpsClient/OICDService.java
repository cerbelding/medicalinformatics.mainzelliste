package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import de.pseudonymisierung.mainzelliste.webservice.Authenticator.OICDAuthenticator;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.HttpMethod;

public class OICDService {

    private static final String USERINFOENDPOINT = "userinfo_endpoint";
    private static final Logger logger = Logger.getLogger(OICDService.class);
    private static final String userInfoEndpoint = "userinfo";

    /**
     * Return the UserInformation provided by the Userinfo endpoint from the openId Provider
     * @return Userinformation as JSONObject
     */
    public static JSONObject getIdTokenFromUserInfoEndpoint(String accessToken, String userInfoEndpointUrl) {
        JSONObject idToken = new JSONObject();
        if (!userInfoEndpointUrl.isEmpty()) {
            HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
            httpHeader.setAuthorization(HttpClientAuthorization.createBearerAuthentication(accessToken));
            idToken = new HttpsClient(userInfoEndpointUrl).request(httpHeader, new HttpUrlParameterBuilder());
        }
        return idToken;
    }


    /**
     * Retrieves the Userinfo endpoint Url from the OpenId Configuration
     * @return the Url to the Userinfo endpoint
     */
    public static String getUserInfoEndPointURL(String iss){

        //return iss+ userInfoEndpoint;
        return iss+ "/protocol/openid-connect/userinfo";
    }
}
