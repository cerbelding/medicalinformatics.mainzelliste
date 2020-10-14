package de.pseudonymisierung.mainzelliste.auth.httpsClient;

import org.apache.commons.net.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Represent the HTTP Authorization Methods
 */
public class HttpClientAuthorization {

  private final String authorization;


  public HttpClientAuthorization(String authorization) {
    this.authorization = authorization;

  }

  public String toString() {
    return this.authorization;
  }

  /**
   * Creates a Bearer Authorization
   *
   * @param bearerToken the bearertoken
   * @return returns an Instance of the HTTTpClientAuthorization
   */
  public static HttpClientAuthorization createBearerAuthentication(String bearerToken) {
    return new HttpClientAuthorization("Bearer " + bearerToken);

  }

  /**
   * Creates a Basic Authorization
   *
   * @param username username
   * @param password password
   * @return returns an Instance of the HTTTpClientAuthorization
   */
  public static HttpClientAuthorization createBasicAuthentication(String username,
      String password) {
    String auth = username + ":" + password;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
    return new HttpClientAuthorization("Basic " + new String(encodedAuth));
  }
}
