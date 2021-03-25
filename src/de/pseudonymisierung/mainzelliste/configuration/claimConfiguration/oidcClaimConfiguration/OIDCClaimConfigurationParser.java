package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.oidcClaimConfiguration;


import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServers;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationItem;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList.OIDCClaimConfigurationItemList;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.subset.Subset;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.subset.SubsetEnum;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.subset.SubsetFactory;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator.Operator;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator.OperatorEnum;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.operator.OperatorFactory;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses the OIDC claim-configuration given by the configuration file
 */

public class OIDCClaimConfigurationParser {

  private static final Logger logger = LogManager.getLogger(OIDCClaimConfigurationParser.class);
  private static final String OPERATOR = "operator";
  private static final String SUBSET = "subset";
  private static final OperatorEnum defaultOperatorEnum = OperatorEnum.AND;
  private static final SubsetEnum defaultSubsetEnum = SubsetEnum.ANY;
  private static final String subsetRegex = "subset|";
  private static final String EXCLUDEOIDC = "(?!.operator)";
  private static final String ISSPREFIX = "iss";

  /**
   * Retrieves the OIDC-Server defined by the iss property of an claim configuration
   *
   * @param configurationProperties the key-value Mapping of the claim configuration
   * @param prefix                  the oidc prefix to define the OIDC-Server
   * @param authorizationServers    the related Authorizationservers
   * @return the OIDC-Server if it could be found otherwise null
   */

  public static AuthorizationServer getOIDCServer(Map<String, String> configurationProperties,
      String prefix, AuthorizationServers authorizationServers) {
    String oidcServerKey = ConfigurationUtils.getConcatenatedConfigurationPath(prefix, ISSPREFIX);
    if (configurationProperties.containsKey(oidcServerKey)) {
      String oidcServerName = configurationProperties.get(oidcServerKey);
      AuthorizationServer authorizationServer = authorizationServers.getAuthorizationServerByName(oidcServerName);
      if (authorizationServer == null) {
        logger.warn("OIDC-Server name not found " + prefix);
      }
      return authorizationServer;
    }
    logger.warn("OIDC-Server name not found " + prefix);
    return  null;
  }

  /**
   * Defines the required operator value
   *
   * @param configurationProperties the key-value Mapping of the claim configuration
   * @param prefix                  the oidc prefix to define the operator value
   * @return the operator value
   */


  private static OperatorEnum getOperatorEnum(Map<String, String> configurationProperties,
      String prefix) {
    String operatorKey = ConfigurationUtils.getConcatenatedConfigurationPath(prefix, OPERATOR);
    if (configurationProperties.containsKey(operatorKey)) {
      String operatorValue = configurationProperties.get(operatorKey);
      return OperatorEnum.valueOf(operatorValue);
    } else {
      return defaultOperatorEnum;
    }
  }

  /**
   * Defines the required subset value for an claimItem
   *
   * @param configurationProperties the key-value Mapping of the claim configuration
   * @param prefix                  the oidc prefix to define the subset value
   * @return the subset value
   */
  private static SubsetEnum getSubsetEnum(Map<String, String> configurationProperties,
      String prefix) {
    String subsetKey = ConfigurationUtils.getConcatenatedConfigurationPath(prefix, SUBSET);
    if (configurationProperties.containsKey(subsetKey)) {
      String subsetValue = configurationProperties.get(subsetKey);
      return SubsetEnum.valueOf(subsetValue);
    } else {
      return defaultSubsetEnum;
    }
  }

  /**
   * Parses the claimItem configuration given by the configuration file to an claimItem instance
   *
   * @param configurationProperties the key-value Mapping of the claim configuration
   * @param prefix                  the oidc prefix of the claimItem
   * @param claim                   the name of the claim
   * @return the generated claimItem
   */

  private static ClaimConfigurationItem parseOIDCClaim(Map<String, String> configurationProperties,
      String prefix, String claim) {
    String claimPrefix = ConfigurationUtils.getConcatenatedConfigurationPath(prefix, claim);
    SubsetEnum subsetEnum = getSubsetEnum(configurationProperties, claimPrefix);
    Subset subset = new SubsetFactory().createSubset(subsetEnum);
    String claimValue = configurationProperties.get(claimPrefix);
    Set<String> splittedClaimValues = ConfigurationUtils.splitDefaultConfigurationValue(claimValue);
    return new ClaimConfigurationItem(claim, subset, splittedClaimValues);
  }


  /**
   * Generates an OIDCProperty for an ClaimConfiguration instance
   *
   * @param configurationProperties the key-value Mapping of the claim configuration
   * @param prefix                  the oidc prefix of the claimItem
   * @return the generated OIDCProperty instance
   */

  public static OIDCClaimConfigurationItemList parseConfiguration(Map<String, String> configurationProperties,
      String prefix, AuthorizationServers authorizationServers) {

    OperatorEnum operatorEnum = getOperatorEnum(configurationProperties, prefix);
    Operator operator = new OperatorFactory().createOperator(operatorEnum);

    Set<String> claims = ConfigurationUtils
        .getDynamicKeys(configurationProperties, "(" + prefix + ")" + EXCLUDEOIDC, subsetRegex);
    claims.remove(ISSPREFIX);
    List<ClaimConfigurationItem> claimConfigurationItemList = new ArrayList<>();
    for (String claim : claims) {
      ClaimConfigurationItem oidcClaimConfigurationItem = parseOIDCClaim(configurationProperties, prefix, claim);
      claimConfigurationItemList.add(oidcClaimConfigurationItem);
    }
    AuthorizationServer oidcServer = getOIDCServer(configurationProperties, prefix, authorizationServers);
    if (oidcServer == null) {
      return null;
    }
    return new OIDCClaimConfigurationItemList(operator, claimConfigurationItemList);

  }

}
