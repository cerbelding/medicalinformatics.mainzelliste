/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste.util;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidConfigurationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ConfigUtils {

  private ConfigUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Get and parse a boolean configuration value
   *
   * @param configurations   configurations as properties
   * @param configurationKey the key of the configured value
   * @param defaultValue     return this value, if no configuration value found
   * @return the configuration value, otherwise the given default value
   */
  public static boolean readValue(Properties configurations, String configurationKey,
      boolean defaultValue) {
    String value = configurations.getProperty(configurationKey);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    } else if (StringUtils.trim(value).equalsIgnoreCase("true")) {
      return true;
    } else if (StringUtils.trim(value).equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new InvalidConfigurationException(
          configurationKey, "the configured value " + value + " must be boolean");
    }
  }

  /**
   * transform a configuration entry in the following format : <br> {@code
   * prefix.<variable>.<propertyKey> = <propertyValue>} <br> in a map with {@code <variable>} as key
   * and the given suffix {@code <propertyKey>} together with the value {@code <propertyValue>} in
   * property list as value.
   *
   * @param properties configurations as Properties
   * @param prefix     configuration key prefix
   * @return a map with variable name as key and its properties as value
   */
  public static Map<String, Properties> getVariableSubProperties(Properties properties,
      String prefix) {
    Map<String, Properties> childrenPropertiesMap = new HashMap<>();
    // property key should look like this : prefix.<var>.suffix
    properties.stringPropertyNames()
        .stream()
        .filter(k -> Pattern.matches("^" + prefix + "\\.\\w+\\..+", k.trim()))
        .forEach(k -> {
          String subKey = k.substring(prefix.length() + 1); // remove prefix from key
          childrenPropertiesMap.compute(
              subKey.split("\\.")[0], // get "<var>" @see example above
              (newK, newProperties) -> addProperty(
                  newProperties,
                  subKey.substring(newK.length() + 1), // get "suffix" @see example above
                  properties.getProperty(k)));         // get property value
        });
    return childrenPropertiesMap;
  }

  public static Properties addProperty(Properties properties, String key, String value) {
    if (properties == null) {
      properties = new Properties();
    }
    properties.setProperty(key, value);
    return properties;
  }
}
