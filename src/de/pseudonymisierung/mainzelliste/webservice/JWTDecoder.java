package de.pseudonymisierung.mainzelliste.webservice;



import de.pseudonymisierung.mainzelliste.webservice.Authenticator.OICDAuthenticator;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public class JWTDecoder {
    private static final Logger logger = Logger.getLogger(JWTDecoder.class);

    // Decode JWT encoded token
    public static JSONObject decode(String jwtToken) throws IOException, JSONException {


        String[] split_string = jwtToken.split("\\.");
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();

        if(split_string.length != 3){
            throw new IOException();
        }

        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];
        String header = new String(decoder.decode(base64EncodedHeader));
        String body = new String(decoder.decode(base64EncodedBody));

        // parse payload to JSONObject
        JSONObject payLoad = new JSONObject(body);;
        logger.debug("Decoded AccessToken: "+payLoad);
        return payLoad;




    }
}
