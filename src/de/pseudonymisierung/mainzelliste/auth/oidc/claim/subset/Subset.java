package de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset;

import java.util.List;
import java.util.Set;

public interface Subset {

  public boolean validate(List<String> values, Set<String> requiredValues );
}
