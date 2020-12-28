package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator;

/**
 * Enum which stores all possible Operator values
 */
public enum OperatorEnum {
  AND("AND"),
  OR("OR");

  private final String operator;

  OperatorEnum(String operator) {
    this.operator = operator;
  }

  public String getClaimAuthName() {
    return this.operator;
  }
}
