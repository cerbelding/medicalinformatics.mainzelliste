package de.pseudonymisierung.mainzelliste.auth.httpsClient;


/**
 * An Interface for HTtpHeaders
 */
public interface HttpHeadersInterface<ReturnType> {

  // HTTP RequestMethod
  String getRequestMethod();

  // Returns the Header-Parameters
  ReturnType getHeaderParams();
}
