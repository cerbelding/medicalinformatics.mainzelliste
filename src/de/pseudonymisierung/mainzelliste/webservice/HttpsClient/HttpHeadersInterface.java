package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import java.util.Map;

/**
 * An Interface for HTtpHeaders
 */
public interface HttpHeadersInterface<ReturnType> {

    // HTTP RequestMethod
    String getRequestMethod();
    // Returns the Headerparameters
    ReturnType getHeaderParams();
}
