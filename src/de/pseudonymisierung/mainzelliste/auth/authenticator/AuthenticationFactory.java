package de.pseudonymisierung.mainzelliste.auth.authenticator;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Creates a Authenticator Instance given an ClaimAuthEnum instance
 */

public class AuthenticationFactory {

  private final ClientCredentials clientCredentials;
  private final Map<AuthenticationEum, Supplier<Authenticator>> factoryMap = new HashMap<>();
  private final static Logger logger = LogManager.getLogger(AuthenticationFactory.class);


  private void initFactoryMap() {
    factoryMap.put(AuthenticationEum.ACCESS_TOKEN,
        () -> new OIDCAuthenticator(clientCredentials.getId()));
  }

  public AuthenticationFactory(ClientCredentials clientCredentials) {
    this.clientCredentials = clientCredentials;
    this.initFactoryMap();
  }

  public Authenticator getAuthenticator() {
    Supplier<Authenticator> factory = factoryMap.get(clientCredentials.getAuthEnum());
    if (factory == null) {
      logger.warn("Authenticator could not be found");
      return null;
    }
    return factory.get();
  }
}