package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList;

import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator.Operator;
import java.util.List;

public class OIDCClaimConfigurationItemList extends ClaimConfigurationItemList {

  public OIDCClaimConfigurationItemList(Operator operator, List<ClaimConfigurationItem> claimConfigurationItemList) {
    super(operator, claimConfigurationItemList);
  }

}
