package de.pseudonymisierung.mainzelliste.configuration.claim;


import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.IAuthorization;
import de.pseudonymisierung.mainzelliste.configuration.claim.claimList.ClaimList;
import java.util.Set;

/**
 * Represents an specific claim instance given in the configuration file
 */

public class ClaimConfiguration {

  private final Set<String> permissions;
  private final ClaimAuthEnum claimAuthEnum;
  private final ClaimList claimList;
  private final IAuthorization authorization;


  public ClaimConfiguration(Set<String> permissions, ClaimAuthEnum claimAuthEnum,
      ClaimList claimList, IAuthorization authorization) {
    this.permissions = permissions;
    this.claimAuthEnum = claimAuthEnum;
    this.claimList = claimList;
    this.authorization = authorization;
  }

  public ClaimList getClaimProperty() {
    return claimList;
  }

  public IAuthorization getAuthorizator() {
    return this.authorization;
  }

  /**
   * Checks  if the credentials of the User are sufficient to grant the permissions of this claim
   * configuration
   *
   * @param claims the credentials of the requester
   * @return true if the requester is could be authorized to the resource otherwise false
   */
  public boolean isAuthorized(OIDCCredentials claims) {
    return authorization.authorize(claims) && claimList.isAuthorized(claims);
  }

  public Set<String> getPermissions() {
    return permissions;
  }
}
