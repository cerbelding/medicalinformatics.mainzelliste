package de.pseudonymisierung.mainzelliste.webservice.Authenticator;

import java.util.Set;

public class OICDIDToken {
    private String sub;
    private Set<String> roles;

    public OICDIDToken(String sub, Set<String> roles){
        this.sub=sub;
        this.roles = roles;
    }


}
