package de.securerecordlinkage.helperClasses;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.samply.common.http.HttpConnector;
import de.samply.common.http.HttpConnectorException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

//TODO: run in threads
public class HTTPSendHelper {

    private static Logger logger = null;

    public static CloseableHttpResponse doRequest(String url, String action, String data) {
        return doRequest(url, action, data, null);
    }

    public static CloseableHttpResponse doRequest(String url, String action, String data, ArrayList<Header> headers) {
        initLogger();
        logger.info("doRequest(" + String.valueOf(url) + ", " + String.valueOf(action) + ", " + String.valueOf(data) + "," + String.valueOf(headers) + ")");

        try {
            //TODO proxy config
            HashMap config = new HashMap();
            HttpConnector httpConnector = new HttpConnector(config);

            if (headers != null) {
                addHeadersToHttpConnector(headers, httpConnector);
            }

            CloseableHttpResponse httpResult = httpConnector.doAction(action, url, null, null, "application/json", data, false, false, 5);
            httpStatusEvaluation(httpResult);
            return httpResult;

        } catch (HttpConnectorException e) {
            e.printStackTrace();
            logger.error("Cannot connect to http ", e);
            return null;
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
