package de.pseudonymisierung.mainzelliste.configuration.claim.oidc;

import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimProperty;
import de.pseudonymisierung.mainzelliste.configuration.claim.Claim;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator.Operator;
import java.util.List;

public class OIDCProperty extends ClaimProperty {

  public  OIDCProperty(Operator operator, List<Claim> claimList){
    super(operator, claimList);
  }

}
