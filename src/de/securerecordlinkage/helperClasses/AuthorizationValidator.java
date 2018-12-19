package de.securerecordlinkage.helperClasses;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthorizationValidator {

    private Map<String, List<String>> allowedAuthTypesAndValues = new HashMap<>();
    private static Logger logger = Logger.getLogger("de.securerecordlinkage.helperClasses.AuthorizationValidator");

    public AuthorizationValidator(Map<String, List<String>> allowedAuthTypesAndValues){
        this.allowedAuthTypesAndValues = allowedAuthTypesAndValues;

    }

    public boolean validate(HttpServletRequest request){

        if (allowedAuthTypesAndValues.get("apiKey") != null){
            if(apiKeyValidation(request, allowedAuthTypesAndValues) == true){
                return true;
            }
        }


        return false;
    }

    private boolean apiKeyValidation(HttpServletRequest request, Map<String, List<String>> allowedAuthTypesAndValues) {

        logger.info("authorizationValidator() " + "validate ApiKey");

        List<String> apiKeys = allowedAuthTypesAndValues.get("apiKey");
        String authHeader;

        try {
            authHeader = request.getHeader("Authorization");
        } catch (Exception e) {
            logger.error("Failed getting Authorization Header. " + e.toString());
            return false;
        }

        if (authHeader == null) {
            logger.info("Can't find ApiKey in request authHeader==null");
            return false;
        }

        for (String apiKey : apiKeys){
            if (authHeader.equals("apiKey apiKey=\""+apiKey + "\"")) {
            //if (authHeader.equals("apiKey apiKey=" + apiKey + "")) {

                logger.info("ApiKey correct");
                return true;
            }
        }

        logger.info("Wrong ApiKey!");
        return false;

    }


}
