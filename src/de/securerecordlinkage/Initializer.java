package de.securerecordlinkage;

import com.sun.jersey.spi.container.servlet.WebComponent;
import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.PlainTextField;
import de.securerecordlinkage.configuration.ConfigLoader;
import de.securerecordlinkage.helperClasses.HTTPSendHelper;
import de.securerecordlinkage.helperClasses.Header;
import de.securerecordlinkage.configuration.Server;
import de.securerecordlinkage.helperClasses.HeaderHelper;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.util.*;



/**
 * 
 */
public class Initializer {

    /** The injected ServletContext. */
    private static ServletContext configurationContext;
    private static Logger logger = null;

    
    public void contextInitialized(ServletContextEvent sce) {
        configurationContext = sce.getServletContext();
        initialize();
    }

    private void initialize() {

        initLogger();
        logger.info("#####initialize()...");
        Config config = Config.instance;
        ConfigLoader srlConfig = ConfigLoader.instance;

        sendInitLocalToOwnSecureEpiLinker(config, srlConfig);

        // TODO: Only the first remote remoteServer is taken at the moment
        // Who decides which remoteServer is taken in case we have more than one
        HashMap<String, Server> remoteServers = srlConfig.instance.getServers();
        Server remoteServer = remoteServers.entrySet().iterator().next().getValue();

        initializeCommunicatorResource(srlConfig, remoteServer);

        sendInitRemoteToOwnSecureEpiLinker(srlConfig, remoteServer);



        //log4jSetup();



        /*
         * Limit Jersey logging to avoid spamming the log with "the request body has been consumed" messages
         * (see http://stackoverflow.com/questions/2011895/how-to-fix-jersey-post-request-parameters-warning).
         * This applies to use cases where all fields are transmitted via the "addPatient" token and the
         * POST /patients request is intentionally empty.
         */
        /*
        java.util.logging.Logger webComponentLogger = java.util.logging.Logger.getLogger(WebComponent.class.getName());
        webComponentLogger.setLevel(Level.SEVERE);
        logger.info("#####Startup succeeded. Ready to take requests.");
        */

    }

    private void initializeCommunicatorResource(ConfigLoader srlConfig, Server remoteServer) {
        try {
            logger.info("initialize - Communicator");
            CommunicatorResource.init(srlConfig, remoteServer.getId());
        } catch (Exception e) {
            logger.error("initialize() - Could not load configuration and init communicator");
            //e.printStackTrace();
        }
    }

    private void sendInitLocalToOwnSecureEpiLinker(Config c, ConfigLoader srlConfig) {
        logger.info("sendInitLocalToOwnSecureEpiLinker");
        try {
            JSONObject configJSON = createLocalInitJSON(c);
            logger.info(configJSON);
            HTTPSendHelper.doRequest(srlConfig.getLocalSELUrl() + "/initLocal", "PUT", configJSON.toString());
        } catch (Exception e) {
            logger.error("initialize() - Could not send initJSON " + e.toString());
            e.printStackTrace();
        }
    }

    private void sendInitRemoteToOwnSecureEpiLinker(ConfigLoader srlConfig, Server remoteServer) {
        logger.info("sendInitRemoteToOwnSecureEpiLinker");
        ArrayList<Header> headers = HeaderHelper.addHeaderToNewCreatedArrayList("Authorization", "apiKey apiKey=\"" + srlConfig.getLocalApiKey() + "\"");
        try {
            //remote Init
            JSONObject remoteInitJSON = createRemoteInitJSON(remoteServer);
            logger.info(remoteInitJSON);
            HTTPSendHelper.doRequest(srlConfig.getLocalSELUrl()+"/initRemote/"+remoteServer.getId(), "PUT", remoteInitJSON.toString(), headers);

        } catch (Exception e) {
            logger.error("initialize() - Could not send remoteJSON " + e.toString());
            e.printStackTrace();
        }
    }


