package de.pseudonymisierung.mainzelliste.configuration.claim;

import de.pseudonymisierung.mainzelliste.configuration.claim.subset.Subset;
import java.util.List;
import java.util.Set;

/**
 * Represents an specific claim in the claims configuration file
 */

public class ClaimItem {

  private final String claim;
  private final Subset subset;
  private final Set<String> requiredValues;

  public ClaimItem(String claim, Subset subset, Set<String> requiredValues) {
    this.claim = claim;
    this.subset = subset;
    this.requiredValues = requiredValues;
  }

  public String getClaim() {
    return claim;
  }

  /**
   * Validates if the clientValues are sufficient to grant access to this claim
   *
   * @param clientValues the given clientValues by the requesters credentials
   * @return true if the credentials are sufficient, otherwise false
   */
  public boolean validate(List<String> clientValues) {
    return subset.validate(clientValues, requiredValues);
  }
}
