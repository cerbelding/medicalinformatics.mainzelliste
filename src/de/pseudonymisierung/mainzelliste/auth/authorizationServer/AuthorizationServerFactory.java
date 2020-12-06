package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimAuthEnum;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.log4j.Logger;

/**
 * Creates a IAuthorization Instance given an ClaimAuthEnum instance
 */

public class AuthorizationServerFactory {

  private final Map<String, String> config;
  private final String prefix;
  private final Map<ClaimAuthEnum, Supplier<IAuthorization>> factoryMap = new HashMap<>();
  private final static Logger logger = Logger.getLogger(AuthorizationServerFactory.class);


  private void initFactoryMap() {
    factoryMap.put(ClaimAuthEnum.OIDC,
        () -> OIDCConfigurationParser.getOIDCServer(this.config, this.prefix));
  }

  public AuthorizationServerFactory(Map<String, String> config, String prefix) {
    this.config = config;
    this.prefix = prefix;
    this.initFactoryMap();
  }

  public IAuthorization getAuthorizationServer(ClaimAuthEnum claimAuthEnum) {
    Supplier<IAuthorization> factory = factoryMap.get(claimAuthEnum);
    if (factory == null) {
      logger.warn("Authorization Server could not be found");
      return null;
    }
    return factory.get();
  }
}