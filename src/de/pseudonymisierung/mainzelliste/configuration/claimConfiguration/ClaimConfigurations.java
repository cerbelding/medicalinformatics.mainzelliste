package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration;

import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationFactory;
import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;
import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList.ClaimConfigurationItemList;
import de.pseudonymisierung.mainzelliste.requester.Client;
import de.pseudonymisierung.mainzelliste.requester.Requester;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Stores a Set of Claim configurations
 */
public class ClaimConfigurations {

  private  final Logger  logger = Logger.getLogger(ClaimConfigurations.class);
  private final Set<ClaimConfiguration> claimConfigurationSet;

  public ClaimConfigurations(Set<ClaimConfiguration> claimConfigurationSet) {
    this.claimConfigurationSet = claimConfigurationSet;
  }

  /**
   * Returns all ClaimConfigurations which has the given authServer
   *
   * @param authorizationServerCredentials the given authServer
   * @return A filtered ClaimConfiguration Set
   */

  public Set<ClaimConfiguration> getClaimConfigurationsByAuthServer(AuthorizationServerCredentials authorizationServerCredentials) {
    Set<ClaimConfiguration> configurations = new HashSet<>();

    for (ClaimConfiguration configuration : this.claimConfigurationSet) {
      if (configuration.getAuthorizator().authorize(authorizationServerCredentials)) {
        configurations.add(configuration);
      }
    }
    return configurations;
  }


  /**
   * Creates on runtime a    * @param claims The claims to authenticate the requester by its given
   * credentials. (At the moment its only possible to create an OIDC-Requester)
   *
   * @param claims The claims to authenticate the requester
   * @return the created requester if it could been authenticated, otherwise null
   */

  public Requester createRequester(ClientCredentials claims, AuthorizationServerCredentials authorizationServerCredentials) {
    Set<ClaimConfiguration> claimConfigurations = getClaimConfigurationsByAuthServer(authorizationServerCredentials);

    AuthorizationServer authorizationServer = null;
    Set<String> permissions = new HashSet<>();
    Set<ClaimConfigurationItemList> claimProperties = new HashSet<>();

    for (ClaimConfiguration claimConfiguration : claimConfigurations) {
      if (claimConfiguration.isAuthorized(claims, authorizationServerCredentials)) {
        if (authorizationServer == null) {
          authorizationServer = claimConfiguration.getAuthorizator();
        }
        permissions.addAll(claimConfiguration.getPermissions());
        claimProperties.add(claimConfiguration.getClaimProperty());
      }
    }

    Authenticator authenticator = new AuthenticationFactory(claims).getAuthenticator();
    if (authorizationServer != null && !permissions.isEmpty() && !claimProperties
        .isEmpty()) {
      return new Client(permissions, authenticator);
    } else {
      logger.info("Requester could not been authenticated");
      return null;
    }
  }


}


