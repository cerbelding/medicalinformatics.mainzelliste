package de.pseudonymisierung.mainzelliste.configuration.claim;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator.Operator;
import java.util.List;

public class ClaimProperty {

  protected Operator operator;
  protected List<Claim> claimList;

  public  ClaimProperty(Operator operator, List<Claim> claimList){
    this.claimList = claimList;
    this.operator = operator;
  }

  public boolean isAuthorized(UserInfoClaims claims){
    return operator.validate(claimList, claims);
  }
}
