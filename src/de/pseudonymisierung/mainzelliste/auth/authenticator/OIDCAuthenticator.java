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

import de.pseudonymisierung.mainzelliste.auth.ClaimProperty;
import de.pseudonymisierung.mainzelliste.auth.IAuthorization;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * Represents the OpenID Connect Authentication
 */
public class OIDCAuthenticator implements Authenticator {

  private final Logger logger = Logger.getLogger(OIDCAuthenticator.class);
  private Set<ClaimProperty> claimProperties;
  private IAuthorization oidcServer;
  private String sub;


  public OIDCAuthenticator(String sub, Set<ClaimProperty> claimProperties, IAuthorization oidcServer) {
    this.claimProperties = claimProperties;
    this.oidcServer = oidcServer;
    this.sub = sub;
  }

  @Override
  public boolean isAuthenticated(ClaimMap claimMap) {
    String sub = claimMap.get("sub");
    boolean isEqual = sub.equals(this.sub);
    return isEqual;
  }

  @Override
  public String getId() {
    return oidcServer.getId();
  }
}
