package de.pseudonymisierung.mainzelliste.webservice.User;

import de.pseudonymisierung.mainzelliste.webservice.Authorizator.Authorizator;
import de.pseudonymisierung.mainzelliste.webservice.Authorizator.OICDAuthorizator;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Represents a user or usergroup
 */
public class User {

    private final Logger logger = Logger.getLogger(OICDAuthorizator.class);
    /** permissions of the user */
    protected Set<String> permissions;
    /** authorization method of the user */
    protected Authorizator authorizator;


    public User(Set<String> permission, Authorizator authorizator) {
        this.permissions = permission;
        this.authorizator = authorizator;
    }

    /**
     * Checks if a user could be authenticated
     * @param claims the authorization properties from the user
     * @return true is the user could be authenticated, otherwise falses
     */
    public boolean isAuthenticated(Map<String, String> claims) {
        return authorizator.isAuthenticated(claims);
    }
    /**
     * Checks if the user has permission to the requested resource
     * @param claims the authorization properties from the user
     * @param permission the permission resource the user is request to
     * @return true is the user could be authenticated, otherwise false
     */
     public boolean hasPermission(Map<String, String> claims, String permission) {
        if (authorizator.isAuthenticated(claims) && this.permissions.contains(permission)) {
            return true;
        } else {
            logger.info("Permission of User rejected");
            return false;
        }
    }



}
