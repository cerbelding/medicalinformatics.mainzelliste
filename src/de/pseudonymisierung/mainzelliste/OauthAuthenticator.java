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
public class OauthAuthenticator implements Authenticator<String> {

    private final Logger logger = Logger.getLogger(OauthAuthenticator.class);

    protected String cliendId;
    protected String clientSecret;
    protected String metadataUrl;
    protected Set<String> subs;
    protected Set<String> roles;
    final String USERINFOENDPOINT = "userinfo_endpoint";
    final String userInfoEndpointUrl;

    public OauthAuthenticator(String cliendId, String clientSecret, String metadataUrl, Set<String> subs, Set<String> roles){
        this.cliendId = cliendId;
        this.clientSecret = clientSecret;
        this.metadataUrl = metadataUrl;
        this.subs = subs;
        this.roles = roles;
        this.userInfoEndpointUrl =  getUserInfoEndPointURL();
    }

    /**
     * Return the UserInformation provided by the Userinfo endpoint from the openId Provider
     * @return Userinformation as JSONObject
     */
    private JSONObject getUserInformations(String accessToken) {
        JSONObject userInfo = new JSONObject();
        if (userInfoEndpointUrl.isEmpty()) {
            return userInfo;
        } else {
                HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
                httpHeader.setAuthorization(HttpClientAuthorization.createBearerAuthentication(accessToken));
                userInfo = new HttpsClient(metadataUrl).request(httpHeader, new HttpUrlParameterBuilder());

                return userInfo;
        }
    }

    /**
     * Returns the User Claims as a Map
     * @param acessToken The acessToken of the User
     * @return The sub, roles Claims of the User stored in a Map
     */

    private Map<String, String> getUserPermissions(String acessToken){
        JSONObject userInfo = getUserInformations(acessToken);
        Map<String, String> userClaims = new HashMap<>();
        String sub;
        String roles;
        try {
            sub = userInfo.getString("sub");
            roles = userInfo.getString("roles");
            userClaims.put("roles", roles);
            userClaims.put("sub", sub);
        }
        catch (JSONException e) {
            userClaims.put("roles", "");
            userClaims.put("sub", "");
        }
        return userClaims;
    }

    /**
     * Retrieves the Userinfo endpoint Url from the OpenId Configuration
     * @return the Url to the Userinfo endpoint
     */
    private String getUserInfoEndPointURL(){
        try {
            HttpHeadersImpl httpHeader = new HttpHeadersImpl(HttpMethod.GET);
            JSONObject json = new HttpsClient(metadataUrl).request(httpHeader, new HttpUrlParameterBuilder());
            return  json.getString(USERINFOENDPOINT);
        } catch(JSONException e){
            logger.error(e);
            return "";
        }
    }

    @Override
    public boolean hasPermission(String accessToken) {
        Map<String,String> userInformation = getUserPermissions(accessToken);
        return subs.contains(userInformation.get("sub")) || roles.contains(userInformation.get("sub"));
    }
}
