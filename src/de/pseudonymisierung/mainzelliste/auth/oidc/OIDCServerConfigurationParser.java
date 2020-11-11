package de.pseudonymisierung.mainzelliste.auth.oidc;

import de.pseudonymisierung.mainzelliste.configuration.ConfigurationParser;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

public class OIDCServerConfigurationParser {
  private static Logger logger = Logger.getLogger(OIDCConfigurationParser.class);
  private static final String OIDCKEY="oidc";
  private static final String ISS="iss";
  private static final String POSTFIXREGEX="iss|";


  private static OIDCServer createOIDCServer(Map<String,String> configurationMap, String prefix, String serverId){
    String issuerKey = ConfigurationUtils.getConcatedConfigurationPath(prefix, ISS);

    if(!configurationMap.containsKey(issuerKey)){
      logger.warn("OIDC-Serverconfig could not been parsed " + issuerKey);
      return  null;
    }
    String issuerValue = configurationMap.get(issuerKey);
    return new OIDCServer(issuerValue,serverId);
  }

  public static Set<OIDCServer> parseOIDCServerConfiguration(Properties props){
    Set<OIDCServer> oidcServerSet = new HashSet<>();
    Set<String> oidcServerNameList = ConfigurationParser.getDynamicKeys(props, OIDCKEY, POSTFIXREGEX);

    for(String oidcServerId: oidcServerNameList){
      String oidcServerPrefix = ConfigurationUtils.getConcatedConfigurationPath(OIDCKEY, oidcServerId);

      List<String> oidcServerKeys = ConfigurationParser.filterConfiguration(props,oidcServerPrefix, false);
      Map<String, String> mappedOIDCServerKeys = ConfigurationParser.parseConfigurationToMap(props, oidcServerKeys);

      OIDCServer oidcServer = createOIDCServer(mappedOIDCServerKeys,oidcServerPrefix, oidcServerId);
      if(oidcServer != null) oidcServerSet.add(oidcServer);
    }
    return oidcServerSet;
  }
}
