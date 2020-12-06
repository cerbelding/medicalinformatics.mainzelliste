package de.pseudonymisierung.mainzelliste.configuration.claim.operator;

import com.sun.jersey.api.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory to generate the right Operator
 */

public class OperatorFactory {

  private final Map<OperatorEnum, Supplier<Operator>> factoryMap = new HashMap<>();

  private void initFactoryMap() {
    factoryMap.put(OperatorEnum.AND, AndOperator::new);
    factoryMap.put(OperatorEnum.OR, OrOperator::new);
  }

  public OperatorFactory() {
    this.initFactoryMap();

  }

  public Operator createOperator(OperatorEnum operator) {
    Supplier<Operator> factory = factoryMap.get(operator);
    if (factory == null) {
      throw new NotFoundException("Could not parse Operator");
    }
    return factory.get();
  }
}