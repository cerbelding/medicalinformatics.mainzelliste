package de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset;

import java.util.List;
import java.util.Set;

public class AnySubset implements Subset {

  @Override
  public boolean validate(List<String> values, Set<String> requiredValues ) {
    for(String value: values){
      if(requiredValues.contains(value)){
        return true;
      }
    }
    return false;
  }
}
