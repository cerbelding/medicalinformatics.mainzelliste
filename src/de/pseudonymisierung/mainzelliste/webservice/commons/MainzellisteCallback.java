package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.webservice.AddPatientToken;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainzellisteCallback {

    /** The logging instance */
    private Logger logger = Logger.getLogger(this.getClass());

    /** The TLS context depending on the configuration parameters */
    private SSLConnectionSocketFactory sslsf;

    private Servers.ApiVersion apiVersion;

    /**
     * represents the callback done by the request
     *   TODO: describe behaviour of callback here
     *
     * @param apiVersion
     */
    public MainzellisteCallback(Servers.ApiVersion apiVersion) {
        try {

            SSLContextBuilder builder = new SSLContextBuilder();
            SSLContext sslCtx;

            if ("true".equals(Config.instance.getProperty("callback.allowSelfsigned"))) {
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                sslCtx = builder.build();

            } else {
                sslCtx = SSLContexts.createSystemDefault();
            }

            sslsf = new SSLConnectionSocketFactory(sslCtx, new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
        }
        this.apiVersion = apiVersion;
    }

    public void executeCallback(AddPatientToken t, List<ID> returnIds) {
        String callback = t.getDataItemString("callback");
        if (callback != null && callback.length() > 0) {
            try {
                logger.debug("Sending request to callback " + callback);

                JSONObject reqBody = new JSONObject();

                if (apiVersion.majorVersion >= 2) {
                    // Collect ids for Callback object
                    JSONArray idsJson = new JSONArray();

                    for (ID thisID : returnIds) {
                        idsJson.put(thisID.toJSON());
                    }

                    reqBody.put("tokenId", t.getId())
                            .put("ids", idsJson);

                } else {  // API version 1.0
                    if (returnIds.size() > 1) {
                        throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity("Selected API version 1.0 permits only one ID in callback, " +
                                                "but several were requested. Set mainzellisteApiVersion to a " +
                                                "value >= 2.0 or request only one ID type in token.")
                                        .build());
                    }
                    reqBody.put("tokenId", t.getId())
                            .put("id", returnIds.get(0).getIdString());
                }

                HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
                HttpPost callbackReq = new HttpPost(callback);
                callbackReq.setHeader("Content-Type", MediaType.APPLICATION_JSON);
                callbackReq.setHeader("User-Agent", Config.instance.getUserAgentString());
                StringEntity reqEntity = new StringEntity(reqBody.toString());
                reqEntity.setContentType("application/json");
                callbackReq.setEntity(reqEntity);
                HttpResponse response = httpClient.execute(callbackReq);
                StatusLine sline = response.getStatusLine();
                // Accept callback if OK, CREATED or ACCEPTED is returned
                if ((sline.getStatusCode() < 200) || sline.getStatusCode() >= 300) {
                    logger.error("Received invalid status form mdat callback: " + response.getStatusLine());
                    throw new InternalErrorException("Request to callback failed!");
                }
                // TODO: Server-Antwort auslesen

            } catch (JSONException e) {
                logger.error("Internal serializitaion error: ", e);
                throw new InternalErrorException("Internal serializitaion error!");
            } catch (IOException e) {
                logger.error("Internal error building the request: ", e);
                throw new InternalErrorException("Internal error building the request!");
            }
        }
    }
}
