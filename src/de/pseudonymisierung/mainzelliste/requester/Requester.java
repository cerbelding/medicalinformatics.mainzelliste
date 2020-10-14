package de.pseudonymisierung.mainzelliste.requester;

import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Requester could be a UserGroup or a Server
 */

public abstract class Requester {

  /**
   * permissions of the user
   */
  protected Set<String> permissions;
  /**
   * Authentication method of the user
   */
  protected Authenticator authenticator;
  protected String id;
  protected String name;

  /**
   * Creates a new Requester, with his permissions and authentication method, creates a random ID
   *
   * @param permissions    List of the permissions
   * @param authenticator Authentication method of the User
   */
  public Requester(Set<String> permissions, Authenticator authenticator) {
    this.permissions = permissions;
    this.authenticator = authenticator;
    this.id = UUID.randomUUID().toString();
    this.name = id;
  }

  public Requester() {
  }

  /**
   * Returns a List with all permissions of the Requester
   *
   * @return List of permissions
   */
  public Set<String> getPermissions() {
    return this.permissions;
  }

  /**
   * Checks if a Requester could been authenticated
   *
   * @param authentication the claims to authenticate
   * @return true if the requester could be authenticated, false if not
   */
  public boolean isAuthenticated(Map<String, String> authentication) {
    return this.authenticator.isAuthenticated(authentication);
  }

  /**
   * Returns the id of the Requester
   *
   * @return Returns the id
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns the name of the user
   *
   * @return Returns the name
   */
  public String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return "Requester{" +
        "permissions=" + permissions +
        ", authenticator=" + authenticator +
        ", id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
