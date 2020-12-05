package de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset;

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
