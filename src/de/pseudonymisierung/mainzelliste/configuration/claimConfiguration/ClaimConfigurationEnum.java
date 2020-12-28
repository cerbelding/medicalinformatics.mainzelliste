package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration;

/**
 * Enum to store all relevant Claim properties.
 */
public enum ClaimConfigurationEnum {
  PERMISSIONS("permissions"),
  AUTH("auth");

  private final String claimProperty;

  ClaimConfigurationEnum(String claimProperty) {
    this.claimProperty = claimProperty;
  }

  public String getClaimName() {
    return this.claimProperty;
  }
}
