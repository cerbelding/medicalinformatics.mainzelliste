package de.pseudonymisierung.mainzelliste.auth.oidc.operator;

import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.Claim;
import java.util.List;

public class OrOperator implements Operator {

  public OrOperator(){
  }

  public boolean validate(List<Claim> claimList, ClaimMap claims) {
    for(Claim claim: claimList){
      List<String> claimValues =  claims.getValuesByKey(claim.getClaim());
      if(claim.validate(claimValues)){
        return true;
      }
    }
    return false;
  }
}
