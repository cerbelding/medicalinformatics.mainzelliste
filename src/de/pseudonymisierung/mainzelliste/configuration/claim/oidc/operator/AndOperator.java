package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;
import de.pseudonymisierung.mainzelliste.configuration.claim.Claim;
import java.util.List;

public class AndOperator implements    Operator {

  public AndOperator(){ }

  @Override
  public boolean validate(List<Claim> claimList, UserInfoClaims claims) {
    for(Claim claim: claimList){
     List<String> claimValues =  claims.getValuesByKey(claim.getClaim());
     if(!claim.validate(claimValues)){
       return false;
     }
    }
    return true;
  }
}
