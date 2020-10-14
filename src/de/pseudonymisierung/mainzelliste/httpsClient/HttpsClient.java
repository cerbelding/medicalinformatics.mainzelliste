package de.pseudonymisierung.mainzelliste.httpsClient;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.*;
import javax.ws.rs.HttpMethod;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Implement a HTTPsClient
 */

public class HttpsClient implements
    HttpsClientInterface<HttpHeadersInterface<Map<String, String>>, JSONObject> {

  private static final Logger logger = Logger.getLogger(HttpsClient.class);
  private Proxy proxy;


  /**
   * Constructor
   */
  public HttpsClient() {
  }

  public HttpsClient(Proxy proxy) {
    this.proxy = proxy;
  }


  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  // Only for testing
  private static void disableSslVerification() {
    try {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      };

      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      // Install the all-trusting host verifier
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }

  @Override
  public JSONObject request(String urlPath,
      HttpHeadersInterface<Map<String, String>> httpHeadersImpl) throws IOException {
    logger.info("Try to request " + urlPath + " with Http-header: " + httpHeadersImpl.toString()
        + " and url parameters ");
    JSONObject json = new JSONObject();
    HttpsURLConnection con = null;
    BufferedReader reader = null;

    try {
      URL url = new URL(urlPath);
      con = proxy != null ? (HttpsURLConnection) url.openConnection(proxy)
          : (HttpsURLConnection) url.openConnection();
      setHeader(con, httpHeadersImpl);
      con.setConnectTimeout(1000);
      logger.info(con.getRequestMethod());
      con.connect();
      int code = con.getResponseCode();
      logger.info("Response code: " + code);

      if (code >= 200 && code <= 300) {
        reader = new BufferedReader(
            new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        logger.info("Read: " + reader.toString());
        String jsonText = readAll(reader);
        json = new JSONObject(jsonText);
      }

      con.disconnect();
      return json;

    } catch (MalformedURLException e) {
      logger.error("Error to extrakt the URL");
      logger.error(e);
      throw new IOException(e);
    } catch (IOException e) {
      logger.error("Error to open a Connection");
      logger.error(e);
      throw new IOException(e);
    } catch (JSONException e) {
      logger.error("Error parsing JSON");
      logger.error(e);
      throw new IOException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          logger.error("Error closing BufferReader");
        }
      }
      if (con != null) {
        con.disconnect();
      }
    }
  }

  private void setHeader(HttpsURLConnection con,
      HttpHeadersInterface<Map<String, String>> httpHeader) throws ProtocolException {
    String httpMethod = httpHeader.getRequestMethod();
    switch (httpMethod) {
      case HttpMethod.GET:
        con.setDoInput(true);
        break;
      default:
        con.setDoInput(true);
        break;
    }
    //con.setRequestMethod(httpMethod);
    for (Map.Entry<String, String> entry : httpHeader.getHeaderParams().entrySet()) {
      con.setRequestProperty(entry.getKey(), entry.getValue());
    }
  }

}