package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidJSONException;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import de.pseudonymisierung.mainzelliste.webservice.TokenParam;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PermissionUtil {

    private static Logger logger = Logger.getLogger(PermissionUtil.class);
    //TODO: Class should not be static, multiple access!
    private List<PermissionUtilTokenDTO> tokenValues = new ArrayList<>();


    //TODO: will be obsolete!
    public static boolean checkTokenPermission(String tokenid) {
        boolean valid = true;
        // TODO: Check if resultFields, resultIds, any other thing inside token is valid !!!!!!!!!!!
        // TODO: see above
        // TODO: see above
        // TODO: see above
        // TODO: see above
        return valid;
    }

    public static boolean checkPermission(Token token) {
        //Token token = Servers.instance.getTokenByTid(tokenId);
        Set<String> permissions = Servers.instance.getServerPermissionsForServerName(token.getParentServerName());

        return true;
    }

    public boolean checkPermission(String tokenParameter, Session session) {
        //Token token = new TokenParam(tokenParameter).getValue();
        logger.debug("checkPermission");
        extractJSONObjectTokenValues(tokenParameter);
        getPermissions(session.getParentServerName());

        tokenValues.clear();
        return true;
    }

    private void getPermissions(String servername){
        Servers.instance.getServerPermissionsForServerName(servername);
    }


    private void extractJSONObjectTokenValues(String tokenParameter) {
        try {
            JSONObject tokenParameterJson = new JSONObject(tokenParameter);
            tokenParameterJson.keys().forEachRemaining(parameterKey -> {

                try {
                    Object parameterValue = tokenParameterJson.get((String) parameterKey);

                    logger.debug(parameterKey + "=" + parameterValue);
                    if (parameterValue.getClass().getTypeName().equals("java.lang.String")) {
                        tokenValues.add(new PermissionUtilTokenDTO((String) parameterKey, (String)parameterValue));
                    } else {
                        extractSubJSONTokenValues((String)parameterKey, parameterValue);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new InvalidJSONException(e.toString());
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            throw new InvalidJSONException(e.toString());
        }
    }

    private void extractSubJSONTokenValues(String parameterDescriber, Object parameterValue) {

        if (parameterValue.getClass().getName().contains("JSONArray")) {
            JSONArray parameterArray = (JSONArray) parameterValue;

            for (int i = 0; i < parameterArray.length(); i++) {
                try {
                    logger.debug(parameterArray.get(i));
                    logger.debug(parameterArray.get(i).getClass());
                    if (parameterArray.get(i).getClass().toString().contains("JSONObject") || parameterArray.get(i).getClass().toString().contains("JSONArray")) {
                        extractSubJSONTokenValues(parameterDescriber + "[" + i + "]", parameterArray.get(i));
                    }else if(parameterArray.get(i).getClass().getTypeName().equals("java.lang.String")) {
                        tokenValues.add(new PermissionUtilTokenDTO((String) parameterDescriber, (String)parameterArray.get(i)));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new InvalidJSONException(e.toString());
                }
            }

        }

        if (parameterValue.getClass().getName().contains("JSONObject")) {
            JSONObject parameterValueJson = (JSONObject) parameterValue;
            parameterValueJson.keys().forEachRemaining(parameterSubKey -> {
                try {
                    Object parameterSubValue = parameterValueJson.get((String) parameterSubKey);
                    logger.debug(parameterSubKey + "=" + parameterSubValue);
                    if (parameterSubValue.getClass().getName().contains("JSONObject") || parameterSubValue.getClass().getName().contains("JSONArray")) {
                        extractSubJSONTokenValues(parameterDescriber + "." + (String) parameterSubKey, parameterSubValue);
                    }else if(parameterSubValue.getClass().getTypeName().equals("java.lang.String")) {
                        tokenValues.add(new PermissionUtilTokenDTO(parameterDescriber + "." + (String) parameterSubKey, (String)parameterSubValue));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new InvalidJSONException(e.toString());
                }

            });
        }
    }


}
