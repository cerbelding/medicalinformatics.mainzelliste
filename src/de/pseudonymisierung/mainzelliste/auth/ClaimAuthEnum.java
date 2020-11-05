package de.pseudonymisierung.mainzelliste.auth;

public enum ClaimAuthEnum {
  OIDC("OIDC");

  private String claimAuth;

  ClaimAuthEnum(String claimAuth){this.claimAuth = claimAuth;}
  public String getClaimAuthName(){return this.claimAuth;}
}
