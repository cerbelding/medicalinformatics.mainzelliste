package de.pseudonymisierung.mainzelliste.configuration.claim.subset;

import com.sun.jersey.api.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Factory to generate the right Subset instance
 */

public class SubsetFactory {

  private Map<SubsetEnum, Supplier<Subset>> factoryMap = new HashMap();

  private void initFactoryMap() {
    factoryMap.put(SubsetEnum.ALL, () -> new AllSubset());
    factoryMap.put(SubsetEnum.ANY, () -> new AnySubset());
  }

  public SubsetFactory() {
    this.initFactoryMap();

  }

  public Subset createSubset(SubsetEnum subset) {
    Supplier<Subset> factory = factoryMap.get(subset);
    if (factory == null) {
      throw new NotFoundException("Could not parse Operator");
    }
    return factory.get();
  }
}