package de.pseudonymisierung.mainzelliste.configuration.claim.subset;

import java.util.List;
import java.util.Set;

/**
 * The AllSubset Instance represents the "ALL" Subset-Property. The Requester needs all values of
 * the subset to get access
 */

public class AllSubset implements Subset {

  /**
   * Checks if all required values are defined in the values List
   *
   * @param values         the credentials of the requester
   * @param requiredValues required values which must be included by the values
   * @return true if all required values are defined in the values List, otherwise false
   */
  @Override
  public boolean validate(List<String> values, Set<String> requiredValues) {
    return requiredValues.stream().filter(element -> values.contains(element)).count()
        == requiredValues.size();
  }
}
