package de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset;

import java.util.List;

public class AllSubset implements Subset {

  @Override
  public boolean validate(List<String> values, List<String> requiredValues) {
    return requiredValues.stream().filter(element -> values.contains(element)).findFirst().isPresent();
  }
}
