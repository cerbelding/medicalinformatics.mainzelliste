package de.pseudonymisierung.mainzelliste.utils;

import de.pseudonymisierung.mainzelliste.auth.ClaimConfigurationParser;
import org.apache.log4j.Logger;

public class EnumLookup {

  private static Logger logger = Logger.getLogger(EnumLookup.class);

  public static <E extends Enum<E>> E lookup(Class<E> eClass, String id) {
    E result;
    try {
      result = Enum.valueOf(eClass, id);
    } catch (IllegalArgumentException e) {
      // log error or something here
      logger.error("Error parsing Enum");
      throw new RuntimeException(
          "Invalid value for enum " + ": " + id);
    }

    return result;
  }
}