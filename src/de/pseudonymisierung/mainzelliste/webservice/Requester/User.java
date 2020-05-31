package de.pseudonymisierung.mainzelliste.webservice.Requester;

import de.pseudonymisierung.mainzelliste.webservice.Authenticator.Authenticator;
import de.pseudonymisierung.mainzelliste.webservice.Authenticator.OICDAuthenticator;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a user or usergroup
 */
public class User implements Requester {

    private final Logger logger = Logger.getLogger(OICDAuthenticator.class);
    /** permissions of the user */
    protected Set<String> permissions;
    /** authorization method of the user */
    protected Authenticator authenticator;
    protected String id;
    protected String name;

    public User(Set<String> permission, Authenticator authenticator) {
        this.permissions = permission;
        this.authenticator = authenticator;
        this.id = UUID.randomUUID().toString();
        this.name = id;
    }

    public User(Set<String> permission, Authenticator authenticator, String id) {
        this.permissions = permission;
        this.authenticator = authenticator;
        if(id != null){
            this.id = id;
        }
        else{
            this.id = UUID.randomUUID().toString();
        }

    }

    /**
     * Checks if a user could be authenticated
     * @param claims the authorization properties from the user
     * @return true is the user could be authenticated, otherwise falses
     */
    public boolean isAuthenticated(Map<String, String> claims) {
        return authenticator.isAuthenticated(claims);
    }

    @Override
    /**
     * Returns the id
     * @return
     */
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Checks if the user has permission to the requested resource
     * @param claims the authorization properties from the user
     * @param permission the permission resource the user is request to
     * @return true is the user could be authenticated, otherwise false
     */
     public boolean hasPermission(Map<String, String> claims, String permission) {
        if (authenticator.isAuthenticated(claims) && this.permissions.contains(permission)) {
            return true;
        } else {
            logger.info("Permission of User rejected");
            return false;
        }
    }

    /**
     * Return the permission from the User
     * @return the permissions
     */
    public Set<String> getPermissions(){
         return this.permissions;
    }



    public Set<String> getPermissions(Map<String, String> authentication){
        Set<String> permissions = authenticator.isAuthenticated(authentication) ?  getPermissions(): new HashSet<String>();
        return permissions;

    }

}
