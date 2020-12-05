package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.auth.ClaimProperty;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.Claim;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import java.util.List;

public class OIDCProperty extends ClaimProperty {

  public  OIDCProperty(Operator operator, List<Claim> claimList){
    super(operator, claimList);
  }

}
