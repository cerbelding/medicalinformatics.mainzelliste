/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */
package de.securerecordlinkage.configuration;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Configuration of the patient list. Implemented as a singleton object, which
 * can be referenced by ConfigLoader.instance. The configuration is read from the
 * properties file specified as parameter
 * de.pseudonymisierung.mainzelliste.ConfigurationFile in context.xml (see
 * {@link Properties#load(InputStream) java.util.Properties}).
 */
public enum ConfigLoader {

	/** The singleton instance */
	instance;

	/** localID of Mainzelliste instance */
	private final String localID;
	private final String localSELUrl;
	private final String localApiKey;
	private final String localDataServiceUrl;
	private final String localCallbackLinkUrl;
	private final String localCallbackMatchUrl;
	private final String localAuthenticationType;



	private final HashMap<String, Server> remoteServers = new HashMap<String, Server>();

	/** Default paths from where configuration is read if no path is given in the context descriptor */
	private final String defaultConfigPaths[] = {"/etc/mainzelliste/sel.conf", "/WEB-INF/classes/sel.conf"};

	/** The configured fields, keys are field names, values the respective field types. */
	private final HashMap<String,Integer> fieldBitlength;

	/** Properties object that holds the configuration parameters. */
	private Properties props;

	/** Logging instance */
	private Logger logger = Logger.getLogger(ConfigLoader.class);

	/**
	 * Creates an instance. Invoked on first access to ConfigLoader.instance. Reads
	 * the configuration file.
	 *
	 */
	@SuppressWarnings("unchecked")
	ConfigLoader() {
		props = new Properties();
		try {
			// Check if path to configuration file is given in context descriptor
			ServletContext context = de.securerecordlinkage.Initializer.getServletContext();
			String configPath = context.getInitParameter("de.pseudonymisierung.mainzelliste.ConfigurationFileSEL");

			// try to read config from configured path
			if (configPath != null) {
				logger.info("Reading config from path " + configPath + "...");
				props = readConfigFromFile(configPath);
				if (props == null) {
					throw new Error("Configuration file could not be read from provided location " + configPath);
				}
			} else {
				// otherwise, try default locations
				logger.info("No configuration file configured. Try to read from default locations...");
				for (String defaultConfigPath : defaultConfigPaths) {
					logger.info("Try to read configuration from default location " + defaultConfigPath);
					props = readConfigFromFile(defaultConfigPath);
					if (props != null) {
						logger.info("Found configuration file at default location " + defaultConfigPath);
						break;
					}
				}
				if (props == null) {
					throw new Error("Configuration file could not be found at any default location");
				}
			}

			logger.info("ConfigLoader read successfully");
			logger.debug(props);

		} catch (IOException e)	{
			logger.fatal("Error reading configuration file. Please configure according to installation manual.", e);
			throw new Error(e);
		}

		localID = props.getProperty("localID");
		localSELUrl = props.getProperty("localSELUrl");
		localApiKey = props.getProperty("localApiKey");
		localDataServiceUrl = props.getProperty("localDataServiceUrl");
		localCallbackLinkUrl = props.getProperty("localCallbackLinkUrl");
		localCallbackMatchUrl = props.getProperty("localCallbackMatchUrl");
		localAuthenticationType = props.getProperty("localAuthenticationType");



		// Read field bitlength from configuration
		Pattern pattern = Pattern.compile("field\\.(\\w+)\\.bitlength");
		java.util.regex.Matcher patternMatcher;
		this.fieldBitlength = new HashMap<>();
		for (String propKey : props.stringPropertyNames()) {
			patternMatcher = pattern.matcher(propKey);
			if (patternMatcher.find())
			{
				String fieldName = patternMatcher.group(1);
				int fieldValue = Integer.parseInt(props.getProperty(propKey).trim());
				this.fieldBitlength.put(fieldName, fieldValue);
			}
		}

		Boolean found = true;
		int i = 0;

		while (found) {
			if (props.containsKey("servers." + i + ".remoteId")){
				Server server = new Server();
				server.setId(props.getProperty("servers." + i + ".remoteId"));
				if (props.containsKey("servers." + i + ".apiKey")) {
					server.setApiKey(props.getProperty("servers." + i + ".apiKey"));
				}
				if (props.containsKey("servers." + i + ".idType")) {
					server.setIdType(props.getProperty("servers." + i + ".idType"));
				}
				if (props.containsKey("servers." + i + ".remoteSELUrl")) {
					server.setUrl(props.getProperty("servers." + i + ".remoteSELUrl"));
				}
				if (props.containsKey("servers." + i + ".linkageServiceBaseURL")) {
					server.setLinkageServiceBaseURL(props.getProperty("servers." + i + ".linkageServiceBaseURL"));
				}
				if (props.containsKey("servers." + i + ".linkageServiceAuthType")) {
					server.setLinkageServiceAuthType(props.getProperty("servers." + i + ".linkageServiceAuthType"));
				}
				if (props.containsKey("servers." + i + ".linkageServiceSharedKey")) {
					server.setLinkageServiceSharedKey(props.getProperty("servers." + i + ".linkageServiceSharedKey"));
				}

				remoteServers.put(server.getId(), server);
				i = i + 1;
			} else {
				found = false;
			}
		}
	}

	/**
	 * Read configuration from the given file. Tries to read the file in the
	 * following order:
	 * <ol>
	 * <li>From inside the application via getResourceAsStream().
	 * <li>From the file system
	 * </ol>
	 *
	 * @param configPath
	 *            Path to configuration file.
	 * @return The configuration as a Properties object or null if the given
	 *         file was not found.
	 * @throws IOException
	 *             If an I/O error occurs while reading the configuration file.
	 */
	private Properties readConfigFromFile(String configPath) throws IOException {
		ServletContext context = de.pseudonymisierung.mainzelliste.Initializer.getServletContext();
		// First, try to read from resource (e.g. within the war file)
		InputStream configInputStream = context.getResourceAsStream(configPath);
		// Else: read from file System
		if (configInputStream == null) {
			File f = new File(configPath);
			if (f.exists())
				configInputStream = new FileInputStream(configPath);
			else return null;
		}

		Reader reader = new InputStreamReader(configInputStream, "UTF-8");
		Properties props = new Properties();
		props.load(reader);
		// trim property values
		for (String key : props.stringPropertyNames()) {
			String value = props.getProperty(key);
			if (!value.equals(value.trim())) {
				props.setProperty(key, value.trim());
			}
		}
		configInputStream.close();
		return props;
	}

	public String getLocalID() {
		return localID;
	}
	public String getLocalSELUrl() {
		return localSELUrl;
	}
	public String getLocalApiKey() {return localApiKey; }
	public String getLocalAuthenticationType() {return localAuthenticationType; }

	/*
	public String getSharedKey() {
		String [] parts = localApiKey.split("=");
		String sharedKey = parts[parts.length-1].trim();
		return sharedKey.substring(1,sharedKey.length()-1);
	}
	*/
	public String getLocalDataServiceUrl() {return localDataServiceUrl;}





	public String getLocalCallbackLinkUrl() {return localCallbackLinkUrl; }
	public String getLocalCallbackMatchUrl() {return localCallbackMatchUrl; }

	public HashMap<String, Integer> getFieldBitLength() {
		return fieldBitlength;
	}
	public HashMap<String, Server> getServers() {
		return remoteServers;
	}
}
