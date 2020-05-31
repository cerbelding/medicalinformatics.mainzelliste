package de.pseudonymisierung.mainzelliste;

public enum AuthenticationEum {
    APIKEY("apiKey"),
    ACCESS_TOKEN("access_token");

    private String authentication;

    AuthenticationEum(String authString) {
        this.authentication = authString;
    }

    public String getUrl() {
        return authentication;
    }
}
