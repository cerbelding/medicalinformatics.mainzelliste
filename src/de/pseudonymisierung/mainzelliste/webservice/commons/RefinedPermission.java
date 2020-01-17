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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RefinedPermission {

    final static private String JAVA_LANG_STRING = "java.lang.String";
    final static private String JSON_OBJECT = "JSONObject";
    final static private String JSON_ARRAY = "JSONArray";


    final private static Logger logger = Logger.getLogger(RefinedPermission.class);

    private List<RefinedPermissionDTO> tokenValues = new ArrayList<>();

    private String returnMessage = "";


    public static boolean checkTokenPermission(String tokenId) {
        logger.warn("//TODO: will be obsolete!");
        return true;
    }

    public static boolean checkPermission(Token token) {
        logger.warn("//TODO: will be obsolete!");
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

    private boolean compareRequestAndPermissions(List<RefinedPermissionDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("compareRequestAndPermissions" + "requestedPermissions: " + requestedPermissions + " serverPermissions" + serverPermissions);

        if (requestedPermissions.stream().filter(tokenRequest -> tokenRequest.getRequestedParameter().equals("type")).map(RefinedPermissionDTO::getRequestedValue).count() == 1) {
            String requestedTokenType = requestedPermissions.stream().filter(tokenRequest -> tokenRequest.getRequestedParameter().equals("type")).map(RefinedPermissionDTO::getRequestedValue).findFirst().orElse("noElementFound");
            if (!validateTokeType(serverPermissions, requestedTokenType)) {
                this.setReturnMessage("token type " + requestedTokenType + " not allowed");
                logger.debug(this.getReturnMessage());
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

        return allowedTokenTypes.stream().anyMatch(str -> str.contains(requestedTokenType));
    }

    private boolean validateFurtherTokenPermissions(String requestedTokenType, List<RefinedPermissionDTO> requestedPermissions, Set<String> serverPermissions) {

        if (serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).contains("tt_" + requestedTokenType)) {
            //TODO: check on Mainzelliste boot up
            logger.warn(requestedTokenType + " is configured with (too) extensive rights, maybe you can limit them.");
            return true;
        } else if (serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).count() == 1) {
            logger.debug("validateFurtherTokenPermissions: " + requestedTokenType);

            if (!validateTokenParameterAgainstServerParameter(requestedTokenType, requestedPermissions, serverPermissions)) {
                return false;
            }
            return validateTokenValuesAgainstServerValues(requestedTokenType, requestedPermissions, serverPermissions);
        }
        logger.debug("missing permission: " + requestedTokenType);
        return false;
    }

    private boolean validateTokenParameterAgainstServerParameter(String requestedTokenType, List<RefinedPermissionDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("validateTokenParameterAgainstServerParameter");

        //TODO: replace "{" and "}" with first and last replace - didn't work yet
        String detailedServerPermissionsForRequestedTokenType = serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).get(0).replace("tt_" + requestedTokenType, "").replace("{", "").replace("}", "");

        AtomicBoolean returnValue = new AtomicBoolean(true);
        requestedPermissions.forEach(r -> {
            if (!detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter().replaceAll("[0-9]*", "").replaceAll("\\[", "").replaceAll("]", "")) && !r.getRequestedParameter().equals("type")) {
                this.setReturnMessage(r.getRequestedParameter() + " is not in server permissions ");
                logger.info(this.getReturnMessage() + detailedServerPermissionsForRequestedTokenType);
                returnValue.set(false);
            }
        });

        return returnValue.get();
    }

    private boolean validateTokenValuesAgainstServerValues(String requestedTokenType, List<RefinedPermissionDTO> requestedPermissions, Set<String> serverPermissions) {
        logger.debug("validateTokenValuesAgainstServerValues");

        //TODO: replace "{" and "}" with first and last replace - didn't work yet
        String detailedServerPermissionsForRequestedTokenType = serverPermissions.stream().filter(o -> o.startsWith("tt_" + requestedTokenType)).collect(Collectors.toList()).get(0).replace("tt_" + requestedTokenType, "").replace("{", "").replace("}", "");

        String matchingGroup = null;

        for (RefinedPermissionDTO r : requestedPermissions) {
            if (r.getRequestedParameter().matches(".*\\[[][0-9]*\\[]].*")) {

                Pattern pattern = Pattern.compile("(.*[0-9]*\\]).*");

                Matcher matcher = pattern.matcher(r.getRequestedParameter());
                matcher.matches();

                if (matchingGroup == null || !matchingGroup.equals(matcher.group(1))) {
                    logger.info("Call check " + requestedPermissions.stream().filter(e -> e.getRequestedParameter().contains(matcher.group(1))).collect(Collectors.toList()));

                    Pattern patternCut = Pattern.compile(".*(\\[[][0-9]*\\[]]).*");
                    Matcher cut = patternCut.matcher(r.getRequestedParameter());
                    cut.matches();

                    for (RefinedPermissionDTO isAllowed : requestedPermissions.stream().filter(e -> e.getRequestedParameter().contains(matcher.group(1))).collect(Collectors.toList())) {
                        if (!isParameterValueCombinationAllowed(isAllowed.getRequestedParameter().replace(cut.group(1), ""), isAllowed.getRequestedValue(), detailedServerPermissionsForRequestedTokenType)) {
                            return false;
                        }
                    }

                } else {
                    logger.debug(matchingGroup);
                }

                //Call check
                matchingGroup = matcher.group(1);

                logger.info("multiple existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter().replaceAll("[0-9]*", "").replaceAll("\\[", "").replaceAll("]", "")));
            } else {
                logger.info("single existing values: " + r.getRequestedParameter() + ":" + r.getRequestedValue() + " " + detailedServerPermissionsForRequestedTokenType.contains(r.getRequestedParameter()));
                if (!isParameterValueCombinationAllowed(r.getRequestedParameter(), r.getRequestedValue(), detailedServerPermissionsForRequestedTokenType)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isParameterValueCombinationAllowed(String requestedParameter, String requestedValue, String tokenTypeServerPermissions) {
        String requestedParameterAndValue = requestedParameter + ":" + requestedValue;
        String functionName = "isParameterValueCombinationAllowed()";

        logger.info(functionName + " request " + requestedParameterAndValue);

        List<String> tokenTypeServerPermissionsList = Arrays.asList(tokenTypeServerPermissions.split("\\|"));
        List<String> tokenTypeServerPermissionsListWildCard = tokenTypeServerPermissionsList.stream().filter(o -> o.contains("*")).collect(Collectors.toList());

        if (requestedParameter.equals("type")) {
            return true;
        } else if (tokenTypeServerPermissionsList.contains(requestedParameterAndValue)) {
            logger.debug(functionName + requestedParameterAndValue + " is exactly like in config");
            return true;
        } else if (isValueInAmpersandConfigPart(tokenTypeServerPermissionsList, requestedParameter, requestedValue)) {
            return true;
        } else if (tokenTypeServerPermissionsListWildCard.stream().anyMatch(o -> o.contains(requestedParameter + ":"))) {
            logger.debug(functionName + requestedParameterAndValue + " matches wildcard in config");
            return true;
        }
        this.setReturnMessage(requestedParameterAndValue + " is not in config!");
        logger.info(functionName + " " + this.getReturnMessage());

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
                    if (parameterValue.getClass().getTypeName().equals(JAVA_LANG_STRING)) {
                        tokenValues.add(new RefinedPermissionDTO((String) parameterKey, (String) parameterValue));
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
                        tokenValues.add(new RefinedPermissionDTO(parameterDescriber, (String) parameterArray.get(i)));
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
                        tokenValues.add(new RefinedPermissionDTO(parameterDescriber + "." + parameterSubKey, (String) parameterSubValue));
                    }

                } catch (JSONException e) {
                    logger.error(e);
                    throw new InvalidJSONException(e.toString());
                }

            });
        }
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    private void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

}
