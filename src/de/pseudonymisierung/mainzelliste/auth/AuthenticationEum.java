package de.pseudonymisierung.mainzelliste.auth;

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

    public String getAuthenticationName() {
        return this.authentication;
    }
}
