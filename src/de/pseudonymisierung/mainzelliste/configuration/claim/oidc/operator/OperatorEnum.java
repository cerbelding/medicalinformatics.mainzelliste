package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator;

public enum  OperatorEnum {
  AND("AND"),
  OR("OR");

  private String operator;

  OperatorEnum(String operator){this.operator = operator;}
  public String getClaimAuthName(){return this.operator;}
}