    private void log4jSetup() {
        Logger root = Logger.getRootLogger();

        Logger.getRootLogger().setLevel(Level.WARN);
        String logFileName = "/usr/local/tomcat/logs/secureRecordLinkage.log";
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
                //if (!Config.instance.debugIsOn()) {
                //    root.warn("Redirecting SecureRecordLinkage log to " + logFileName
                //            + ".");
                //    root.removeAllAppenders();
                //}

                root.addAppender(app);
        //        root.info("Logger setup to log on level "
        //                + ConfigLoader.instance.getLogLevel() + " to " + logFileName);
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

            ConfigLoader srlConfig = ConfigLoader.instance;
            reqObject.put("localId", srlConfig.getLocalID());
            tmpObj.put("authType", srlConfig.getLocalAuthenticationType());

            tmpObj.put("sharedKey", srlConfig.getLocalApiKey());

            if(srlConfig.getLocalAuthenticationType().equals("apiKey")){
                reqObject.put("localAuthentication", tmpObj);
            }

            dateServiceObj.put("url", srlConfig.getLocalDataServiceUrl());
            reqObject.put("dataService", dateServiceObj);
            tmpObj = new JSONObject();
            tmpObj.put("algoType", "epilink");
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

                String comparator = config.getProperty(propName + ".comparator");
                comparator = comparator.replace("Field", "").replace("Comparator", "").replace("NGram", "dice").toLowerCase();
                field.put("comparator", comparator);

                //TODO What criterias are needed?
                // TODO: What if we are not working with
                // TODO: Default values for fields move to Configuration or variables
                HashMap<String, Integer> fieldBitLength = srlConfig.getFieldBitLength();
                Class<? extends Field<?>> fieldType = config.getFieldType(key);
                if("Dice".equalsIgnoreCase(comparator)) {
                    field.put("fieldType", "bitmask");
                    if (fieldBitLength.containsKey(key)) {
                        field.put("bitlength", fieldBitLength.get(key));
                    } else {
                        field.put("bitlength", 500);
                    }
                }else if(PlainTextField.class.isAssignableFrom(fieldType)){
                    field.put("fieldType", "string");
                    if (fieldBitLength.containsKey(key)) {
                        field.put("bitlength", fieldBitLength.get(key));
                    } else {
                        field.put("bitlength", 240);
                    }
                }else{
                    String type;
                    type = fieldType.getName().toLowerCase().replace("field", "");
                    String [] parts = type.split("\\.");
                    field.put("fieldType", parts[parts.length-1]);
                    if (fieldBitLength.containsKey(key)) {
                        field.put("bitlength", fieldBitLength.get(key));
                    } else {
                        field.put("bitlength", 17);
                    }
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
    private JSONObject createRemoteInitJSON(Server server) {
        JSONObject rootJSON = new JSONObject();
        try {

            JSONObject connectionProfileJSON = new JSONObject();
            JSONObject localAuthenticationJSON = new JSONObject();
            JSONObject linkageServiceJSON = new JSONObject();

            connectionProfileJSON.put("url", server.getUrl());
            localAuthenticationJSON.put("authType", "apiKey");
            //TODO: if authentication type == apikey, has to be part of server object
            localAuthenticationJSON.put("sharedKey", server.getApiKey());

            connectionProfileJSON.put("authentication", localAuthenticationJSON);

            rootJSON.put("connectionProfile", connectionProfileJSON);

            linkageServiceJSON.put("url", server.getLinkageServiceBaseURL());
            rootJSON.put("linkageService", linkageServiceJSON);
            //TODO: linkageServiceAuthentification is missing

            rootJSON.put("matchingAllowed", true);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rootJSON;
    }

    /**
     * Gets the injected ServletContext.
     * @return The injected ServletContext.
     */
    public static ServletContext getServletContext() {
        return configurationContext;
    }

    private static void initLogger(){
        logger = Logger.getLogger(Initializer.class);

        //FileAppender fileAppender = new FileAppender();
        //fileAppender.setLayout();


        //logger.addAppender();
        logger.info("SecureRecordLinkage Initializer initLogger()");
    }
}
