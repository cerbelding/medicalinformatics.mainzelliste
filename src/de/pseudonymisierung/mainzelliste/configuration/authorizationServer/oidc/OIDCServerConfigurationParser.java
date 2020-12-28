package de.pseudonymisierung.mainzelliste.configuration.authorizationServer.oidc;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.OIDCServer;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.oidcClaimConfiguration.OIDCClaimConfigurationParser;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Parses the oidc-Servers configuration properties
 */

public class OIDCServerConfigurationParser {

  private static final Logger logger = LogManager.getLogger(OIDCClaimConfigurationParser.class);
  private static final String OIDCKEY = "oidc";
  private static final String ISS = "iss";
  private static final String POSTFIXREGEX = "iss|";

  /**
   * Creates an OIDCServer Instance with the given property
   *
   * @param configurationMap Configuration Map related to the oidc config entry
   * @param prefix           prefix of the oidc config entry
   * @param serverId         id of the oidc server
   * @return the created OIDCServer
   */

  private static OIDCServer createOIDCServer(Map<String, String> configurationMap, String prefix,
      String serverId) {
    String issuerKey = ConfigurationUtils.getConcatenatedConfigurationPath(prefix, ISS);

    if (!configurationMap.containsKey(issuerKey)) {
      logger.warn("OIDC-Server config could not been parsed " + issuerKey);
      return null;
    }
    String issuerValue = configurationMap.get(issuerKey);
    return new OIDCServer(issuerValue, serverId);
  }

  /**
   * Parsed the configuration to a Set of OIDCServers
   *
   * @param props the configuration file
   * @return a Set of OIDCServers
   */

  public static Set<AuthorizationServer> parseOIDCServerConfiguration(Properties props) {
    Set<AuthorizationServer> oidcServerSet = new HashSet<>();
    Set<String> oidcServerNameList = ConfigurationUtils
        .getDynamicKeys(props, OIDCKEY, POSTFIXREGEX);

    for (String oidcServerId : oidcServerNameList) {
      String oidcServerPrefix = ConfigurationUtils
          .getConcatenatedConfigurationPath(OIDCKEY, oidcServerId);

      List<String> oidcServerKeys = ConfigurationUtils
          .filterConfiguration(props, oidcServerPrefix, false);
      Map<String, String> mappedOIDCServerKeys = ConfigurationUtils
          .parseConfigurationToMap(props, oidcServerKeys);

      OIDCServer oidcServer = createOIDCServer(mappedOIDCServerKeys, oidcServerPrefix,
          oidcServerId);
      if (oidcServer != null) {
        oidcServerSet.add(oidcServer);
      }
    }
    return oidcServerSet;
  }
}
