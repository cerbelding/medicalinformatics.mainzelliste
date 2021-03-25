package de.pseudonymisierung.mainzelliste.httpsClient;


/**
 * An Interface for HTtpHeaders
 */
public interface HttpHeadersInterface<ReturnType> {

  // HTTP RequestMethod
  String getRequestMethod();

  // Returns the Header-Parameters
  ReturnType getHeaderParams();
}
