package de.pseudonymisierung.mainzelliste.webservice.User;

import de.pseudonymisierung.mainzelliste.webservice.Authorizator.Authorizator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class User<T> {

    protected Set<String> permissions;
    protected Authorizator authorizator;



    public User(Set<String> permission, AuthorizationProperties authProps){
        this.permissions = permission;
          }

    public Set<String> getPermissions(Map<String,String> claims) {
        if(authorizator.hasPermission(claims)){
            return permissions;
        }
        else {
            return new HashSet<>();
        }
    }
}
