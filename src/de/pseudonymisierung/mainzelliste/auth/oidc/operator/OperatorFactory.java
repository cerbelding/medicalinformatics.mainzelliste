package de.pseudonymisierung.mainzelliste.auth;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.auth.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClaimPropertyFactory {
  private Map<String, String> config;

  private Map<ClaimAuth, Supplier<ClaimProperty>> factoryMap = new HashMap() {
    {
      put(ClaimAuth.OIDC,  OIDCConfigurationParser.parseConfiguration(config));
    }
  };

  public ClaimProperty createClaimProperty(ClaimAuth claimAuth, Map<String, String> config) {
    this.config = config;
    Supplier<ClaimProperty> factory = factoryMap.get(claimAuth);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}