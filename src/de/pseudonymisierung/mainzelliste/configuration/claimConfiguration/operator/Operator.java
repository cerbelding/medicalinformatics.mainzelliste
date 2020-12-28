package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import java.util.List;

/**
 * Represents the operator property of the claim property in the configuration file.
 */
public interface Operator {

  /**
   * The validation Operation to check if the requester get access to the resource
   *
   * @param claimConfigurationItemList All given claims which need to been validated
   * @param claims        the credentials given by the requester
   * @return true if the requester grants access, otherwise false
   */
  boolean validate(List<ClaimConfigurationItem> claimConfigurationItemList, ClientCredentials claims);


}
