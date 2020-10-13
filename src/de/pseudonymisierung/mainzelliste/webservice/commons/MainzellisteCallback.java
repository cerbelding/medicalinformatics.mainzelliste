package de.pseudonymisierung.mainzelliste.webservice.commons;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainzellisteCallback {

    /**
     * The logging instance
     */
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * The TLS context depending on the configuration parameters
     */
    private SSLConnectionSocketFactory sslSocketFactory;

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
    private Collection<ID> returnIds;
    private JSONObject returnFields;
    private JSONArray returnPatients;
    private Collection<Double> similarityScores;

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
            SSLContext sslContext;

            if ("true".equals(Config.instance.getProperty("callback.allowSelfsigned"))) {
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                sslContext = builder.build();

            } else {
                sslContext = SSLContexts.createSystemDefault();
            }

            sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            LogManager.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
        }
    }

    /**
     * Sets the apiVersion of this MainzellisteCallback.
     *
     * @param apiVersion {@link de.pseudonymisierung.mainzelliste.Servers.ApiVersion} a valid Mainzelliste Version.
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback apiVersion(Servers.ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Sets the url for this MainzellisteCallback. A Post Request will be executed to this Url.
     *
     * @param url
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback url(String url) {
        this.url = url;
        return this;
    }

    /**
     * Sets the tokenId for this MainzellisteCallback. Then executed the MainzellisteCallback will send a PostRequest which contains this tokenId.
     *
     * @param tokenId
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback tokenId(String tokenId) {
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Sets the returnIds for this MainzellisteCallback. Then executed the MainzellisteCallback will send a PostRequest which contains this List of Ids.
     *
     * @param returnIds A {@link List} of type {@link ID} which contains the IDs that should be send with this callback
     * @return {@link MainzellisteCallback} the updated instance of this object
     */
    public MainzellisteCallback returnIds(Collection<ID> returnIds) {
        this.returnIds = returnIds;
        return this;
    }

    /**
     * execute this Callback. The build method must be called before.
     *
     * @return {@link HttpResponse} then the callback was successful
     * @throws IOException then the callback fails
     */
    public HttpResponse execute() throws IOException {
        logger.info("Executing Mainzelliste Callback on url " + this.url + " and apiVersion " + this.apiVersion.majorVersion + "." + this.apiVersion.minorVersion + ". Tokenid is " + this.tokenId);

        if(Config.instance.getProperty("proxy.callback.url")==null){
            HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
            return httpClient.execute(this.callbackRequest);

        }else{
            HttpHost proxy = new HttpHost(Config.instance.getProperty("proxy.callback.url"), Integer.valueOf(Config.instance.getProperty("proxy.callback.port")));
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            HttpClient httpClient = HttpClients.custom().setRoutePlanner(routePlanner).setSSLSocketFactory(sslSocketFactory).build();
            return httpClient.execute(this.callbackRequest);
        }
    }

    /**
     * build this callback. This method will generate the JSON and build the {@link HttpPost} Request
     *
     * @return {@link MainzellisteCallback} - With the prepared {@link HttpPost}
     * @throws JSONException                then the json could not be serialized
     * @throws UnsupportedEncodingException
     */
    public MainzellisteCallback build() throws JSONException, UnsupportedEncodingException {
        HttpPost callbackRequest = new HttpPost(this.url);
        callbackRequest.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        callbackRequest.setHeader("User-Agent", Config.instance.getUserAgentString());
        HttpEntity reqEntity = buildRequestEntity();
        callbackRequest.setEntity(reqEntity);
        this.callbackRequest = callbackRequest;
        return this;
    }

    /**
     * Builds the callback body
     * currently only supports JSON as return type
     *
     * @return
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    private StringEntity buildRequestEntity() throws JSONException, UnsupportedEncodingException {

        // Check if request is valid
        if (apiVersion.majorVersion == 1) {
            if (this.returnIds != null && this.returnIds.size() > 1)
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity("Selected API version 1.0 permits only one ID in callback, " +
                                        "but several were requested. Set mainzellisteApiVersion to a " +
                                        "value >= 2.0 or request only one ID type in token.")
                                .build());
        }

        if (this.returnPatients != null && (this.returnIds != null || this.returnFields != null))
            throw new IllegalArgumentException("Can't handle MainzellisteCallback that returns patients and either ids or fields. Return only patients or returnIds and returnFields");

        // Building the JSON
        JSONObject json = new JSONObject();

        if (tokenId != null)
            json.put("tokenId", this.tokenId);

        if (returnIds != null && returnIds.size() != 0) {
            JSONArray idsJson = new JSONArray();
            for (ID id : this.returnIds) {
                idsJson.put(id.toJSON());
            }
            if (apiVersion.majorVersion == 1)
                json.put("id", this.returnIds.iterator().next().getEncryptedIdStringFirst());
            else
                json.put("ids", idsJson);
        }

        if (returnFields != null) {
            json.put("fields", this.returnFields);
        }

        if (similarityScores != null) {
            json.put("similarityScores", this.similarityScores);
        }

        if (this.returnPatients != null) {
            json.put("patients", this.returnPatients);
        }

        // Parse JSON to Request Entity
        logger.debug("Building StringEntity for Callback on url " + this.url + " with json " + json.toString());
        StringEntity reqEntity = new StringEntity(json.toString());
        reqEntity.setContentType("application/json");

        return reqEntity;
    }

    public MainzellisteCallback returnFields(JSONObject returnFields) {
        this.returnFields = returnFields;
        return this;
    }

    /**
     * This method will add the array of patients to the Callback. If there are already Patients added to this callback, new Patients will be appended
     *
     * @param patients
     * @return
     * @throws JSONException - then an element from the input couldn't be read
     */
    public MainzellisteCallback addPatients(JSONArray patients) throws JSONException, IllegalArgumentException {
        if (this.returnPatients == null)
            this.returnPatients = new JSONArray();
        for (int i = 0; i < patients.length(); i++) {
            JSONObject patient = patients.getJSONObject(i);
            if (patient.getString("ids") != null && patient.getString("fields") != null)
                this.returnPatients.put(patient);
            else {
                throw new IllegalArgumentException("The JSONArray passed to addPatients does not contain ids and fields for each patient");
            }
        }
        return this;
    }

    /**
     * This method will add a patient Object to the Callback. If there are already Pateints added to this callback, the new patient will be appended
     *
     * @param patient
     * @return
     * @throws JSONException
     */
    public MainzellisteCallback addPatient(JSONObject patient) throws JSONException {
        if (this.returnPatients == null)
            this.returnPatients = new JSONArray();
        if (patient.getString("ids") != null && patient.getString("fields") != null)
            this.returnPatients.put(patient);
        else {
            throw new IllegalArgumentException("The JSONObject passed to addPatient does not contain ids and fields");
        }
        return this;
    }

    /**
     *
     * @param similarityScores
     * @return
     */
    public MainzellisteCallback similarityScores(Collection<Double> similarityScores) {
        this.similarityScores = similarityScores;
        return this;
    }
}
