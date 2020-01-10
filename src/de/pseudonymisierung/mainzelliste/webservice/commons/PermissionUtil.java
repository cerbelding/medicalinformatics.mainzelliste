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
import java.util.Arrays;
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
                logger.debug("token type " + requestedTokenType + " not allowed");
                return false;
            } else if (!validateFurtherTokenPermissions(requestedTokenType, requestedPermissions, serverPermissions)) {
                logger.debug("not enough permissions");
                return false;
            } else {
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

        if (serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).contains("tt_" + requestedTokenType)) {
            //TODO: check on Mainzelliste boot up
            logger.warn(requestedTokenType + " is configured with (too) extensive rights, maybe you can limit them.");
            return true;
        } else if (serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).count() == 1) {
            logger.debug("validateFurtherTokenPermissions: " + requestedTokenType);

            if (!validateTokenParameterAgainstServerParameter(requestedTokenType, requestedPermissions, serverPermissions)) {
                return false;
            }
            if (!validateTokenValuesAgainstServerValues(requestedTokenType, requestedPermissions, serverPermissions)) {
                return false;
            } else {
                return true;
            }
        }
        logger.debug("missing permission: " + requestedTokenType);
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

    private boolean validateTokenValuesAgainstServerValues(String requestedTokenType, List<PermissionUtilTokenDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("validateTokenValuesAgainstServerValues");

        //TODO: replace "{" and "}" with first and last replace - didn't work yet
        String detailedServerPermissionsForRequestedTokenType = serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).get(0).replace("tt_" + requestedTokenType, "").replace("{", "").replace("}", "");


        for (PermissionUtilTokenDTO r : requestedPermissions) {
            if (r.getRequestedParameter().matches(".*\\[[\\][0-9]*\\[]\\].*")) {
                //TODO: Check multiple values
                logger.info("multiple existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter().replaceAll("[0-9]*", "").replaceAll("\\[", "").replaceAll("\\]", "")));
            } else {
                logger.info("single existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter()));
                if (!isParameterValueCombinationAllowed(r.getRequestedParameter(), r.getRequestedValue(), requestedTokenType, detailedServerPermissionsForRequestedTokenType)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isParameterValueCombinationAllowed(String requestedParameter, String requestedValue, String requestedTokenType, String tokenTypeServerPermissions) {
        String requestedParameterAndValue = requestedParameter + ":" + requestedValue;
        logger.info("isParameterValueCombinationAllowed request " + requestedParameterAndValue);
        //TODO: hier gehts weiter. checken der param=Value abfragen
        List<String> tokenTypeServerPermissionsList = Arrays.asList(tokenTypeServerPermissions.split("\\|"));
        List<String> tokenTypeServerPermissionsListWildCard = tokenTypeServerPermissionsList.stream().filter(o -> o.contains("*")).collect(Collectors.toList());

        if (requestedParameter.equals("type")) {
            return true;
        } else if (tokenTypeServerPermissionsList.contains(requestedParameterAndValue)) {
            logger.debug("isParameterValueCombinationAllowed() " + requestedParameterAndValue + " is exactly like in config");
            return true;
        } else if (isValueInAmpersandConfigPart(tokenTypeServerPermissionsList, requestedParameter, requestedValue)) {
            return true;
        } else if (tokenTypeServerPermissionsListWildCard.stream().anyMatch(o -> o.contains(requestedParameter + ":"))) {
            logger.debug("isParameterValueCombinationAllowed() " + requestedParameterAndValue + " matches wildcard in config");
            return true;
        }
        logger.info("isParameterValueCombinationAllowed() " + requestedParameterAndValue + " is not in config!");

        return false;
    }

    private boolean isValueInAmpersandConfigPart(List<String> tokenTypeServerPermissionsList, String requestedParameter, String requestedValue) {

        //containing in configuration
        if (!tokenTypeServerPermissionsList.stream().filter(o -> o.contains(requestedParameter)).collect(Collectors.toList()).stream().filter(o -> o.contains(requestedValue)).collect(Collectors.toList()).isEmpty()) {

            //check if exact value is in the config
            List<String> matchingConfigs = tokenTypeServerPermissionsList.stream().filter(o -> o.contains(requestedParameter)).collect(Collectors.toList()).stream().filter(o -> o.contains(requestedValue)).collect(Collectors.toList());
            boolean allowedValue = true;
            for (String configMatches : matchingConfigs) {
                String configMatchesValues = configMatches.replace(requestedParameter + ":", "");

                List<String> ampersandSplits = Arrays.asList(configMatchesValues.split("&"));
                if (!ampersandSplits.contains(requestedValue)) {
                    allowedValue = false;
                }
            }

            return allowedValue;
        }
        return false;
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
                    //logger.debug(parameterKey + "=" + parameterValue);
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
