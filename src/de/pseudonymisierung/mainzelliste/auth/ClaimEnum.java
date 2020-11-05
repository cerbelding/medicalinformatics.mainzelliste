package de.pseudonymisierung.mainzelliste.auth;

public enum ClaimEnum {
  PERMISSIONS("permissions"),
  AUTH("auth");

  private String claimProperty;
  ClaimEnum(String claimProperty){this.claimProperty = claimProperty;}
  public String getClaimName(){return this.claimProperty;}
}
