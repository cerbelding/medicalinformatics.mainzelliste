package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidJSONException;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PermissionUtil {

    final static private String JAVA_LANG_STRING = "java.lang.String";
    final static private String JSON_OBJECT = "JSONObject";
    final static private String JSON_ARRAY = "JSONArray";


    private static Logger logger = Logger.getLogger(PermissionUtil.class);


    //TODO: Class should not be static, multiple access!
    private List<PermissionUtilTokenDTO> tokenValues = new ArrayList<>();
    private boolean multipleExistingValues;


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
        Set<String> permissions = Servers.instance.getServerPermissionsForServerName(token.getParentServerName());

        return true;
    }

    public boolean checkPermission(String tokenParameter, Session session) {
        logger.debug("checkPermission");
        extractJSONObjectTokenValues(tokenParameter);
        Set<String> serverPermissions = getServerPermissions(session.getParentServerName());

        if (compareRequestAndPermissions(tokenValues, serverPermissions)) {
            tokenValues.clear();
            return true;
        } else {
            tokenValues.clear();
            return false;
        }

    }

    private boolean compareRequestAndPermissions(List<PermissionUtilTokenDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("compareRequestAndPermissions" + "requestedPermissions: " + requestedPermissions + " serverPermissions" + serverPermissions);

        //Token request has exactly one time "type" : "xxx"
        if (requestedPermissions.stream().filter(tokenRequest -> tokenRequest.getRequestedParameter().equals("type")).map(PermissionUtilTokenDTO::getRequestedValue).count() == 1) {
            String requestedTokenType = requestedPermissions.stream().filter(tokenRequest -> tokenRequest.getRequestedParameter().equals("type")).map(PermissionUtilTokenDTO::getRequestedValue).findFirst().orElse("noElementFound");
            if (!validateTokeType(serverPermissions, requestedTokenType)) {
                return false;
            } else if (!validateFurtherTokenPermissions(requestedTokenType, requestedPermissions, serverPermissions)) {
                return false;
            }
            else {
                return true;
            }
        }
        return false;
    }

    private boolean validateTokeType(Set<String> serverPermissions, String requestedTokenType) {

        List<String> allowedTokenTypes = serverPermissions.stream().filter(o -> o.startsWith("tt_")).collect(Collectors.toList());
        allowedTokenTypes.replaceAll(e -> e.replaceAll("^tt_", ""));

        if (allowedTokenTypes.stream().anyMatch(str -> str.contains(requestedTokenType))) {
            return true;
        }

        return false;
    }

    private boolean validateFurtherTokenPermissions(String requestedTokenType, List<PermissionUtilTokenDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("validateFurtherTokenPermissions");

        if(serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).contains("tt_" + requestedTokenType)) {
            //TODO: check on Mainzelliste boot up
            logger.warn(requestedTokenType + " is configured with (too) extensive rights, maybe you can limit them.");
            return true;
        }
        else if(serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).count()==1){
            logger.debug("validateFurtherTokenPermissions: " + requestedTokenType);

            if(!validateTokenParameterAgainstServerParameter(requestedTokenType, requestedPermissions, serverPermissions)){
                return false;
            }
            if(!validateTokenValuesAgainstServerValues(requestedTokenType, requestedPermissions, serverPermissions)){
                return false;
            }
        }
        return false;
    }

    private boolean validateTokenParameterAgainstServerParameter(String requestedTokenType, List<PermissionUtilTokenDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("validateTokenParameterAgainstServerParameter");

        //TODO: replace "{" and "}" with first and last replace - didn't work yet
        String detailedServerPermissionsForRequestedTokenType = serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).get(0).replace("tt_" + requestedTokenType, "").replace("{", "").replace("}", "");

        AtomicBoolean returnValue = new AtomicBoolean(true);
        requestedPermissions.forEach(r -> {
            if (!detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter().replaceAll("[0-9]*", "").replaceAll("\\[", "").replaceAll("\\]", "")) && !r.getRequestedParameter().equals("type")) {
                logger.info(r.getRequestedParameter() + " is not in server permissions " + detailedServerPermissionsForRequestedTokenType);
                returnValue.set(false);
            }
        });

        return returnValue.get();
    }

    private boolean validateTokenValuesAgainstServerValues(String requestedTokenType, List<PermissionUtilTokenDTO> requestedPermissions, Set<String> serverPermissions){
        logger.debug("validateTokenValuesAgainstServerValues");

        //TODO: replace "{" and "}" with first and last replace - didn't work yet
        String detailedServerPermissionsForRequestedTokenType = serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).get(0).replace("tt_" + requestedTokenType, "").replace("{", "").replace("}", "");

        AtomicBoolean returnValue = new AtomicBoolean(true);

        requestedPermissions.forEach(r -> {if(multipleExistingValues){
            logger.info("multiple existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter().replaceAll("[0-9]*", "").replaceAll("\\[", "").replaceAll("\\]", "")));
        }else{
            logger.info("single existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter()));
            isParameterValueCombinationAllowed(r.getRequestedParameter(), r.getRequestedValue(), serverPermissions);

        }
            multipleExistingValues = r.getRequestedParameter().matches(".*\\[[\\][0-9]*\\[]\\].*");
        });

        return returnValue.get();
    }

    private boolean isParameterValueCombinationAllowed(String requestedParameter, String requestedValue, Set<String> serverPermissions){
        logger.info("isParameterValueCombinationAllowed");

        return true;
    }
    private Set<String> getServerPermissions(String servername) {
        return Servers.instance.getServerPermissionsForServerName(servername);
    }

    private void extractJSONObjectTokenValues(String tokenParameter) {


        try {
            JSONObject tokenParameterJson = new JSONObject(tokenParameter);
            tokenParameterJson.keys().forEachRemaining(parameterKey -> {

                try {
                    Object parameterValue = tokenParameterJson.get((String) parameterKey);

                    logger.debug(parameterKey + "=" + parameterValue);
                    if (parameterValue.getClass().getTypeName().equals(JAVA_LANG_STRING)) {
                        tokenValues.add(new PermissionUtilTokenDTO((String) parameterKey, (String) parameterValue));
                    } else {
                        extractSubJSONTokenValues((String) parameterKey, parameterValue);
                    }


                } catch (JSONException e) {
                    logger.error(e);
                    throw new InvalidJSONException(e.toString());
                }
            });

        } catch (JSONException e) {
            logger.error(e);
            throw new InvalidJSONException(e.toString());
        }
    }

    private void extractSubJSONTokenValues(String parameterDescriber, Object parameterValue) {

        if (parameterValue.getClass().getName().contains(JSON_ARRAY)) {
            JSONArray parameterArray = (JSONArray) parameterValue;

            for (int i = 0; i < parameterArray.length(); i++) {
                try {
                    logger.debug(parameterArray.get(i));
                    logger.debug(parameterArray.get(i).getClass());
                    if (parameterArray.get(i).getClass().toString().contains(JSON_OBJECT) || parameterArray.get(i).getClass().toString().contains(JSON_ARRAY)) {
                        extractSubJSONTokenValues(parameterDescriber + "[" + i + "]", parameterArray.get(i));
                    } else if (parameterArray.get(i).getClass().getTypeName().equals(JAVA_LANG_STRING)) {
                        tokenValues.add(new PermissionUtilTokenDTO((String) parameterDescriber, (String) parameterArray.get(i)));
                    }

                } catch (JSONException e) {
                    logger.error(e);
                    throw new InvalidJSONException(e.toString());
                }
            }

        }

        if (parameterValue.getClass().getName().contains(JSON_OBJECT)) {
            JSONObject parameterValueJson = (JSONObject) parameterValue;
            parameterValueJson.keys().forEachRemaining(parameterSubKey -> {
                try {
                    Object parameterSubValue = parameterValueJson.get((String) parameterSubKey);
                    logger.debug(parameterSubKey + "=" + parameterSubValue);
                    if (parameterSubValue.getClass().getName().contains(JSON_OBJECT) || parameterSubValue.getClass().getName().contains(JSON_ARRAY)) {
                        extractSubJSONTokenValues(parameterDescriber + "." + parameterSubKey, parameterSubValue);
                    } else if (parameterSubValue.getClass().getTypeName().equals(JAVA_LANG_STRING)) {
                        tokenValues.add(new PermissionUtilTokenDTO(parameterDescriber + "." + (String) parameterSubKey, (String) parameterSubValue));
                    }

                } catch (JSONException e) {
                    logger.error(e);
                    throw new InvalidJSONException(e.toString());
                }

            });
        }
    }

}
