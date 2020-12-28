package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import java.util.List;

/**
 * The OrOperator Instance represents the "Or" Operator-Property. One claim must be validated to
 * grant access
 */
public class OrOperator implements Operator {

  public OrOperator() {
  }

  public boolean validate(List<ClaimConfigurationItem> claimConfigurationItemList, ClientCredentials claims) {
    for (ClaimConfigurationItem claimConfigurationItem : claimConfigurationItemList) {
      List<String> claimValues = claims.getValuesByKey(claimConfigurationItem.getClaim());
      if (claimConfigurationItem.validate(claimValues)) {
        return true;
      }
    }
    return false;
  }
}
