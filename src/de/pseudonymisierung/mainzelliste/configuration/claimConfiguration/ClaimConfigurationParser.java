package de.pseudonymisierung.mainzelliste.configuration.claimConfiguration;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServerFactory;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServers;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList.ClaimConfigurationItemList;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.claimConfigurationItemList.ClaimConfigurationItemListFactory;
import de.pseudonymisierung.mainzelliste.utils.EnumLookup;
import de.pseudonymisierung.mainzelliste.configuration.PermissionUtils;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Parses the Claim- configuration properties
 */

public class ClaimConfigurationParser {

  private static final Logger logger = LogManager.getLogger(ClaimConfigurationParser.class);

  /**
   * Parses a specific Claim configuration
   *
   * @param props  the configuration file
   * @param prefix the prefix of the specific claim configuration
   * @return a parsed ClaimConfiguration object
   */
  private static ClaimConfiguration parseClaim(Properties props, String prefix, AuthorizationServers authorizationServers) {
    Set<String> permissions = PermissionUtils.getPermissions(props, prefix);
    String authValue = props.getProperty(
        ConfigurationUtils.getConcatenatedConfigurationPath(prefix, ClaimConfigurationEnum.AUTH.getClaimName()));
    ClaimConfigurationAuthEnum claimConfigurationAuthEnum;

    try {
      claimConfigurationAuthEnum = EnumLookup.lookup(ClaimConfigurationAuthEnum.class, authValue);
    } catch (IOException e) {
      logger.error("Error parsing the Enum value: " + authValue);
      return null;
    }

    String authPrefix = ConfigurationUtils
        .getConcatenatedConfigurationPath(prefix, claimConfigurationAuthEnum.getClaimAuthName());

    List<String> claimKeys = ConfigurationUtils.filterConfiguration(props, authPrefix, true);
    Map<String, String> mappedClaimProperties = ConfigurationUtils
        .parseConfigurationToMap(props, claimKeys);

    ClaimConfigurationItemList claimConfigurationItemList = new ClaimConfigurationItemListFactory(mappedClaimProperties, authPrefix, authorizationServers)
        .createClaimProperty(claimConfigurationAuthEnum);
    AuthorizationServer iAuthorization = new AuthorizationServerFactory(mappedClaimProperties,
        authPrefix, authorizationServers).getAuthorizationServer(claimConfigurationAuthEnum);
    return new ClaimConfiguration(permissions, claimConfigurationAuthEnum,
        claimConfigurationItemList, iAuthorization);
  }

  /**
   * Iterates over the configuration file and parses the Claim configuration properties
   *
   * @param props the configuration file
   * @param authorizationServers the related Authorizationservers
   * @return A Set of the Claim Configuration
   */
  public static Set<ClaimConfiguration> parseConfiguration(Properties props, AuthorizationServers authorizationServers) {
    Set<ClaimConfiguration> claimConfigurationList = new HashSet<>();

    for (int i = 0; ; i++) {
      if (
          !props.containsKey("claims." + i + ".permissions") ||
              !props.containsKey("claims." + i + ".auth")
      ) {
        logger.warn("Claims configuration parsing failed");
        break;
      }
      ClaimConfiguration claimConfiguration = parseClaim(props, "claims." + i,  authorizationServers);
      if (claimConfiguration != null) {
        claimConfigurationList.add(claimConfiguration);
      }
    }
    return claimConfigurationList;
  }
}
