package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.webservice.Token;

public class PermissionUtil {
    public static boolean checkTokenPermission(Token token) {
        boolean valid = true;
        // TODO: Check if resultFields, resultIds, any other thing inside token is valid !!!!!!!!!!!
        // TODO: see above
        // TODO: see above
        // TODO: see above
        // TODO: see above
        return valid;
    }

    public static boolean checkTokenPermission(String tokenId) {
        Token token = Servers.instance.getTokenByTid(tokenId);
        return checkTokenPermission(token);
    }
}
