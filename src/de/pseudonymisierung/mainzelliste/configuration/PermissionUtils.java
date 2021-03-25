package de.pseudonymisierung.mainzelliste.configuration;

import de.pseudonymisierung.mainzelliste.configuration.claimConfiguration.ClaimConfigurationEnum;
import java.util.Properties;
import java.util.Set;

/**
 * Implements reusable methods for the permission configuration property
 */

public class PermissionUtils {

  /**
   * Splits the permission string into its single permission properties
   *
   * @param permissions The configuration permission value string
   * @return The Permissions as Set
   */

  private static Set<String> splitPermissionValue(String permissions) {
    return ConfigurationUtils.splitDefaultConfigurationValue(permissions);
  }

  /**
   * Extracts the Permissions as a Set from the configuration file
   *
   * @param props  The configuration file as property
   * @param prefix the prefix where the permission is stores (without the permission property path)
   * @return A Set with all Permissions of the path
   */

  public static Set<String> getPermissions(Properties props, String prefix) {
    String propsKey = ConfigurationUtils
        .getConcatenatedConfigurationPath(prefix, ClaimConfigurationEnum.PERMISSIONS.getClaimName());
    String permissionKey = props.getProperty(propsKey);
    return splitPermissionValue(permissionKey);
  }
}
