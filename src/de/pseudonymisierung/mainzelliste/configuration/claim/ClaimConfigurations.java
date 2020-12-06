package de.pseudonymisierung.mainzelliste.configuration.claim;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.auth.authenticator.OIDCAuthenticator;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.IAuthorization;
import de.pseudonymisierung.mainzelliste.configuration.claim.claimList.ClaimList;
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
   * @param authServerId the given authServer-ID
   * @return A filtered ClaimConfiguration Set
   */

  public Set<ClaimConfiguration> getClaimConfigurationsByAuthServer(String authServerId) {
    Set<ClaimConfiguration> configurations = new HashSet<>();

    for (ClaimConfiguration configuration : this.claimConfigurationSet) {
      if (configuration.getAuthorizator().getId().equals(authServerId)) {
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

  public Requester createRequester(OIDCCredentials claims) {
    String authId = Config.instance.getOidcServers().getIdByIssuer(claims.getIss());
    Set<ClaimConfiguration> claimConfigurations = getClaimConfigurationsByAuthServer(authId);

    IAuthorization oidcServer = null;
    Set<String> permissions = new HashSet<>();
    Set<ClaimList> claimProperties = new HashSet<>();

    for (ClaimConfiguration claimConfiguration : claimConfigurations) {
      if (claimConfiguration.isAuthorized(claims)) {
        if (oidcServer == null) {
          oidcServer = claimConfiguration.getAuthorizator();
        }
        permissions.addAll(claimConfiguration.getPermissions());
        claimProperties.add(claimConfiguration.getClaimProperty());
      }
    }

    Authenticator authenticator = new OIDCAuthenticator(claims.getId(), claimProperties, oidcServer);
    if (oidcServer != null && !permissions.isEmpty() && !claimProperties
        .isEmpty()) {
      return new Client(permissions, authenticator);
    } else {
      logger.info("Requester could not been authenticated");
      return null;
    }
  }


}


