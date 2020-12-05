package de.pseudonymisierung.mainzelliste.utils;

import de.pseudonymisierung.mainzelliste.configuration.claim.ClaimEnum;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.Properties;
import java.util.Set;

public class Permission {
  private final static String delimiter="[;,]";

  private static Set<String> splitPermissionValue(String permissions){
    return ConfigurationUtils.splitConfigurationValue(permissions, delimiter);
  }

  public static Set<String> getPemissions(Properties props, String prefix){
    String propsKey = ConfigurationUtils.getConcatedConfigurationPath(prefix, ClaimEnum.PERMISSIONS.getClaimName());
    String permissionKey = props.getProperty(propsKey);
    return splitPermissionValue(permissionKey);
  }
}
