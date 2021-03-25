package de.pseudonymisierung.mainzelliste.httpsClient;

import org.codehaus.jettison.json.JSONObject;
import java.io.IOException;
import java.util.Map;


public interface HttpsClientInterface<Header extends HttpHeadersInterface<Map<String, String>>, ResponseType extends JSONObject> {

  ResponseType request(String url, Header header) throws IOException;

}
