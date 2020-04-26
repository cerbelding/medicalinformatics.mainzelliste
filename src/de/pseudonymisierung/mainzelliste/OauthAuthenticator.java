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
package de.pseudonymisierung.mainzelliste;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the OAuth2.0 Authentication
 */
public class OauthAuthenticator implements Authenticator<String> {

    protected String cliendId;
    protected String clientSecret;
    protected String metadataUrl;
    protected Set<String> subs;
    final String requestedRessource = "userinfo_endpoint";
    final String userInfoEndpointUrl;

    public OauthAuthenticator(String cliendId, String clientSecret, String metadataUrl, Set<String> subs){
        this.cliendId = cliendId;
        this.clientSecret = clientSecret;
        this.metadataUrl = metadataUrl;
        this.subs = subs;
        userInfoEndpointUrl = getUserInfoEndPointURL();


    }

    /**
     * Return the UserInformation provided by the Userinfo endpoint from the openId Provider
     * @return Userinformation as Map
     */
    private Map<String,String> getUserInformations(){
        return new HashMap<String,String>();
    }

    /**
     * Retrieves the Userinfo endpoint Url from the OpenId Configuration
     * @return the Url to the Userinfo endpoint
     */
    private String getUserInfoEndPointURL(){
        return "";

    }

    @Override
    public boolean hasPermission(String accessToken) {
        Map<String,String> userInformation = getUserInformations();
        return subs.contains(userInformation.get("sub"));
    }
}
