package de.pseudonymisierung.mainzelliste.auth.credentials;

/**
 * Represents the ApiKey Authentication
 */
public class ApiKeyCredentials implements Credentials {

  private final String apiKey;

  public ApiKeyCredentials(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public String getId() {
    return this.apiKey;
  }
}
