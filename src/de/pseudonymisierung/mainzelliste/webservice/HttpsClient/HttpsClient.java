package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.*;
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

public class HttpsClient extends AbstractHttpsClient<HttpHeadersInterface<Map<String,String>>, HttpUrlParametersInterface<String>, JSONObject> {
    private static final Logger logger = Logger.getLogger(HttpsClient.class);


    /**
     * Constructor
     * @param url The Url to send a request
     */
    public HttpsClient(String url) {
        super(url);
    }

    public HttpsClient(String url, Proxy proxy) {
        super(url, proxy);
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
        try
        {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
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
    public JSONObject request(HttpHeadersInterface<Map<String,String>> httpHeadersImpl, HttpUrlParametersInterface<String> httpUrlParameterBuilder) {
        logger.info("Disable ssl");
        disableSslVerification();
        logger.info("Try to request " + this.url + " with Http-header: "+ httpHeadersImpl.toString()+ " and url parameters "+ httpUrlParameterBuilder.toString() );
        JSONObject json = new JSONObject();
        HttpsURLConnection con = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(this.url);
            con = proxy != null ? (HttpsURLConnection) url.openConnection(proxy) : (HttpsURLConnection) url.openConnection();
            setHeader(con, httpHeadersImpl);
            setUrlParams(con, httpUrlParameterBuilder);
            con.setConnectTimeout(1000);
            con.connect();
            int code = con.getResponseCode();
            logger.info("Response code: "+ code);

            if(code>= 200 && code <= 300){
                reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                logger.info("Read: "+ reader.toString());
                String jsonText = readAll(reader);
                json = new JSONObject(jsonText);
            }

            con.disconnect();


        } catch (MalformedURLException e) {
            logger.error("Error to extrakt the URL");
            logger.error(e);
            return json;
        } catch (IOException e) {
            logger.error("Error to open a Connection");
            logger.error(e);
            return json;
        } catch (JSONException e) {
            logger.error("Error parsing JSON");
            logger.error(e);
            return json;
        } finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("Error closing BufferReader");

                }
            }
            if(con != null) {
                con.disconnect();
            }
        }
        return json;
    }

    private void setHeader(HttpsURLConnection con, HttpHeadersInterface<Map<String, String>> httpHeader) throws ProtocolException {
        con.setRequestMethod(httpHeader.getRequestMethod());
        for (Map.Entry<String, String> entry : httpHeader.getHeaderParams().entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private void setUrlParams(HttpsURLConnection con, HttpUrlParametersInterface<String> urlParameterBuilder) {
        try {
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(urlParameterBuilder.getParamsString());
        out.flush();
        out.close();
        }
        catch(IOException e){

        }
    }

}