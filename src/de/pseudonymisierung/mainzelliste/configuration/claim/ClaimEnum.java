package de.pseudonymisierung.mainzelliste.configuration.claim;

/**
 * Enum to store all relevant Claim properties.
 */
public enum ClaimEnum {
  PERMISSIONS("permissions"),
  AUTH("auth");

  private final String claimProperty;

  ClaimEnum(String claimProperty) {
    this.claimProperty = claimProperty;
  }

  public String getClaimName() {
    return this.claimProperty;
  }
}
