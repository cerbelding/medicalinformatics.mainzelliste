package de.pseudonymisierung.mainzelliste.configuration.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServer;
import de.pseudonymisierung.mainzelliste.configuration.authorizationServer.oidc.OIDCServerConfigurationParser;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Parses all AuthorizationServers
 */
public class AuthorizationServerParser {

  public static Set<AuthorizationServer> parseOIDCServerConfiguration(Properties props) {
    Set<AuthorizationServer> authorizationServers = new HashSet<>();

    authorizationServers.addAll(OIDCServerConfigurationParser.parseOIDCServerConfiguration(props));

    return authorizationServers;
  }

}
