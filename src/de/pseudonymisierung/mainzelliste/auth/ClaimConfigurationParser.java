package de.pseudonymisierung.mainzelliste.auth;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationParser;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import de.pseudonymisierung.mainzelliste.utils.EnumLookup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

public class ClaimConfigurationParser {


 private static Logger logger = Logger.getLogger(ClaimConfigurationParser.class);

 private static ClaimConfiguration parseClaim(Properties props, String prefix){
   Set<String> permissions = Permission.parsePermissions(
       props.getProperty(ConfigurationUtils.getConcatedConfigurationPath(prefix, ClaimEnum.PERMISSIONS.getClaimName()))
   );
   String authValue = props.getProperty(ConfigurationUtils.getConcatedConfigurationPath(prefix, ClaimEnum.AUTH.getClaimName()));
   ClaimAuthEnum claimAuthEnum = EnumLookup.lookup(ClaimAuthEnum.class,authValue);



   List<String> claimKeys = ConfigurationParser.filterConfiguration(
       props,
       ConfigurationUtils.getConcatedConfigurationPath(prefix,claimAuthEnum.getClaimAuthName().toLowerCase())
       );
   Map<String, String> mappedClaimProperties = ConfigurationParser.parseConfigurationToMap(props, claimKeys);

   ClaimProperty claimProperty = new ClaimPropertyFactory().createClaimProperty(claimAuthEnum,mappedClaimProperties );

   ClaimConfiguration claimConfiguration = new ClaimConfiguration(permissions,claimAuthEnum,claimProperty);

   return claimConfiguration;
 }

  public static List<ClaimConfiguration> parseConfiguration(Properties props) {
    List<ClaimConfiguration> claimConfigurationList = new ArrayList<>();

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
