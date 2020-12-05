package de.pseudonymisierung.mainzelliste.auth;

import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEnumFactory;
import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEum;
import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import de.pseudonymisierung.mainzelliste.auth.authenticator.OIDCAuthenticator;
import de.pseudonymisierung.mainzelliste.requester.Client;
import de.pseudonymisierung.mainzelliste.requester.Requester;
import java.util.HashSet;
import java.util.Set;

public class ClaimConfigurationSet {
  private Set<ClaimConfiguration> claimConfigurationSet;

  public ClaimConfigurationSet(Set<ClaimConfiguration> claimConfigurationSet){
    this.claimConfigurationSet = claimConfigurationSet;
  }

  public Set<ClaimConfiguration> getClaimConfigurationsByAuthServer(String authServerId){
    Set<ClaimConfiguration> configurations = new HashSet<>();

    for(ClaimConfiguration configuration: this.claimConfigurationSet){
      if(configuration.getAuthorizator().getId().equals(authServerId)){
        configurations.add(configuration);
      }
    }
    return configurations;
  }

  private Set<ClaimConfiguration> getClaimConfigurationsByAuthType(ClaimAuthEnum claimAuthEnum){
    Set<ClaimConfiguration> claimFongiruations = new HashSet<>();

    for(ClaimConfiguration claimConfiguration: claimConfigurationSet){
      if(claimConfiguration.getClaimAuthEnum().equals(claimAuthEnum)){
        claimFongiruations.add(claimConfiguration);
      }
    }
    return claimFongiruations;
  }


  public Requester createRequester(ClaimMap claims, AuthenticationEum authenticationEum){
    ClaimAuthEnum claimAuthEnum = new AuthenticationEnumFactory().getClaimAuthEnumByAuthenticationEnum(authenticationEum);
    Set<ClaimConfiguration> claimConfigurations = getClaimConfigurationsByAuthType(claimAuthEnum);

    Authenticator authenticator  = null;
    IAuthorization oidcServer = null;
    Set<String> permissions = new HashSet<>();
    Set<ClaimProperty> claimProperties = new HashSet<>();


    for(ClaimConfiguration claimConfiguration: claimConfigurations){
      if(claimConfiguration.isAuthorized(claims)){
        if(oidcServer == null){
          oidcServer= claimConfiguration.getAuthorizator();
        }
        permissions.addAll(claimConfiguration.getPermissions());
        claimProperties.add(claimConfiguration.getClaimProperty());
      }
    }

    authenticator = new OIDCAuthenticator(claims.get("sub"), claimProperties, oidcServer);

    Client client = new Client(permissions, authenticator);
    return client;
  }


}


