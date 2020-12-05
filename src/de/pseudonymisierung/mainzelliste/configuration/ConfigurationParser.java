package de.pseudonymisierung.mainzelliste.configuration;

import de.pseudonymisierung.mainzelliste.Servers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class ConfigurationParser {

  private static  Logger logger = Logger.getLogger(ConfigurationParser.class);

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

  public static List<String> filterConfiguration(Properties props, String prefix, boolean includeParentKey){
      Predicate<String> oicdFilter;
      if(includeParentKey){
        oicdFilter = Pattern
            .compile("^"+prefix+"\\.(.+)|"+prefix)
            .asPredicate();
      }
      else{
        oicdFilter = Pattern
            .compile("^"+prefix+"\\.(.+)")
            .asPredicate();
      }

      List<String> filteredKeys =  props.stringPropertyNames().stream().
          filter(oicdFilter).collect(Collectors.<String>toList());

    return filteredKeys;
  }

  public  static Set<String> getDynamicKeys(Properties props, String prefix, String postfix){
    return getDynamicKeys(props.stringPropertyNames().stream(), prefix, postfix);
  }

  public  static Set<String> getDynamicKeys(Map<String, String> props, String prefix, String postfix) {
    return getDynamicKeys(props.keySet().stream(), prefix, postfix);
  }

  public static Set<String> getDynamicKeys(java.util.stream.Stream<String> stream,  String prefix, String postfix){
    Predicate<String> dynamicKeyFilter = Pattern
        .compile("^"+"("+prefix+")"+"\\.(.+)"+"("+"\\."+postfix+")", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE)
        .asPredicate();

    return stream .filter(el -> !el.equals(prefix))
        .filter(dynamicKeyFilter)
        .map(s -> s.replaceAll(prefix+".", ""))
        .map(s -> s.replaceAll("."+postfix, ""))
        .collect(Collectors.toSet());

  }
}
