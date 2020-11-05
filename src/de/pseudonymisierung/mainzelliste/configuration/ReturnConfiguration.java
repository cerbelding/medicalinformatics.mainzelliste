package de.pseudonymisierung.mainzelliste.configuration;

import java.util.HashMap;
import java.util.Map;

public class ReturnConfiguration {
  private Map<String,Object> configurationMap = new HashMap<>();

  public ReturnConfiguration(Map<String,Object> configurationMap){
    this.configurationMap = configurationMap;
  }

  public Map<String, Object> getConfigurationMap() {
    return configurationMap;
  }
}
