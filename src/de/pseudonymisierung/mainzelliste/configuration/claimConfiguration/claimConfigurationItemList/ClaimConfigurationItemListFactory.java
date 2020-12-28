package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServers;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationAuthEnum;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.oidcClaimConfiguration.OIDCClaimConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory to generate a ClaimList
 */
public class ClaimConfigurationItemListFactory {

  private final Map<String, String> config;
  private final String prefix;
  private final Map<ClaimConfigurationAuthEnum, Supplier<ClaimConfigurationItemList>> factoryMap = new HashMap<>();
  private final AuthorizationServers authorizationServers;


  private void initFactoryMap() {
    factoryMap.put(ClaimConfigurationAuthEnum.OIDC,
        () -> OIDCClaimConfigurationParser.parseConfiguration(this.config, this.prefix, authorizationServers));
  }

  public ClaimConfigurationItemListFactory(Map<String, String> config, String prefix, AuthorizationServers authorizationServer) {
    this.config = config;
    this.prefix = prefix;
    this.authorizationServers = authorizationServer;
    this.initFactoryMap();

  }

  public ClaimConfigurationItemList createClaimProperty(ClaimConfigurationAuthEnum claimConfigurationAuthEnum) {
    Supplier<ClaimConfigurationItemList> factory = factoryMap.get(claimConfigurationAuthEnum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}