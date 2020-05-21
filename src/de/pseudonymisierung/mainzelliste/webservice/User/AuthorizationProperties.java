package de.pseudonymisierung.mainzelliste.webservice.User;

import java.util.Map;
import java.util.Set;

public interface AuthorizationProperties {

    Map<String, Set<String>> getProperties();
}
