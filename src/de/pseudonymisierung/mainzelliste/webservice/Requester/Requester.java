package de.pseudonymisierung.mainzelliste.webservice.Requester;

import java.util.Map;
import java.util.Set;

/**
 * Represents a Requester could be a User or a Server
 */

public interface Requester {

    /**
     * Returns a List with all permissions of the Requester
     * @return List of permissions
     */
    Set<String> getPermissions();

    /**
     * Checks if a Requester could been authenticated
     * @param authentication the claims to authenticate
     * @return true if the requester could be authenticated, false if not
     */
    boolean isAuthenticated(Map<String, String> authentication);
    String getId();
    String getName();
}
