package de.pseudonymisierung.mainzelliste;

/**
 * An Enum of the provided Authentication methods
 */
public enum AuthenticationEum {
    APIKEY("apiKey"),
    ACCESS_TOKEN("access_token");

    private String authentication;

    AuthenticationEum(String authString) {
        this.authentication = authString;
    }

    public String getAuthentication() {
        return authentication;
    }
}
