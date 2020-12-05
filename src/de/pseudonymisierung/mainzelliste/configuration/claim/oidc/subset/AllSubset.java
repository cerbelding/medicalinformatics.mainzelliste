package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset;

import java.util.List;
import java.util.Set;

public class AllSubset implements Subset {

  @Override
  public boolean validate(List<String> values, Set<String> requiredValues) {
    return requiredValues.stream().filter(element -> values.contains(element)).findFirst().isPresent();
  }
}
