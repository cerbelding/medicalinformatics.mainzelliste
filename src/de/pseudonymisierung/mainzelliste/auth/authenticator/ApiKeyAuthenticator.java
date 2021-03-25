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
package de.pseudonymisierung.mainzelliste.auth.authenticator;

import de.pseudonymisierung.mainzelliste.auth.credentials.ClientCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Implements the ApiKey authentication
 */

public class ApiKeyAuthenticator implements Authenticator {

  private final String apiKey;
  private final Logger logger = LogManager.getLogger(ApiKeyAuthenticator.class);

  /**
   * Creates a new Api Key Authenticator related to a User or a Server
   *
   * @param apiKey the apiKey to authenticate the requester
   */

  public ApiKeyAuthenticator(String apiKey) {
    this.apiKey = apiKey;
  }


  @Override
  public boolean isAuthenticated(ClientCredentials apiKeyClientCredentials) {
    String apiKey = apiKeyClientCredentials.getId();
    boolean isAuthenticated = false;
    if (apiKey != null) {
      isAuthenticated = this.apiKey.equals(apiKey);
    }
    if (isAuthenticated) {
      logger.info("Requester could be authenticated with api key: " + apiKey);
    } else {
      logger.info("Requester could not been authenticated");
    }
    return isAuthenticated;
  }
}
