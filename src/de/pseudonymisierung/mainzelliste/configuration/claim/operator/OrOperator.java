package de.pseudonymisierung.mainzelliste.configuration.claim.operator;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimItem;
import java.util.List;

/**
 * The OrOperator Instance represents the "Or" Operator-Property. One claim must be validated to
 * grant access
 */
public class OrOperator implements Operator {

  public OrOperator() {
  }

  public boolean validate(List<ClaimItem> claimItemList, OIDCCredentials claims) {
    for (ClaimItem claimItem : claimItemList) {
      List<String> claimValues = claims.getValuesByKey(claimItem.getClaim());
      if (claimItem.validate(claimValues)) {
        return true;
      }
    }
    return false;
  }
}
