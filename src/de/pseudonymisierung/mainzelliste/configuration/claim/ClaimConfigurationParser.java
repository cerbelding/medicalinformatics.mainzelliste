package de.pseudonymisierung.mainzelliste.configuration.claim;

import de.pseudonymisierung.mainzelliste.auth.authorizationServer.AuthorizationServerFactory;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.IAuthorization;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationParser;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import de.pseudonymisierung.mainzelliste.utils.EnumLookup;
import de.pseudonymisierung.mainzelliste.utils.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

public class ClaimConfigurationParser {


 private static Logger logger = Logger.getLogger(ClaimConfigurationParser.class);

 private static ClaimConfiguration parseClaim(Properties props, String prefix){
   Set<String> permissions = Permission.getPemissions(props, prefix);
   String authValue = props.getProperty(ConfigurationUtils.getConcatedConfigurationPath(prefix, ClaimEnum.AUTH.getClaimName()));
   ClaimAuthEnum claimAuthEnum = EnumLookup.lookup(ClaimAuthEnum.class, authValue);
   String authPrefix =  ConfigurationUtils.getConcatedConfigurationPath(prefix,claimAuthEnum.getClaimAuthName());

   List<String> claimKeys = ConfigurationParser.filterConfiguration(props,authPrefix,true);
   Map<String, String> mappedClaimProperties = ConfigurationParser.parseConfigurationToMap(props, claimKeys);

   ClaimProperty claimProperty = new ClaimPropertyFactory(mappedClaimProperties, authPrefix).createClaimProperty(claimAuthEnum);
   IAuthorization iAuthorization = new AuthorizationServerFactory(mappedClaimProperties, authPrefix).getAuthorizationServer(claimAuthEnum);
   ClaimConfiguration claimConfiguration = new ClaimConfiguration(permissions,claimAuthEnum,claimProperty, iAuthorization);
   return claimConfiguration;
 }

  public static Set<ClaimConfiguration> parseConfiguration(Properties props) {
    Set<ClaimConfiguration> claimConfigurationList = new HashSet<>();

    for (int i = 0; ; i++) {
      if (
          !props.containsKey("claims." + i + ".permissions") ||
          !props.containsKey("claims." + i + ".auth")
          ){
        logger.warn("Claims configuration parsing failed");
        break;
      }
      ClaimConfiguration claimConfiguration = parseClaim(props, "claims."+i);
      if(claimConfiguration != null) claimConfigurationList.add(claimConfiguration);
    }
    return claimConfigurationList;
  }
}
