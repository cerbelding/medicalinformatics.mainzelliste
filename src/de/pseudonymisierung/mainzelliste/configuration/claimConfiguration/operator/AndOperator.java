package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import java.util.List;

/**
 * The AndOperator Instance represents the "AND" Operator-Property. All claims must be validated
 */
public class AndOperator implements Operator {

  public AndOperator() {
  }

  @Override
  public boolean validate(List<ClaimConfigurationItem> claimConfigurationItemList, ClientCredentials claims) {
    for (ClaimConfigurationItem claimConfigurationItem : claimConfigurationItemList) {
      List<String> claimValues = claims.getValuesByKey(claimConfigurationItem.getClaim());
      if (!claimConfigurationItem.validate(claimValues)) {
        return false;
      }
    }
    return true;
  }
}
