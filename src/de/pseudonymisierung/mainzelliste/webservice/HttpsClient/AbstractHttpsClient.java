package de.pseudonymisierung.mainzelliste.webservice.HttpsClient;


import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.net.Proxy;

public abstract class AbstractHttpsClient<Header extends HttpHeadersInterface, URLParam extends HttpUrlParametersInterface, ResponseType extends JSONObject > {
    protected final  String url;
    protected Proxy proxy;
    protected Logger logger = Logger.getLogger(AbstractHttpsClient.class);


    public AbstractHttpsClient(String url){
        this. url = url;
    }

    public AbstractHttpsClient(String url, Proxy proxy){
        this.proxy = proxy;
        this.url =url;
    }

    public abstract ResponseType  request(Header header, URLParam urlParam);


}
