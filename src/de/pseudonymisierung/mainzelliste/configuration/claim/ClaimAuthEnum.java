package de.pseudonymisierung.mainzelliste.configuration.claim;

/**
 * This enum represents all authorization properties for a claim property
 */
public enum ClaimAuthEnum {
  OIDC("OIDC");

  private final String claimAuth;

  ClaimAuthEnum(String claimAuth) {
    this.claimAuth = claimAuth;
  }

  public String getClaimAuthName() {
    return this.claimAuth;
  }
}
