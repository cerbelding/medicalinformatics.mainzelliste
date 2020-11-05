package de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset;

import java.util.List;

public interface Subset {

  public boolean validate(List<String> values, List<String> requiredValues );
}
