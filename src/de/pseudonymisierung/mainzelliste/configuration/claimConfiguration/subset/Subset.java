package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.subset;

import java.util.List;
import java.util.Set;

/**
 * Defines an Subset type of an claimItem
 */

public interface Subset {

  public boolean validate(List<String> values, Set<String> requiredValues);
}
