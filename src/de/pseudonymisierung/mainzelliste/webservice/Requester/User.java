package de.pseudonymisierung.mainzelliste.webservice.Requester;

import de.pseudonymisierung.mainzelliste.webservice.Authenticator.Authenticator;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a user or usergroup which implements a Requester
 */
public class User implements Requester {

    /** permissions of the user */
    protected Set<String> permissions;
    /** Authentication method of the user */
    protected Authenticator authenticator;
    protected String id;
    protected String name;

    /**
     * Creates a new User, with his permissions and authentication method, creates a random ID
     * @param permission List of the permissions
     * @param authenticator Authentication method of the User
     */
    public User(Set<String> permission, Authenticator authenticator) {
        this.permissions = permission;
        this.authenticator = authenticator;
        this.id = UUID.randomUUID().toString();
        this.name = id;
    }

    /**
     * Creates a new User, with his permissions, authentication method and a ID
     * @param permission List of the permission
     * @param authenticator Authentication method of the User
     * @param id Id of the user
     */
    public User(Set<String> permission, Authenticator authenticator, String id) {
        this.permissions = permission;
        this.authenticator = authenticator;
        this.id = id;
    }

    /**
     * Checks if a user could be authenticated
     * @param claims the authorization properties from the user
     * @return true is the user could be authenticated, otherwise false
     */
    public boolean isAuthenticated(Map<String, String> claims) {
        return authenticator.isAuthenticated(claims);
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Returns the name of the user
     * @return Returns the name
     */
    @Override
    public String getName() {
        return this.name;
    }


    /**
     * Return the permission from the User
     * @return the permissions
     */
    public Set<String> getPermissions(){
         return this.permissions;
    }



    @Override
    public String toString() {
        return "User{" +
                "permissions=" + permissions +
                ", authenticator=" + authenticator +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
