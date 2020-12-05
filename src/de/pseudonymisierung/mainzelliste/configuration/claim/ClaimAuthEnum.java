package de.pseudonymisierung.mainzelliste.configuration.claim;

public enum ClaimAuthEnum {
  OIDC("OIDC");

  private String claimAuth;

  ClaimAuthEnum(String claimAuth){this.claimAuth = claimAuth;}
  public String getClaimAuthName(){return this.claimAuth;}
}
