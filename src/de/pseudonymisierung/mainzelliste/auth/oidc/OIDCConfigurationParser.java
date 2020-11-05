package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.OperatorEnum;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.OperatorFactory;
import java.util.Map;

public class OIDCConfigurationParser {
  private static String operatorKey="operator";

  public static OIDCProperty parseConfiguration(Map<String, String> configurationProperties){
    Operator operator = new OperatorFactory().createOperator(OperatorEnum.valueOf(configurationProperties.get(operatorKey)));

    return null;

  }

}
