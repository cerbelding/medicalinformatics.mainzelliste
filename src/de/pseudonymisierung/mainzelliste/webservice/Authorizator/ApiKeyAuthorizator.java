package de.pseudonymisierung.mainzelliste.webservice.Authorizator;

import java.util.Map;

public class ApiKeyAuthorizator implements Authorizator {

    private String apiKey;

    public ApiKeyAuthorizator (String apiKey){
        this.apiKey = apiKey;
    }


    @Override
    public boolean hasPermission(Map<String, String> tokens) {
        String apiKey = tokens.get("apiKey");
        return this.apiKey == apiKey;
    }
}
