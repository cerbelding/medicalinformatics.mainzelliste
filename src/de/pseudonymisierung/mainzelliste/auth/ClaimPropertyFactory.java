package de.pseudonymisierung.mainzelliste.auth;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.auth.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClaimPropertyFactory {
  private Map<String, String> config;
  private String prefix;
  private  Map<ClaimAuthEnum, Supplier<ClaimProperty>> factoryMap = new HashMap();


  private void initFactoryMap(){
    factoryMap.put(ClaimAuthEnum.OIDC,  () -> OIDCConfigurationParser.parseConfiguration(this.config, this.prefix));
  }

  public ClaimPropertyFactory(Map<String, String> config, String prefix){
    this.config = config;
    this.prefix = prefix;
    this.initFactoryMap();

  }

  public ClaimProperty createClaimProperty(ClaimAuthEnum claimAuthEnum) {
    Supplier<ClaimProperty> factory = factoryMap.get(claimAuthEnum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}