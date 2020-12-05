package de.pseudonymisierung.mainzelliste.auth;

import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.Claim;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import java.util.List;

public class ClaimProperty {

  protected Operator operator;
  protected List<Claim> claimList;

  public  ClaimProperty(Operator operator, List<Claim> claimList){
    this.claimList = claimList;
    this.operator = operator;
  }

  public boolean isAuthorized(ClaimMap claims){
    return operator.validate(claimList, claims);
  }
}
