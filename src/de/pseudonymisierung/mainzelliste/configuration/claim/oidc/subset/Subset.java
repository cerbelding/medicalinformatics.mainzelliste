package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset;

import java.util.List;
import java.util.Set;

public interface Subset {

  public boolean validate(List<String> values, Set<String> requiredValues );
}
