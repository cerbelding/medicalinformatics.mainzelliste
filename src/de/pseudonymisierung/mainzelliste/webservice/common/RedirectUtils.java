package de.pseudonymisierung.mainzelliste.webservice.common;

import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.log4j.Logger;

import java.util.List;

public class RedirectUtils {


    private static Logger logger = Logger.getLogger(RedirectUtils.class);

    public static List<String> getRequestedIDsTypeFromToken(Token token){
        logger.info("getRequestedIDsTypeFromToken");
        if(token.getData().containsKey("idTypes")){
            List <String> requestedIDTypes = (List<String>) token.getDataItemList("idTypes");
            if(requestedIDTypes.size()>=1){
                return requestedIDTypes;
            }
        }
        return null;
    }

}
