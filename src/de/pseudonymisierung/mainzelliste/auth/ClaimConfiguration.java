package de.pseudonymisierung.mainzelliste.auth;


import java.util.Set;

public class ClaimConfiguration  {
  private Set<String> permissions;
  private ClaimAuthEnum claimAuthEnum;
  private ClaimProperty claimProperty;

  public ClaimConfiguration(Set<String> permissions, ClaimAuthEnum claimAuthEnum, ClaimProperty claimProperty){
    this.permissions = permissions;
    this.claimAuthEnum = claimAuthEnum;
    this.claimProperty = claimProperty;
  }

}
