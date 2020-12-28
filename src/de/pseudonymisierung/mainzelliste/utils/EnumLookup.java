package de.pseudonymisierung.mainzelliste.utils;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generic component to Lookup an Enum
 */
public class EnumLookup {

  private static final Logger logger = LogManager.getLogger(EnumLookup.class);

  /**
   * Generated an enum-instance by the given enum-value
   *
   * @param eClass Class of the Enumeration
   * @param id     the value of the Enumeration
   * @param <E>    Extends the default Enum
   * @return the instance of the Enumeration
   * @throws IOException Throws an IOException if the string could not been parsed into the Enum
   *                     instance
   */

  public static <E extends Enum<E>> E lookup(Class<E> eClass, String id) throws IOException {
    E result;
    try {
      result = Enum.valueOf(eClass, id);
    } catch (IllegalArgumentException e) {
      // log error or something here
      logger.error("Invalid value for enum " + ": " + id);
      throw new IOException(
          "Invalid value for enum " + ": " + id);
    }

    return result;
  }
}