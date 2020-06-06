package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import javax.ws.rs.core.HttpHeaders;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the HttpHeadersInterface
 */
public class HttpHeadersImpl implements HttpHeadersInterface {
    private final Map<String, String> headerParams = new HashMap();
    private final String requestMethod;

    public HttpHeadersImpl(String requestMethod){
        this.requestMethod = requestMethod;
    }

    public void setHeaders(Map<String, String> headers){
        headerParams.putAll(headers);
    }

    public void setAuthorization(HttpClientAuthorization auth){
        headerParams.put(HttpHeaders.AUTHORIZATION, auth.toString());
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public Map<String, String> getHeaderParams() {
        return headerParams;
    }


    @Override
    public String toString() {
        return " HTTP-Method: "+ this.requestMethod + " headers: " + headerParams.toString();
    }
}
