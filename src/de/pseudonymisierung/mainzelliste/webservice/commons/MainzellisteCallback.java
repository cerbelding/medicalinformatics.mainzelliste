package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainzellisteCallback {

    /**
     * The logging instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * The TLS context depending on the configuration parameters
     */
    private SSLConnectionSocketFactory sslsf;

    /**
     * Api Version which should be used
     */
    private Servers.ApiVersion apiVersion;

    /**
     * The url to which the callback should be send
     */
    private String url;
    private HttpPost callbackRequest;

    private String tokenId;
    private List<ID> returnIds;

    /**
     * represents a callback executed by the Mainzelliste.
     * Use like this:
     * <ol>
     *    <li>Create with {@code MainzellisteCallback mainzellisteCallback = new MainzellisteCallback()}</li>
     *    <li>Set url and apiVersion:<p>{@code mainzellisteCallback = mainzellisteCallback.url("http://example.callback.url").apiVersion(aSpecificMainzellisteApiVersion)}</p></li>
     *    <li>Set content of the Callback:<p>{@code mainzellisteCallback = mainzellisteCallback.tokenId(token.getIdString()).returnIds(someIdsToReturn)}</p></li>
     *    <li>Build the Callback: {@code mainzellisteCallback = mainzellisteCallback.build()}</li>
     *    <li>Execute: {@Code HttpResponse httpResponse = mainzelliste.execute()}</li>
     *    <li>Process the Response received</li>
     * </ol>
     * Too enable Callbacks on http Resources, callback.allowedFormat in Mainzelliste config file must be adjusted.
     * It is also possible to use selfsigned certificates with: callback.allowSelfsigned = true
     */
    public MainzellisteCallback() {
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

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
        }
    }

    /**
     * Sets the apiVersion of this MainzellisteCallback.
     * @param apiVersion {@link de.pseudonymisierung.mainzelliste.Servers.ApiVersion} a valid Mainzelliste Version.
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback apiVersion(Servers.ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Sets the url for this MainzellisteCallback. A Post Request will be executed to this Url.
     * @param url
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback url(String url) {
        this.url = url;
        return this;
    }

    /**
     * Sets the tokenId for this MainzellisteCallback. Then executed the MainzellisteCallback will send a PostRequest which contains this tokenId.
     * @param tokenId
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback tokenId(String tokenId) {
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Sets the returnIds for this MainzellisteCallback. Then executed the MainzellisteCallback will send a PostRequest which contains this List of Ids.
     * @param returnIds A {@link List} of type {@link ID} which contains the IDs that should be send with this callback
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback returnIds(List<ID> returnIds) {
        this.returnIds = returnIds;
        return this;
    }

    /**
     * execute this Callback. The build method must be called before.
     * @return {@link HttpResponse} then the callback was successful
     * @throws IOException then the callback fails
     */
    public HttpResponse execute() throws IOException {
        logger.info("Executing Mainzelliste Callback on url " + this.url + " and apiVersion " + this.apiVersion.majorVersion + "." + this.apiVersion.minorVersion + ". Tokenid is " + this.tokenId);
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        return httpClient.execute(this.callbackRequest);
    }

    /**
     * build this callback. This method will generate the JSON and build the {@link HttpPost} Request
     * @return {@link MainzellisteCallback} - With the prepared {@link HttpPost}
     * @throws JSONException then the json could not be serialized
     * @throws UnsupportedEncodingException
     */
    public MainzellisteCallback build() throws JSONException, UnsupportedEncodingException {
        HttpPost callbackReq = new HttpPost(this.url);
        callbackReq.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        callbackReq.setHeader("User-Agent", Config.instance.getUserAgentString());
        HttpEntity reqEntity = buildRequestEntity();
        callbackReq.setEntity(reqEntity);
        this.callbackRequest = callbackReq;
        return this;
    }

    /**
     * Builds the callback body
     * currently only supports JSON as return type
     * @return
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    private StringEntity buildRequestEntity() throws JSONException, UnsupportedEncodingException{

        // Check if request is valid
        if (apiVersion.majorVersion == 1) {
            if (this.returnIds.size() > 1)
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity("Selected API version 1.0 permits only one ID in callback, " +
                                        "but several were requested. Set mainzellisteApiVersion to a " +
                                        "value >= 2.0 or request only one ID type in token.")
                                .build());
        }

        // Building the JSON
        JSONObject reqBody = new JSONObject();

        if (tokenId != null)
            reqBody.put("tokenId", this.tokenId);

        if (returnIds != null && returnIds.size() != 0) {
            JSONArray idsJson = new JSONArray();
            for (ID id : this.returnIds) {
                idsJson.put(id.toJSON());
            }
            if(apiVersion.majorVersion == 1)
                reqBody.put("id", this.returnIds.get(0).getIdString());
            else
                reqBody.put("ids", idsJson);
        }

        // Parse JSON to Request Entity
        logger.debug("Building StringEntity for Callback on url " + this.url + " with json " + reqBody.toString());
        StringEntity reqEntity = new StringEntity(reqBody.toString());
        reqEntity.setContentType("application/json");

        return reqEntity;
    }
}
