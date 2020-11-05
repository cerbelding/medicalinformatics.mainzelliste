package de.pseudonymisierung.mainzelliste.configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationUtils {

  public static Set<String> splitConfigurationValue(String value, String delimiter){
    String[] splittedValues = value.split(delimiter);

    return new HashSet<String>(Arrays.asList(splittedValues));
  }

  public static String getConcatedConfigurationPath(String path, String property ){
    return path+"."+property;
  }
}
