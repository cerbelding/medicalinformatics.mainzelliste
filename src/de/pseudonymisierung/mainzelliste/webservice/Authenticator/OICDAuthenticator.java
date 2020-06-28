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
package de.pseudonymisierung.mainzelliste.webservice.Authenticator;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Represents the OpenId Connect Authentication
 */
public class OICDAuthenticator implements Authenticator {

    private final Logger logger = Logger.getLogger(OICDAuthenticator.class);
    protected Set<String> subs;
    protected Set<String> roles;

    /**
     *  Creates a new OICDAuthenticator related to a User or a Server
     * @param subs the registered subs
     * @param roles the registered roles
     */
    public OICDAuthenticator(Set<String> subs, Set<String> roles) {
        this.subs = subs;
        this.roles = roles;
    }

    @Override
    public boolean isAuthenticated(Map<String, String> claims) {
        boolean isAuthenticated = false;
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            if(entry.getKey().equals("sub")){
                isAuthenticated =  isAuthenticated || subs.contains(entry.getValue());
            }
            else if(entry.getKey().matches("role.*")){
                isAuthenticated =  isAuthenticated || roles.contains(entry.getValue());
            }
        }
        if(isAuthenticated){
            logger.info("Requester could be authenticated with claims: " + claims.toString());
        }
        else {
            logger.info("Requester could not been authenticated");
        }
         return isAuthenticated;
    }

}
