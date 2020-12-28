package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationAuthEnum;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.oidcClaimConfiguration.OIDCClaimConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Creates a IAuthorization Instance given an ClaimAuthEnum instance
 */

public class AuthorizationServerFactory {

  private final Map<String, String> config;
  private final String prefix;
  private final Map<ClaimConfigurationAuthEnum, Supplier<AuthorizationServer>> factoryMap = new HashMap<>();
  private final AuthorizationServers authorizationServers;
  private final static Logger logger = LogManager.getLogger(AuthorizationServerFactory.class);


  private void initFactoryMap() {
    factoryMap.put(ClaimConfigurationAuthEnum.OIDC,
        () -> OIDCClaimConfigurationParser.getOIDCServer(this.config, this.prefix, this.authorizationServers));
  }

  public AuthorizationServerFactory(Map<String, String> config, String prefix, AuthorizationServers authorizationServers) {
    this.config = config;
    this.prefix = prefix;
    this.authorizationServers = authorizationServers;
    this.initFactoryMap();
  }

  public AuthorizationServer getAuthorizationServer(
      ClaimConfigurationAuthEnum claimConfigurationAuthEnum) {
    Supplier<AuthorizationServer> factory = factoryMap.get(claimConfigurationAuthEnum);
    if (factory == null) {
      logger.warn("Authorization Server could not be found");
      return null;
    }
    return factory.get();
  }
}