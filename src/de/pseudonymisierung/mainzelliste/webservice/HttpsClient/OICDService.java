package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

public class OICDService {

    private static final String USERINFOENDPOINTKEY = "userinfo_endpoint";
    private static final Logger logger = Logger.getLogger(OICDService.class);
    private static final String  METADATAURL = ".well-known/openid-configuration";

    /**
     * Return the UserInformation provided by the Userinfo endpoint from the openId Provider
     * @return Userinformation as JSONObject
     */
    public static JSONObject getIdTokenFromUserInfoEndpoint(String accessToken, String userInfoEndpointUrl) throws IOException {
        JSONObject idToken = new JSONObject();
        if (!userInfoEndpointUrl.isEmpty()) {
            HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
            httpHeader.setAuthorization(HttpClientAuthorization.createBearerAuthentication(accessToken));
            idToken = new HttpsClient().request(userInfoEndpointUrl, httpHeader);
            logger.info("Userinfo: "  + idToken);

        }
        return idToken;
    }


    /**
     * Retrieves the Userinfo endpoint Url from the OpenId Configuration
     * @return the Url to the Userinfo endpoint if the attribute exist in the metadata of the authorization server, otherwise null
     */
    public static String getUserInfoEndPointURL(String iss) throws IOException {
        HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
        JSONObject metaData  = new HttpsClient().request(getSpecificUrl(iss,METADATAURL), httpHeader);
        logger.debug("Metadata Response: "  + metaData.toString());
        try{
            return metaData.getString(USERINFOENDPOINTKEY);
        }
        catch(JSONException e){
            logger.warn(e);
           return null;
        }
    }

    /**
     * Builds a valid Url path
     * @param base Base Path of the Url
     * @param endpoint Path endpoint
     * @return the full path which contains base + appendix
     */
    private static String getSpecificUrl(String base, String endpoint){
        // Removes Path component
        if(base.matches(".*/")){
            return base+endpoint;
        }else {
            return base+ "/" +endpoint;
        }
    }
}
