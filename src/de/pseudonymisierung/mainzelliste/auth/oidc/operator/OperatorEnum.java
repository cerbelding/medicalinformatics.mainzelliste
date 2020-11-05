package de.pseudonymisierung.mainzelliste.auth.oidc.operator;

public enum  OperatorEnum {
  AND("AND"),
  OR("OR");

  private String operator;

  OperatorEnum(String operator){this.operator = operator;}
  public String getClaimAuthName(){return this.operator;}
}
