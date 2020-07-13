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
package de.pseudonymisierung.mainzelliste.exceptions;

import org.apache.commons.lang.StringUtils;

public class InvalidConfigurationException extends RuntimeException {

  public InvalidConfigurationException(String errorMessage, Throwable err) {
    super(errorMessage, err);
  }

  public InvalidConfigurationException(String errorMessage) {
    super(errorMessage);
  }

  public InvalidConfigurationException(String configurationKey, String errorMessage) {
    this(buildMessage(configurationKey, errorMessage));
  }

  public InvalidConfigurationException(String configurationKey, String errorMessage,
      Throwable err) {
    this(buildMessage(configurationKey, errorMessage), err);
  }

  private static String buildMessage(String configurationKey, String errorMessage) {
    return StringUtils.isBlank(configurationKey) ? errorMessage :
        String.format("Invalid configuration '%s': %s", configurationKey, errorMessage);
  }
}
