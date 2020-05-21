package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import org.apache.commons.net.util.Base64;
import java.nio.charset.StandardCharsets;

public class HttpClientAuthorization {
    private final String authorization;


    public HttpClientAuthorization(String authorization){
        this.authorization = authorization;

    }

    @Override
    public String toString() {
        return  this.authorization;
    }

    public static HttpClientAuthorization createBearerAuthentication(String bearerToken){
        return new HttpClientAuthorization("Bearer " + bearerToken);

    }
    public static HttpClientAuthorization createBasicAuthentication(String user, String password){
        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        return new HttpClientAuthorization("Basic " + new String(encodedAuth));
    }
}
