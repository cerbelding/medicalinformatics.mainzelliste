package de.pseudonymisierung.mainzelliste.auth.oidc.operator;

import de.pseudonymisierung.mainzelliste.auth.authenticator.ClaimMap;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.Claim;
import java.util.List;

public interface Operator {

  public boolean validate(List<Claim> claimList, ClaimMap claims );


}
