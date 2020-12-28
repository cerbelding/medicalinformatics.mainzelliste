package de.pseudonymisierung.mainzelliste.auth.credentials;

import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEum;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the ApiKey Authentication
 */
public class ApiKeyCredentials implements ClientCredentials {

  private final String apiKey;

  public ApiKeyCredentials(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public String getId() {
    return this.apiKey;
  }

  @Override
  public List<String> getValuesByKey(String key) {
    List<String> valueList = new ArrayList<>();

    if(key == "apiKey"){
      valueList.add(this.apiKey);
    }
    return valueList;
  }

  @Override
  public AuthenticationEum getAuthEnum() {
    return AuthenticationEum.APIKEY;
  }
}
