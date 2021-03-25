package de.pseudonymisierung.mainzelliste.auth.credentials;

import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEum;
import java.util.List;

/**
 * Represents the Client-Credentials (Api-Key, OIDC-Credentials,..)
 */
public interface ClientCredentials {

  String getId();
  List<String> getValuesByKey(String key);
  AuthenticationEum getAuthEnum();

}
