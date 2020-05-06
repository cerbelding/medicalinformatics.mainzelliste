package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implement a HTTPsClient
 */

public class HttpsClient extends AbstractHttpsClient<HttpHeadersInterface<Map<String,String>>, HttpUrlParametersInterface<String>, JSONObject> {


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



    @Override
    public JSONObject request(HttpHeadersInterface<Map<String,String>> httpHeadersImpl, HttpUrlParametersInterface<String> httpUrlParameterBuilder) {
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

            reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String jsonText = readAll(reader);
            json = new JSONObject(jsonText);
            con.disconnect();


        } catch (MalformedURLException e) {
            logger.error("Error to extrakt the URL");
            return json;
        } catch (IOException e) {
            logger.error("Error to open a Connection");
            return json;
        } catch (JSONException e) {
            logger.error("Error parsing JSON");
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