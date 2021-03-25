package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration;


import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;
import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList.ClaimConfigurationItemList;
import java.util.Set;

/**
 * Represents an specific claim instance given in the configuration file
 */

public class ClaimConfiguration {

  private final Set<String> permissions;
  private final ClaimConfigurationAuthEnum claimConfigurationAuthEnum;
  private final ClaimConfigurationItemList claimConfigurationItemList;
  private final AuthorizationServer authorization;


  public ClaimConfiguration(Set<String> permissions, ClaimConfigurationAuthEnum claimConfigurationAuthEnum,
      ClaimConfigurationItemList claimConfigurationItemList, AuthorizationServer authorization) {
    this.permissions = permissions;
    this.claimConfigurationAuthEnum = claimConfigurationAuthEnum;
    this.claimConfigurationItemList = claimConfigurationItemList;
    this.authorization = authorization;
  }

  public ClaimConfigurationItemList getClaimProperty() {
    return claimConfigurationItemList;
  }

  public AuthorizationServer getAuthorizator() {
    return this.authorization;
  }

  /**
   * Checks  if the credentials of the User are sufficient to grant the permissions of this claim
   * configuration
   *
   * @param claims the credentials of the requester
   * @return true if the requester is could be authorized to the resource otherwise false
   */
  public boolean isAuthorized(ClientCredentials claims, AuthorizationServerCredentials authorizationServer) {
    return authorization.authorize(authorizationServer) && claimConfigurationItemList.isAuthorized(claims);
  }

  public Set<String> getPermissions() {
    return permissions;
  }
}
