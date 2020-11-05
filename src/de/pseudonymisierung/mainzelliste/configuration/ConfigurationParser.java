package de.pseudonymisierung.mainzelliste.configuration;

import de.pseudonymisierung.mainzelliste.Servers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class ConfigurationParser {

  private static  Logger logger = Logger.getLogger(Servers.class);


  private ConfigurationParser(){
  }


  public static Map<String,String> parseConfigurationToMap(Properties props, List<String> keys){
    Map<String, String> mappedConfiguration = new HashMap<>();
    for(String key: keys){
      String value = props.getProperty(key);
      if(value != null){
        mappedConfiguration.put(key, value);
      }
      else{
        logger.warn("Configurationvalue not found: "+ key);
      }
    }
    return mappedConfiguration;
  }

  public static List<String> filterConfiguration(Properties props, String prefix){
      Predicate<String> oicdFilter = Pattern
          .compile("^"+prefix+"\\.(.+)")
          .asPredicate();

      List<String> filteredKeys =  props.stringPropertyNames().stream().
          filter(oicdFilter).collect(Collectors.<String>toList());

    return filteredKeys;
  }

  public  static List<String> getDynamicKeys(Properties props, String prefix, String postfix){
    Predicate<String> oicdNameFilter = Pattern
        .compile("^"+prefix+"\\.(.+)\\."+postfix)
        .asPredicate();

    List<String> keyList =  props.stringPropertyNames().stream().
        filter(oicdNameFilter)
        .map(s -> s.replaceAll("oicd.", ""))
        .map(s -> s.replaceAll(".iss", ""))
        .collect(Collectors.<String>toList());

    return keyList;
  }
}
