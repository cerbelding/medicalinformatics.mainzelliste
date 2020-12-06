package de.pseudonymisierung.mainzelliste.configuration.claim;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServerFactory;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.IAuthorization;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import de.pseudonymisierung.mainzelliste.configuration.claim.claimList.ClaimList;
import de.pseudonymisierung.mainzelliste.configuration.claim.claimList.ClaimListFactory;
import de.pseudonymisierung.mainzelliste.utils.EnumLookup;
import de.pseudonymisierung.mainzelliste.configuration.PermissionUtils;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Parses the Claim- configuration properties
 */

public class ClaimConfigurationParser {

  private static final Logger logger = Logger.getLogger(ClaimConfigurationParser.class);

  /**
   * Parses a specific Claim configuration
   *
   * @param props  the configuration file
   * @param prefix the prefix of the specific claim configuration
   * @return a parsed ClaimConfiguration object
   */
  private static ClaimConfiguration parseClaim(Properties props, String prefix) {
    Set<String> permissions = PermissionUtils.getPermissions(props, prefix);
    String authValue = props.getProperty(
        ConfigurationUtils.getConcatenatedConfigurationPath(prefix, ClaimEnum.AUTH.getClaimName()));
    ClaimAuthEnum claimAuthEnum;

    try {
      claimAuthEnum = EnumLookup.lookup(ClaimAuthEnum.class, authValue);
    } catch (IOException e) {
      logger.error("Error parsing the Enum value: " + authValue);
      return null;
    }

    String authPrefix = ConfigurationUtils
        .getConcatenatedConfigurationPath(prefix, claimAuthEnum.getClaimAuthName());

    List<String> claimKeys = ConfigurationUtils.filterConfiguration(props, authPrefix, true);
    Map<String, String> mappedClaimProperties = ConfigurationUtils
        .parseConfigurationToMap(props, claimKeys);

    ClaimList claimList = new ClaimListFactory(mappedClaimProperties, authPrefix)
        .createClaimProperty(claimAuthEnum);
    IAuthorization iAuthorization = new AuthorizationServerFactory(mappedClaimProperties,
        authPrefix).getAuthorizationServer(claimAuthEnum);
    return new ClaimConfiguration(permissions, claimAuthEnum,
        claimList, iAuthorization);
  }

  /**
   * Iterates over the configuration file and parses the Claim configuration properties
   *
   * @param props the configuration file
   * @return A Set of the Claim Configuration
   */
  public static Set<ClaimConfiguration> parseConfiguration(Properties props) {
    Set<ClaimConfiguration> claimConfigurationList = new HashSet<>();

    for (int i = 0; ; i++) {
      if (
          !props.containsKey("claims." + i + ".permissions") ||
              !props.containsKey("claims." + i + ".auth")
      ) {
        logger.warn("Claims configuration parsing failed");
        break;
      }
      ClaimConfiguration claimConfiguration = parseClaim(props, "claims." + i);
      if (claimConfiguration != null) {
        claimConfigurationList.add(claimConfiguration);
      }
    }
    return claimConfigurationList;
  }
}
