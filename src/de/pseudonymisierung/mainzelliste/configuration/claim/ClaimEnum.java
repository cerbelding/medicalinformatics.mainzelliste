package de.pseudonymisierung.mainzelliste.configuration.claim;

public enum ClaimEnum {
  PERMISSIONS("permissions"),
  AUTH("auth");

  private String claimProperty;
  ClaimEnum(String claimProperty){this.claimProperty = claimProperty;}
  public String getClaimName(){return this.claimProperty;}
}
