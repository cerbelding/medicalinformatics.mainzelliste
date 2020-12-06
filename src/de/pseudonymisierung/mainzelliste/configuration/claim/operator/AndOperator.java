package de.pseudonymisierung.mainzelliste.configuration.claim.operator;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimItem;
import java.util.List;

/**
 * The AndOperator Instance represents the "AND" Operator-Property. All claims must be validated
 */
public class AndOperator implements Operator {

  public AndOperator() {
  }

  @Override
  public boolean validate(List<ClaimItem> claimItemList, OIDCCredentials claims) {
    for (ClaimItem claimItem : claimItemList) {
      List<String> claimValues = claims.getValuesByKey(claimItem.getClaim());
      if (!claimItem.validate(claimValues)) {
        return false;
      }
    }
    return true;
  }
}
