package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.ClaimProperty;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.OIDCClaim;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import java.util.List;

public class OIDCProperty implements ClaimProperty {
  private Operator operator;
  private List<OIDCClaim> oidcClaimList;
  private OIDCServer server;

  public  OIDCProperty(OIDCServer server, Operator operator, List<OIDCClaim> oidcClaimList){
    this.server = server;
    this.oidcClaimList = oidcClaimList;
    this.operator = operator;
  }

}
