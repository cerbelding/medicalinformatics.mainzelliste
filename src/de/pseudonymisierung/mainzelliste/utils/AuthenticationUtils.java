package de.pseudonymisierung.mainzelliste.utils;

import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.auth.authenticator.AuthenticationEum;
import de.pseudonymisierung.mainzelliste.httpsClient.HttpHeaderEnum;
import de.pseudonymisierung.mainzelliste.requester.Requester;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks if the Requester could be authenticated
 */
public final class AuthenticationUtils {


  private final static Logger logger = LogManager.getLogger(AuthenticationUtils.class);

  /**
   * Parses the HTTP Header to its Authentication methods
   *
   * @param req the HTTPServletRequest
   * @return A Map with the provided Authentication methods
   */
  public static Map<AuthenticationEum, String> getAuthenticationHeader(HttpServletRequest req) {
    Map<AuthenticationEum, String> authenticationMap = new HashMap<>();
    try {
      String apiKey = req.getHeader(HttpHeaderEnum.APIKEY.getHttpHeaderKey());
      if (apiKey == null) // Compatibility to pre 1.0 (needed by secuTrial interface)
      {
        apiKey = req.getHeader(HttpHeaderEnum.APIKEY_DEPRECATED.getHttpHeaderKey());
      }

      if (apiKey != null) {
        authenticationMap.put(AuthenticationEum.APIKEY, apiKey);
      }
      String authorizationHeader = req.getHeader(HttpHeaderEnum.AUTHORIZATION.getHttpHeaderKey());
      if (authorizationHeader != null) {
        String token = authorizationHeader.split(" ")[1];
        if (token != null) {
          authenticationMap.put(AuthenticationEum.ACCESS_TOKEN, token);
        }
      }
    } catch (Exception e) {
      logger.error(e);
      return authenticationMap;
    }
    return authenticationMap;
  }


  /**
   * Returns the authenticated Requester the httpHeader MUST contain either the APIKey or the access
   * token
   *
   * @param httpHeader The HttpHeader of the Request
   * @return the founded requester otherwise null
   */
  public static Requester authenticate(Map<AuthenticationEum, String> httpHeader) {
    if(httpHeader.containsKey(AuthenticationEum.APIKEY) && httpHeader.containsKey(AuthenticationEum.ACCESS_TOKEN) ){
      return null;
    }
    else if (httpHeader.containsKey(AuthenticationEum.APIKEY)) {
      String apiKey = httpHeader.get(AuthenticationEum.APIKEY);
      return Servers.instance.getRequesterByAPIKey(apiKey);
    } else if (httpHeader.containsKey(AuthenticationEum.ACCESS_TOKEN)) {
      String accessToken = httpHeader.get(AuthenticationEum.ACCESS_TOKEN);
      return Servers.instance.getRequesterByAccessToken(accessToken);
    } else {
      return null;
    }
  }
}
