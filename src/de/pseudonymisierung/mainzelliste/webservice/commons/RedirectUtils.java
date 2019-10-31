package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RedirectUtils {


    private static Logger logger = Logger.getLogger(RedirectUtils.class);

    public static List<String> getRequestedIDsTypeFromToken(Token token) {
        logger.info("getRequestedIDsTypeFromToken");
        if (token.getData().containsKey("idTypes")) {
            List<String> requestedIDTypes = (List<String>) token.getDataItemList("idTypes");
            if (requestedIDTypes.size() >= 1) {
                return requestedIDTypes;
            }
        } else if (token.getData().containsKey("searchIds")) {
            ArrayList<HashMap> searchIds = (ArrayList) token.getData().get("searchIds");
            List<String> requestedIDType = new ArrayList<>();

            int i = searchIds.size() - 1;
            do {
                requestedIDType.add(String.valueOf(searchIds.get(i).get("idType")));
                i--;
            } while (i >= 0);
            return requestedIDType;
        }

        return null;
    }

}
