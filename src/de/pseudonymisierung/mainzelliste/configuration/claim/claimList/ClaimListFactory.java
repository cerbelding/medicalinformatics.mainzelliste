package de.pseudonymisierung.mainzelliste.configuration.claim.claimList;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimAuthEnum;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory to generate a ClaimList
 */
public class ClaimListFactory {

  private final Map<String, String> config;
  private final String prefix;
  private final Map<ClaimAuthEnum, Supplier<ClaimList>> factoryMap = new HashMap<>();


  private void initFactoryMap() {
    factoryMap.put(ClaimAuthEnum.OIDC,
        () -> OIDCConfigurationParser.parseConfiguration(this.config, this.prefix));
  }

  public ClaimListFactory(Map<String, String> config, String prefix) {
    this.config = config;
    this.prefix = prefix;
    this.initFactoryMap();

  }

  public ClaimList createClaimProperty(ClaimAuthEnum claimAuthEnum) {
    Supplier<ClaimList> factory = factoryMap.get(claimAuthEnum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}