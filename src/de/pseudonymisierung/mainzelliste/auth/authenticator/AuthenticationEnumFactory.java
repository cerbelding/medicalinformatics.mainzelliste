package de.pseudonymisierung.mainzelliste.auth.authenticator;

import com.sun.jersey.api.NotFoundException;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimAuthEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AuthenticationEnumFactory {
  private  Map<AuthenticationEum, Supplier<ClaimAuthEnum>> factoryMap = new HashMap();


  private void initFactoryMap(){
    factoryMap.put(AuthenticationEum.ACCESS_TOKEN,  () -> ClaimAuthEnum.OIDC);
  }

  public AuthenticationEnumFactory(){
    this.initFactoryMap();

  }

  public ClaimAuthEnum getClaimAuthEnumByAuthenticationEnum(AuthenticationEum authenticationEum) {
    Supplier<ClaimAuthEnum> factory = factoryMap.get(authenticationEum);
    if (factory == null) {
      throw new NotFoundException("Could not parse ClaimProperty");
    }
    return factory.get();
  }
}