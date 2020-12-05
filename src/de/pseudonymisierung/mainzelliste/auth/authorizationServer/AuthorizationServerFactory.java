package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimAuthEnum;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.OIDCConfigurationParser;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AuthorizationServerFactory {
  private Map<String, String> config;
  private String prefix;
  private  Map<ClaimAuthEnum, Supplier<IAuthorization>> factoryMap = new HashMap();


  private void initFactoryMap(){
    factoryMap.put(ClaimAuthEnum.OIDC,  () -> OIDCConfigurationParser.getOIDCServer(this.config, this.prefix));
  }

  public AuthorizationServerFactory(Map<String, String> config, String prefix){
    this.config = config;
    this.prefix = prefix;
    this.initFactoryMap();
  }

  public IAuthorization getAuthorizationServer(ClaimAuthEnum claimAuthEnum) {
    Supplier<IAuthorization> factory = factoryMap.get(claimAuthEnum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}