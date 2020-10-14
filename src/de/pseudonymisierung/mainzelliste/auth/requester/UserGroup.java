package de.pseudonymisierung.mainzelliste.auth.requester;

import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import java.util.Set;


/**
 * Represents a user or usergroup which implements a Requester
 */
public class UserGroup extends Requester {

  /**
   * Creates a new User, with his permissions and authentication method, creates a random ID
   *
   * @param permission    List of the permissions
   * @param authenticator Authentication method of the User
   */
  public UserGroup(Set<String> permission,
      Authenticator authenticator) {
    super(permission, authenticator);
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
