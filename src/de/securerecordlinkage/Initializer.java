package de.securerecordlinkage;

import com.sun.jersey.spi.container.servlet.WebComponent;
import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.PlainTextField;
import de.securerecordlinkage.initializer.config.ExternalServer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.util.*;
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
        Logger logger = Logger.getLogger(de.securerecordlinkage.Initializer.class);
        logger.info("#####Initializing...");

        try {
            logger.info("initialize - SRL");
            Config c = Config.instance;
            JSONObject configJSON = createLocalInitJSON(c);
            SendHelper.doRequest(c.getProperty("srl.initLocal"), "PUT", configJSON.toString());


            //Only For Tests!! We init a second local SRL TODO: delete

        } catch (Exception e) {
            logger.error("initialize() - Could not send initJSON " + e.toString());
            //e.printStackTrace();
        }

        try {
            logger.info("initialize - Communicator");
            Config c = Config.instance;
            CommunicatorResource.init(c);

        } catch (Exception e) {
            logger.error("initialize() - Could not load configuration and init communicator");
            //e.printStackTrace();
        }
        //Init Communicator Ressource


        try {
            //de.securerecordlinkage.initializer.Config selConfig = de.securerecordlinkage.initializer.Config.instance;
            Config c = Config.instance;
            //List<JSONObject> remoteInitJSON = createRemoteInitJSON();
            JSONObject remoteInitJSON = createRemoteInitJSON();
            SendHelper.doRequest(c.getProperty("srl.secureEpiLinkRemoteURL"), "PUT", remoteInitJSON.toString());

        } catch (Exception e) {
            logger.error("initialize() - Could not send remoteJSON " + e.toString());
            //e.printStackTrace();
        }

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

    private JSONObject createLocalInitJSON(Config config){
        JSONObject reqObject = new JSONObject();
        JSONObject tmpObj = new JSONObject();
        JSONObject dateServiceObj = new JSONObject();
        try {

            //de.securerecordlinkage.initializer.Config configInitializer = de.securerecordlinkage.initializer.Config.instance;
            tmpObj.put("authType", "apiKey");
            tmpObj.put("sharedKey", config.getProperty("srl.apiKey"));
            reqObject.put("localAuthentication", tmpObj);
            dateServiceObj = new JSONObject();
            dateServiceObj.put("url", config.getProperty("srl.baseCommunicatorURL"));
            reqObject.put("dataService", dateServiceObj);
            tmpObj = new JSONObject();
            tmpObj.put("algoType", "epilink");
            tmpObj.put("bloomLength", 500);
            tmpObj.put("threshold_match", Float.valueOf(config.getProperty("matcher.epilink.threshold_match")));
            tmpObj.put("threshold_non_match", Float.valueOf(config.getProperty("matcher.epilink.threshold_non_match")));

            JSONArray exchangeGroups = new JSONArray();
            for (int i = 0; config.getProperties().containsKey("exchangeGroup." + i); i++) {
                String[] exchangeFields = config.getProperty("exchangeGroup." + i).split(" *[;,] *");
                exchangeGroups.put(Arrays.asList(exchangeFields));
            }

            tmpObj.put("exchangeGroups", exchangeGroups);

            JSONArray fields = new JSONArray();
            for (String key : config.getFieldKeys()) {

                String propName = "field." + key;

                JSONObject field = new JSONObject();
                field.put("name", key);
                float frequency = Float.valueOf(config.getProperty("matcher.epilink." + key +  ".frequency"));
                float errorRate = Float.valueOf(config.getProperty("matcher.epilink." + key +  ".errorRate"));

                field.put("frequency", frequency);
                field.put("errorRate", errorRate);

                field.put("bitlength", 500);

                String comparator = config.getProperty(propName + ".comparator");
                comparator = comparator.replace("Field", "").replace("Comparator", "").replace("NGram", "nGram").replace("Binary", "binary");
                field.put("comparator", comparator);

                //TODO What criterias are needed?
                // TODO: What if we are not working with
                Class<? extends Field<?>> fieldType = config.getFieldType(key);
                if("NGram".equalsIgnoreCase(comparator)) {
                    field.put("fieldType", "bitmask");
                }else if(PlainTextField.class.isAssignableFrom(fieldType)){
                    field.put("fieldType", "string");
                }else{
                    String type;
                    type = fieldType.getName().toLowerCase().replace("field", "");
                    String [] parts = type.split("\\.");
                    field.put("fieldType", parts[parts.length-1]);
                }

                fields.put(field);
            }

            tmpObj.put("fields", fields);

            reqObject.put("algorithm", tmpObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqObject;
    }

    // TODO: 1. Create config file for remote init
    // 2. Read parameters from this file
    // 3. Use samply.config ?
    //private List<JSONObject> createRemoteInitJSON() {
    private JSONObject createRemoteInitJSON() {
        List<ExternalServer> servers = de.securerecordlinkage.initializer.Config.instance.getConfig().getServers().getServer();
        //List<JSONObject> results = new ArrayList<>();
        JSONObject results = new JSONObject();

        try {

            for (ExternalServer server : servers) {
                JSONObject reqObject = new JSONObject();
                JSONObject tmpObj = new JSONObject();
                JSONObject authObj = new JSONObject();

                tmpObj.put("url", server.getUrl());
                tmpObj.put("idType", "lid");

                authObj.put("authType", "apiKey");
                authObj.put("sharedKey", server.getApiKey());
                tmpObj.put("authentication", authObj);
                //reqObject.put("connectionProfile", tmpObj);

                results.put("connectionProfile", tmpObj);
                //results.add(reqObject);
            }

            //TODO: linkageService missing
            for (ExternalServer server : servers) {
                JSONObject reqObject = new JSONObject();
                JSONObject tmpObj = new JSONObject();
                JSONObject authObj = new JSONObject();

                tmpObj.put("url", server.getUrl());
                tmpObj.put("idType", "lid");

                authObj.put("authType", "apiKey");
                authObj.put("sharedKey", server.getApiKey());
                tmpObj.put("authentication", authObj);
                //reqObject.put("linkageService", tmpObj);

                results.put("linkageService", tmpObj);
                //results.add(reqObject);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }

}
