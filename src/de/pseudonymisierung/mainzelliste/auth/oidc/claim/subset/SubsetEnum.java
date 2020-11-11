package de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset;

public enum SubsetEnum {
  ALL("ALL"),
  ANY("ANY");

  private String subset;

  SubsetEnum(String subset){this.subset = subset;}
  public String getSubsetName(){return this.subset;}
}
