package de.securerecordlinkage;

import com.sun.jersey.spi.container.servlet.WebComponent;
import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.samply.common.http.HttpConnector;
import de.samply.common.http.HttpConnectorException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * 
 */
public class Initializer {

    /** The injected ServletContext. */
    private static ServletContext configurationContext;
    
    public void contextInitialized(ServletContextEvent sce) {
        configurationContext = sce.getServletContext();
        initialize();
    }

    private void initialize() {
        Logger logger = Logger.getLogger(de.pseudonymisierung.mainzelliste.Initializer.class);
        logger.info("#####Initializing...");

        Config c = Config.instance;
        JSONObject configJSON = createInitJSON(c);
        doRequest("http://echo.jsontest.com/test/value", configJSON.toString());

        log4jSetup();

        /*
         * Limit Jersey logging to avoid spamming the log with "the request body has been consumed" messages
         * (see http://stackoverflow.com/questions/2011895/how-to-fix-jersey-post-request-parameters-warning).
         * This applies to use cases where all fields are transmitted via the "addPatient" token and the
         * POST /patients request is intentionally empty.
         */
        java.util.logging.Logger webComponentLogger = java.util.logging.Logger.getLogger(WebComponent.class.getName());
        webComponentLogger.setLevel(Level.SEVERE);
        logger.info("#####Startup succeeded. Ready to take requests.");
    }

    private void log4jSetup() {
        Logger root = Logger.getRootLogger();
        //root.setLevel(Config.instance.getLogLevel());
        String logFileName = Config.instance.getProperty("log.filename");
        if (logFileName == null) {
            root.info("Using default logging output.");
        } else {
            PatternLayout layout = new PatternLayout("%d %p %t %c - %m%n");
            try {
                FileAppender app;
                app = new FileAppender(layout, logFileName);
                app.setName("SecureRecordLinkageFileAppender");

                // In production mode, avoid spamming the servlet container's
                // logfile.
                if (!Config.instance.debugIsOn()) {
                    root.warn("Redirecting SecureRecordLinkage log to " + logFileName
                            + ".");
                    root.removeAllAppenders();
                }

                root.addAppender(app);
        //        root.info("Logger setup to log on level "
        //                + Config.instance.getLogLevel() + " to " + logFileName);
            } catch (IOException e) {
                root.fatal("Unable to log to " + logFileName + ": "
                        + e.getMessage());
                return;
            }
        }
        root.info("#####BEGIN SecureRecordLinkage LOG SESSION");
    }

    private JSONObject createInitJSON(Config config){
        JSONObject reqObject = new JSONObject();
        JSONObject tmpObj = new JSONObject();
        try {
            // TODO: Aus Config auslesen
            tmpObj.put("authType", "apiKey");
            tmpObj.put("sharedKey", "123abc");
            reqObject.put("localAuthentification", tmpObj);
            tmpObj = new JSONObject();
            tmpObj.put("url", "https://localhost:8080/ValueList");
            reqObject.put("dataService", tmpObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqObject;
    }

    private void doRequest(String url, String data) {
        Logger logger = Logger.getLogger(de.pseudonymisierung.mainzelliste.Initializer.class);
        //TODO proxy config
        HashMap config = new HashMap();
        HttpConnector hc = new HttpConnector(config);
        try {
            CloseableHttpResponse result = hc.doAction("POST", url, null, null, "application/json", data, false, false, 5);
        if(result.getStatusLine().getStatusCode() != 204) {
            throw new InternalErrorException(result.getStatusLine().toString());
        }
        } catch (HttpConnectorException e) {
            e.printStackTrace();
            logger.error("Cannot connect to http ", e);
        }
    }
}
