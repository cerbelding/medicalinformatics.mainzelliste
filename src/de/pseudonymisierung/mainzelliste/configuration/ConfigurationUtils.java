package de.pseudonymisierung.mainzelliste.configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 * Implements reusable Configuration methods
 */

public class ConfigurationUtils {

  private final static String delimiter = "[;,]";
  private final static Logger logger = Logger.getLogger(ConfigurationUtils.class);

  /**
   * Splits a String by a delimiter (e.g.  for splitting the permissions, claims,..)
   *
   * @param value     string which should be splitted
   * @param delimiter delimiter to split the string
   * @return the splitted string as Set
   */

  public static Set<String> splitConfigurationValue(String value, String delimiter) {
    String[] splittedValues = value.split(delimiter);
    return new HashSet<>(Arrays.asList(splittedValues));
  }

  /**
   * Splits a String by a "," and ";" (e.g.  for splitting the permissions, claims,..)
   *
   * @param value string which should be splitted
   * @return the splitted string as Set
   */
  public static Set<String> splitDefaultConfigurationValue(String value) {
    return splitConfigurationValue(value, delimiter);
  }

  /**
   * Concatenates a path with its property (e.g {path}.{property})
   *
   * @param path     The path where the property should be appended
   * @param property the property which is appended to the path
   * @return the concatenated string
   */

  public static String getConcatenatedConfigurationPath(String path, String property) {
    return path + "." + property.toLowerCase();
  }

  /**
   * Returns the given configuration keys as Map
   *
   * @param props The configuration file
   * @param keys  the keys of the configuration which should been mapped to a Map
   * @return A key-value Map configuration filtered by keys
   */
  public static Map<String, String> parseConfigurationToMap(Properties props, List<String> keys) {
    Map<String, String> mappedConfiguration = new HashMap<>();
    for (String key : keys) {
      String value = props.getProperty(key);
      if (value != null) {
        mappedConfiguration.put(key, value);
      } else {
        logger.warn("Configuration value not found: " + key);
      }
    }
    return mappedConfiguration;
  }

  /**
   * Returns all child properties as List by a given prefix
   *
   * @param props            the configuration file
   * @param prefix           the prefix of the filtered keys
   * @param includeParentKey true if the parent key should be included in the returning List
   *                         otherwise false
   * @return Returns a List with all child keys
   */
  public static List<String> filterConfiguration(Properties props, String prefix,
      boolean includeParentKey) {
    Predicate<String> oidcFilter;
    if (includeParentKey) {
      oidcFilter = Pattern
          .compile("^" + prefix + "\\.(.+)|" + prefix)
          .asPredicate();
    } else {
      oidcFilter = Pattern
          .compile("^" + prefix + "\\.(.+)")
          .asPredicate();
    }

    return props.stringPropertyNames().stream().
        filter(oidcFilter).collect(Collectors.<String>toList());
  }

  /**
   * Returns all child-properties between a prefix and a postfix
   *
   * @param props   the configuration file
   * @param prefix  the prefix of the keys
   * @param postfix the postfix of the keys
   * @return All keys which has the same prefixes and postfixes
   */

  public static Set<String> getDynamicKeys(Properties props, String prefix, String postfix) {
    return getDynamicKeys(props.stringPropertyNames().stream(), prefix, postfix);
  }

  /**
   * Returns all child-properties between a prefix and a postfix
   *
   * @param props   the configuration file as Map
   * @param prefix  the prefix of the keys
   * @param postfix the postfix of the keys
   * @return All keys which has the same prefixes and postfixes
   */
  public static Set<String> getDynamicKeys(Map<String, String> props, String prefix,
      String postfix) {
    return getDynamicKeys(props.keySet().stream(), prefix, postfix);
  }

  /**
   * Returns all child-properties between a prefix and a postfix
   *
   * @param stream  the configuration file as stream
   * @param prefix  the prefix of the keys
   * @param postfix the postfix of the keys
   * @return All keys which has the same prefixes and postfixes
   */

  public static Set<String> getDynamicKeys(java.util.stream.Stream<String> stream, String prefix,
      String postfix) {
    Predicate<String> dynamicKeyFilter = Pattern
        .compile("^" + "(" + prefix + ")" + "\\.(.+)" + "(" + "\\." + postfix + ")",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
        .asPredicate();

    return stream.filter(el -> !el.equals(prefix))
        .filter(dynamicKeyFilter)
        .map(s -> s.replaceAll(prefix + ".", ""))
        .map(s -> s.replaceAll("." + postfix, ""))
        .collect(Collectors.toSet());
  }
}
