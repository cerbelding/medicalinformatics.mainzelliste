package de.securerecordlinkage;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.samply.common.http.HttpConnector;
import de.samply.common.http.HttpConnectorException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import java.util.HashMap;

//TODO: run in threads
public class SendHelper {
    static void doRequest(String url, String action, String data) {
        Logger logger = Logger.getLogger(de.pseudonymisierung.mainzelliste.Initializer.class);
        //TODO proxy config
        HashMap config = new HashMap();
        HttpConnector hc = new HttpConnector(config);
        logger.info("action: "+ action + ", data: " + data + ", url: " + url);
        try {
            CloseableHttpResponse result = hc.doAction(action, url, null, null, "application/json", data, false, false, 5);
            if (result.getStatusLine().getStatusCode() == 200) {
                logger.info("SRL configuration updated. Response Code " + String.valueOf(result.getStatusLine().getStatusCode()));
            } else if (result.getStatusLine().getStatusCode() == 204) {
                logger.info("SRL configuration initialized. Response Code " + String.valueOf(result.getStatusLine().getStatusCode()));
            } else {
                throw new InternalErrorException(result.getStatusLine().toString());
            }
        } catch (HttpConnectorException e) {
            e.printStackTrace();
            logger.error("Cannot connect to http ", e);
        }
    }
}
