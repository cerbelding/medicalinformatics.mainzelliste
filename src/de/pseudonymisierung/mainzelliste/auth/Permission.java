package de.pseudonymisierung.mainzelliste.auth;

import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.Set;

public class Permission {
  private final static String delimiter="[;,]";

  public static Set<String> parsePermissions(String permissions){
    return ConfigurationUtils.splitConfigurationValue(permissions, delimiter);
  }
}
