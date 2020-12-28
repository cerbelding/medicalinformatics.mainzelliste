package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration;

/**
 * This enum represents all authorization properties for a claim property
 */
public enum ClaimConfigurationAuthEnum {
  OIDC("OIDC");

  private final String claimAuth;

  ClaimConfigurationAuthEnum(String claimAuth) {
    this.claimAuth = claimAuth;
  }

  public String getClaimAuthName() {
    return this.claimAuth;
  }
}
