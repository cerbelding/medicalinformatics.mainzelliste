package de.pseudonymisierung.mainzelliste.webservice.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OICDProperties implements AuthorizationProperties {

    private Set<String> subs;
    private Set<String> roles;


    @Override
    public Map<String, Set<String>> getProperties() {
        Map<String, Set<String>> properties = new HashMap<String, Set<String>>();
        properties.put("subs", subs);
        properties.put("roles", roles);
        return properties;
    }
}
