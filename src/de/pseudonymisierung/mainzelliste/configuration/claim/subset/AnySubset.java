package de.pseudonymisierung.mainzelliste.configuration.claim.subset;

import java.util.List;
import java.util.Set;

/**
 * The AnySubset Instance represents the "ANY" Subset-Property. The Requester needs any value of the
 * subset to get access
 */
public class AnySubset implements Subset {

  /**
   * Checks if any required values are defined in the values List
   *
   * @param values         the credentials of the requester
   * @param requiredValues required values which any value must be included by the values
   * @return true if any required values are defined in the values List, otherwise false
   */
  @Override
  public boolean validate(List<String> values, Set<String> requiredValues) {
    return requiredValues.stream().filter(element -> values.contains(element)).findFirst()
        .isPresent();
  }
}
