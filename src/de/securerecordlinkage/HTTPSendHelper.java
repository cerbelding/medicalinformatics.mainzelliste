package de.securerecordlinkage;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.samply.common.http.HttpConnector;
import de.samply.common.http.HttpConnectorException;
import de.securerecordlinkage.helperClasses.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

//TODO: run in threads
public class HTTPSendHelper {

    private static Logger logger = null;

    static void doRequest(String url, String action, String data) {
        initLogger();
        doRequest(url, action, data, null);

    }

    static void doRequest(String url, String action, String data, ArrayList<Header> header) {
        logger.info("doRequest(" + url + ", " + action + ", " + data + "," + header + ")");
        try {
            //TODO proxy config
            HashMap config = new HashMap();
            HttpConnector httpConnector = new HttpConnector(config);

            if (header != null) {
                addHeadersToHttpConnector(header, httpConnector);
            }

            CloseableHttpResponse result = httpConnector.doAction(action, url, null, null, "application/json", data, false, false, 5);
            httpStatusEvaluation(result);

        } catch (HttpConnectorException e) {
            e.printStackTrace();
            logger.error("Cannot connect to http ", e);
        }
    }

    private static void addHeadersToHttpConnector(ArrayList<Header> header, HttpConnector httpConnector) {
        for (Header specificHeader : header) {
            //httpConnector.addCustomHeader("Authorization", "apiKey apiKey=\"123qwerty\"");
            httpConnector.addCustomHeader(specificHeader.getKey(), specificHeader.getValue());
        }
    }

    private static void httpStatusEvaluation(CloseableHttpResponse result) {
        logger.info("httpStatusEvaluation");
        if (result.getStatusLine().getStatusCode() == 200) {
            logger.info("SRL configuration updated. Response Code " + String.valueOf(result.getStatusLine().getStatusCode()));
        } else if (result.getStatusLine().getStatusCode() == 204) {
            logger.info("SRL configuration initialized. Response Code " + String.valueOf(result.getStatusLine().getStatusCode()));
        } else {
            throw new InternalErrorException(result.getStatusLine().toString());
        }
    }

    private static void initLogger(){
        logger = Logger.getLogger(HTTPSendHelper.class);
        logger.info("HTTPSendHelper initLogger()");
    }
}
