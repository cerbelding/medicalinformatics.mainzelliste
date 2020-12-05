package de.pseudonymisierung.mainzelliste.auth;


import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import java.util.Set;

public class ClaimConfiguration  {
  private Set<String> permissions;
  private ClaimAuthEnum claimAuthEnum;
  private ClaimProperty claimProperty;
  private IAuthorization authorization;


  public ClaimConfiguration(Set<String> permissions, ClaimAuthEnum claimAuthEnum, ClaimProperty claimProperty, IAuthorization authorization){
    this.permissions = permissions;
    this.claimAuthEnum = claimAuthEnum;
    this.claimProperty = claimProperty;
    this.authorization = authorization;
  }

  public ClaimProperty getClaimProperty() {
    return claimProperty;
  }

  public IAuthorization getAuthorizator(){
    return this.authorization;
  }

  public ClaimAuthEnum getClaimAuthEnum() {
    return claimAuthEnum;
  }

  public boolean isAuthorized(ClaimMap claims){
    return authorization.authorize(claims) && claimProperty.isAuthorized(claims);
  }

  public Set<String> getPermissions() {
    return permissions;
  }
}
