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
import java.util.Properties;
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
}
