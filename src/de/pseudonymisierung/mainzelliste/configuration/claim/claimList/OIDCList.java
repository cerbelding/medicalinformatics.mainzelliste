package de.pseudonymisierung.mainzelliste.configuration.claim.claimList;

import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimItem;
import de.pseudonymisierung.mainzelliste.configuration.claim.operator.Operator;
import java.util.List;

public class OIDCList extends ClaimList {

  public OIDCList(Operator operator, List<ClaimItem> claimItemList) {
    super(operator, claimItemList);
  }

}
