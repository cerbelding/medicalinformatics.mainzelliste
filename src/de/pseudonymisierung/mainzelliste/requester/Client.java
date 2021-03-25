package de.pseudonymisierung.mainzelliste.requester;

import de.pseudonymisierung.mainzelliste.auth.authenticator.Authenticator;
import java.util.Set;


/**
 * Represents a authenticated client
 */
public class Client extends Requester {


  public Client(Set<String> permissions,
      Authenticator authenticator) {
    super(permissions, authenticator);
  }

  public Client(Set<String> permissions,
      Authenticator authenticator, String sub) {
    super(permissions, authenticator, sub);
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
