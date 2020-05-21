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
package de.pseudonymisierung.mainzelliste.webservice.Authorizator;

import de.pseudonymisierung.mainzelliste.webservice.HttpsClient.*;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the OpenId Connect Authentication
 */
public class OICDAuthorizator implements Authorizator{

    private final Logger logger = Logger.getLogger(OICDAuthorizator.class);

    protected Set<String> subs;
    protected Set<String> roles;


    public OICDAuthorizator( Set<String> subs, Set<String> roles){
        this.subs = subs;
        this.roles = roles;
    }




    @Override
    public boolean hasPermission(Map<String,String> claims) {
        return subs.contains(claims.get("sub")) || roles.contains(claims.get("roles"));
    }
}
