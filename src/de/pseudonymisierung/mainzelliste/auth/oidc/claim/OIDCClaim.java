package de.pseudonymisierung.mainzelliste.auth.oidc.claim;

import de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset.Subset;
import java.util.List;
import java.util.Set;

public class OIDCClaim {
  private String claim;
  private Subset subset;
  private Set<String> requiredValues;

  public OIDCClaim(String claim, Subset subset, Set<String> requiredValues){
    this.claim = claim;
    this.subset=subset;
    this.requiredValues=requiredValues;
  }

  public String getClaim() {
    return claim;
  }
  public boolean validate(List<String> clientValues){
    return subset.validate(clientValues,requiredValues);
  }
}
