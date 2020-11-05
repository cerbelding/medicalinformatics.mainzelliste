package de.pseudonymisierung.mainzelliste.auth;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.auth.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClaimPropertyFactory {
  private Map<String, String> config;

  private Map<ClaimAuthEnum, Supplier<ClaimProperty>> factoryMap = new HashMap() {
    {
      put(ClaimAuthEnum.OIDC,  OIDCConfigurationParser.parseConfiguration(config));
    }
  };

  public ClaimProperty createClaimProperty(ClaimAuthEnum claimAuthEnum, Map<String, String> config) {
    this.config = config;
    Supplier<ClaimProperty> factory = factoryMap.get(claimAuthEnum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}