package de.pseudonymisierung.mainzelliste.configuration.claim.claimList;

import de.pseudonymisierung.mainzelliste.auth.credentials.OIDCCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimItem;
import de.pseudonymisierung.mainzelliste.configuration.claim.operator.Operator;
import java.util.List;

/**
 * Represents a List of all ClaimItems given in an Claim Configuration
 */

public class ClaimList {

  protected Operator operator;
  protected List<ClaimItem> claimItemList;

  public ClaimList(Operator operator, List<ClaimItem> claimItemList) {
    this.claimItemList = claimItemList;
    this.operator = operator;
  }

  /**
   * Checks if  credentials of the requester are sufficient to get access to this resource
   *
   * @param claims credentials of the requester
   * @return true if the credentials are sufficient, otherwise false
   */

  public boolean isAuthorized(OIDCCredentials claims) {
    return operator.validate(claimItemList, claims);
  }
}
