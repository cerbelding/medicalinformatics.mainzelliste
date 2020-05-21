package de.pseudonymisierung.mainzelliste.webservice.User;

import de.pseudonymisierung.mainzelliste.webservice.Authorizator.OICDAuthorizator;
import de.pseudonymisierung.mainzelliste.webservice.HttpsClient.OICDService;
import de.pseudonymisierung.mainzelliste.webservice.JWTDecoder;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class UserList {
    private final List<User> userList;
    private static final Logger logger = Logger.getLogger(OICDAuthorizator.class);


    public UserList(List<User> userList){
        this.userList = userList;

    }



    private Map<String,String> getIOCDUserClaims(String accessToken) throws JSONException,IOException{
        JSONObject jwtPayload = JWTDecoder.decode(accessToken);
        String iss = jwtPayload.getString("iss");
        String userInfoEndpointUrl = OICDService.getUserInfoEndPointURL(iss);
        Map<String,String> claims = OICDService.getUserClaims(accessToken, userInfoEndpointUrl);
        return claims;
    }

    private Set<String> getUserPermissions(Map<String,String> claims){
        Set<String> permissions = new HashSet<>();

        for(int i = 0; i < userList.size(); i++){
            Set<String> usersPermissions =  userList.get(i).getPermissions(claims);
            if(usersPermissions.isEmpty()){
                continue;
            }
            else{
                return usersPermissions;
            }
        }
        return  permissions;
    }


    public Set<String> getOICDPermissions(String accessToken){
        Set<String> permissions = new HashSet<String>();
        Map<String, String> claims = new HashMap<>();
        try {
            claims = getIOCDUserClaims(accessToken);
            permissions = getUserPermissions(claims);
            return permissions;

        } catch (JSONException e) {
           logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        finally {
            return permissions;
        }
    }


}
