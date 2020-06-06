package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpUrlParameterBuilder implements HttpUrlParametersInterface {
        private final Map<String, String>  urlParams;

        public HttpUrlParameterBuilder(){
            urlParams = new HashMap<>();

        }
        public HttpUrlParameterBuilder(Map<String, String> urlParams){
            this.urlParams = urlParams;
        }



    public String getParamsString()  {
        StringBuilder result = new StringBuilder();

        try{
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
                }
        }
        catch(UnsupportedEncodingException e){
            return "";

        }
        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }
}
