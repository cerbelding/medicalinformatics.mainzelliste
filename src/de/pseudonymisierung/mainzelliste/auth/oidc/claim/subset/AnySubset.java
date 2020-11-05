package de.pseudonymisierung.mainzelliste.auth.oicd.claim.subset;

import java.util.List;

public class AnySubset implements Subset {

  @Override
  public boolean validate(List<String> values, List<String> requiredValues ) {
    for(String value: values){
      if(requiredValues.contains(value)){
        return true;
      }
    }
    return false;
  }
}
