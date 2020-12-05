package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator;

import de.pseudonymisierung.mainzelliste.auth.jwt.UserInfoClaims;
import de.pseudonymisierung.mainzelliste.configuration.claim.Claim;
import java.util.List;

public interface Operator {

  public boolean validate(List<Claim> claimList, UserInfoClaims claims );


}
