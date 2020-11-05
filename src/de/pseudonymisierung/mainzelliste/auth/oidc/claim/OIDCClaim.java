package de.pseudonymisierung.mainzelliste.auth.oidc.claim;

import de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset.Subset;
import java.util.List;

public class OICDClaim {
  private String claim;
  private Subset subset;
  private List<String> requiredValues;

  public OICDClaim(String claim, Subset subset, List<String> requiredValues){
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
