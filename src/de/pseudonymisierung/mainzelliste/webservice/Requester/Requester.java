package de.pseudonymisierung.mainzelliste.webservice.Requester;

import java.util.Map;
import java.util.Set;

public interface Requester {


    Set<String> getPermissions();
    boolean isAuthenticated(Map<String, String> authentication);
    String getId();
    String getName();
}
