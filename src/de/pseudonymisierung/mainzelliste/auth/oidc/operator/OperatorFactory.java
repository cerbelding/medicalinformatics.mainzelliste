package de.pseudonymisierung.mainzelliste.auth.oidc.operator;

import com.sun.jersey.api.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OperatorFactory {
  private Map<OperatorEnum, Supplier<Operator>> factoryMap = new HashMap();

  private void initFactoryMap(){
    factoryMap.put(OperatorEnum.AND, () -> new AndOperator());
    factoryMap.put(OperatorEnum.OR, () -> new OrOperator());
  }

  public OperatorFactory(){
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