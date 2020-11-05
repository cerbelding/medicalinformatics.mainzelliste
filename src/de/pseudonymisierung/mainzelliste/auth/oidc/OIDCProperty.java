package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.ClaimProperty;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.OIDCClaim;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import java.util.List;

public class OIDCProperty implements ClaimProperty {
  private Operator operator;
  private List<OIDCClaim> oidcClaimList;

  public  OIDCProperty(Operator operator, List<OIDCClaim> oidcClaimList){
    this.oidcClaimList = oidcClaimList;
    this.operator = operator;
  }

}
