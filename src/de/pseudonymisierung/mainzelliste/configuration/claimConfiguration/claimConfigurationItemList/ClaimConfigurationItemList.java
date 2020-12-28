package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator.Operator;
import java.util.List;

/**
 * Represents a List of all ClaimItems given in an Claim Configuration
 */

public class ClaimConfigurationItemList {

  protected Operator operator;
  protected List<ClaimConfigurationItem> claimConfigurationItemList;

  public ClaimConfigurationItemList(Operator operator, List<ClaimConfigurationItem> claimConfigurationItemList) {
    this.claimConfigurationItemList = claimConfigurationItemList;
    this.operator = operator;
  }

  /**
   * Checks if  credentials of the requester are sufficient to get access to this resource
   *
   * @param claims credentials of the requester
   * @return true if the credentials are sufficient, otherwise false
   */

  public boolean isAuthorized(ClientCredentials claims) {
    return operator.validate(claimConfigurationItemList, claims);
  }
}
