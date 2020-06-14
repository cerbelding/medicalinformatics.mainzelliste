package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;


public interface HttpsClientInterface<Header extends HttpHeadersInterface, URLParam extends HttpUrlParametersInterface, ResponseType extends JSONObject > {


    public  ResponseType  request(String url, Header header, URLParam urlParam) throws IOException;


}
