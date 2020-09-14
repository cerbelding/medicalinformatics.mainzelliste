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
package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.blocker.BlockingKeyExtractors;
import de.pseudonymisierung.mainzelliste.crypto.CryptoUtil;
import de.pseudonymisierung.mainzelliste.crypto.Encryption;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidConfigurationException;
import de.pseudonymisierung.mainzelliste.matcher.Matcher;
import java.security.Key;
import java.security.spec.InvalidKeySpecException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configuration of the patient list. Implemented as a singleton object, which
 * can be referenced by Config.instance. The configuration is read from the
 * properties file specified as parameter
 * de.pseudonymisierung.mainzelliste.ConfigurationFile in context.xml (see
 * {@link java.util.Properties#load(InputStream) java.util.Properties}).
 */
public enum Config {

	/** The singleton instance */
	instance;

	/** The software version of this instance. */
	private final String version;

	/** Default paths from where configuration is read if no path is given in the context descriptor */
	private final String defaultConfigPaths[] = {"/etc/mainzelliste/mainzelliste.conf", "/WEB-INF/classes/mainzelliste.conf"};

	/** The configured fields, keys are field names, values the respective field types. */
	private final Map<String,Class<? extends Field<?>>> FieldTypes;

	/** Properties object that holds the configuration parameters. */
	private Properties props;

	/** The record transformer matching the configured field transformations. */
	private RecordTransformer recordTransformer;

	/** The configured matcher */
	private Matcher matcher;

	/** The configured blockingkey extractors */
	private BlockingKeyExtractors blockingKeyExtractors;

	/** The configured cryptographic key */
	private final Map<String, Key> cryptographicKeys = new HashMap<>();

	/** The configured encryption */
	private final Map<String, Encryption> encryptionMap = new HashMap<>();

	/** Logging instance */
	private Logger logger = Logger.getLogger(Config.class);

	/** Allowed origins for Cross Domain Resource Sharing. */
	private Set<String> allowedOrigins;

	/** Allowed headers for Cross Domain Resource Sharing */
	private String allowedHeaders;
	/** Allowed methods for Cross Domain Resource Sharing */
	private String allowedMethods;
	/** Allowed caching time for Cross Domain Resource Sharing Preflight Requests */
	private int allowedMaxAge;

	/** some gui configurations */
	private GUI guiConfig;

	/**
	 * Creates an instance. Invoked on first access to Config.instance. Reads
	 * the configuration file.
	 *
	 * @throws InternalErrorException
	 *             If an error occurs during initialization. This signals a
	 *             fatal error and prevents starting the application.
	 */
	@SuppressWarnings("unchecked")
	Config() throws InternalErrorException {
		props = new Properties();
		try {
			// try to read config from general config path for all components
			props = readConfigFromEnv("CONFIG_DIRS");

			if (props == null) {
				// try to read config from general config path for identity management
				props = readConfigFromEnv("IDM_CONFIG_DIRS");
			}

			if (props == null) {
				// Check if path to configuration file is given in context descriptor
				ServletContext context = Initializer.getServletContext();
				String configPath = context.getInitParameter("de.pseudonymisierung.mainzelliste.ConfigurationFile");

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
			}
			addSubConfigurationPropertiesToProps();

			logger.info("Config read successfully");
			logger.debug(props);

		} catch (IOException e)	{
			logger.fatal("Error reading configuration file. Please configure according to installation manual.", e);
			throw new Error(e);
		}

		this.recordTransformer = new RecordTransformer(props);

		try {
			Class<?> matcherClass = Class.forName("de.pseudonymisierung.mainzelliste.matcher." + props.getProperty("matcher"));
			matcher = (Matcher) matcherClass.newInstance();
			matcher.initialize(props);
			logger.info("Matcher of class " + matcher.getClass() + " initialized.");
		} catch (Exception e){
			logger.fatal("Initialization of matcher failed: " + e.getMessage(), e);
			throw new InternalErrorException();
		}

		// Read field types from configuration
		Pattern pattern = Pattern.compile("field\\.(\\w+(?:\\.\\w+)*)\\.type");
		java.util.regex.Matcher patternMatcher;
		this.FieldTypes = new HashMap<String, Class<? extends Field<?>>>();
		for (String propKey : props.stringPropertyNames()) {
			patternMatcher = pattern.matcher(propKey);
			if (patternMatcher.find())
			{
				String fieldName = patternMatcher.group(1);
				String fieldClassStr = props.getProperty(propKey).trim();
				try {
					Class<? extends Field<?>> fieldClass;
					try {
						fieldClass = (Class<? extends Field<?>>) Class.forName(fieldClassStr);
					} catch (ClassNotFoundException e) {
						// Try with "de.pseudonymisierung.mainzelliste..."
						fieldClass = (Class<? extends Field<?>>) Class.forName("de.pseudonymisierung.mainzelliste." + fieldClassStr);
					}
					this.FieldTypes.put(fieldName, fieldClass);
					logger.debug("Initialized field " + fieldName + " with class " + fieldClass);
				} catch (Exception e) {
					logger.fatal("Initialization of field " + fieldName + " failed: ", e);
					throw new InternalErrorException();
				}
			}
		}

		// Parse blockingkey extractors after the matcher as the blocking may depend on the matcher config
		this.blockingKeyExtractors = new BlockingKeyExtractors(props);

		// Read allowed origins for cross domain resource sharing (CORS)
		allowedOrigins = new HashSet<>();
		String allowedOriginsString = props.getProperty("servers.allowedOrigins");
		if (allowedOriginsString != null)
			allowedOrigins.addAll(Arrays.asList(allowedOriginsString.trim().split("[;,]")));

		allowedHeaders = props.getProperty("servers.allowedHeaders","mainzellisteApiVersion,mainzellisteApiKey")
				.trim()
				.replace(';', ',');
		allowedMethods = props.getProperty("servers.allowedMethods", "OPTIONS,GET,POST")
				.trim()
				.replace(';', ',');

		try {
			String allowedMaxAgeString = props.getProperty("servers.allowedMaxAge", "600");
			allowedMaxAge = Integer.parseInt(allowedMaxAgeString);
			if(allowedMaxAge < -1){
				throw new InvalidConfigurationException("The servers.allowedMaxAge parameter is in an unexpected format: " + allowedMaxAge + ". Expected number greater than -1");
			}
		} catch (NumberFormatException e){
			throw new InvalidConfigurationException("The servers.allowedMaxAge parameter is in an unexpected format: " + allowedMaxAge + ". Expected numeric value", e);
		}

		// Read cryptographic key
		getVariableSubProperties("crypto.key").forEach((v, p) -> {
			try {
				cryptographicKeys
						.put(v, CryptoUtil.readKey(p.getProperty("type"),
								readFileFromURL(p.getProperty("uri").trim())));
			} catch (IOException | InvalidKeySpecException e) {
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.key." + v + ".uri", p.getProperty("uri"), e);
				logger.error(exception.getMessage(), e);
				throw exception;
			} catch (IllegalArgumentException e) {
				// unsupported key type
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.key." + v + ".type", p.getProperty("type"), e);
				logger.error(exception.getMessage(), e);
				throw exception;
			} catch (UnsupportedOperationException e) {
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.key." + v + ".*", "Instantiation of a cryptographic key failed", e);
				logger.error(exception.getMessage(), e);
				throw exception;
			}
		});

		// Read encryption
		getVariableSubProperties("crypto.encryption").forEach((v, p) -> {
			try {
				encryptionMap
						.put(v, CryptoUtil.createEncryption(p.getProperty("type"),
								cryptographicKeys.get(p.getProperty("key"))));
			} catch (InvalidKeySpecException e) {
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.encryption." + v + ".key", p.getProperty("key"), e);
				logger.error(exception.getMessage(), e);
				throw exception;
			} catch (IllegalArgumentException e) { // unsupported encryption type
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.encryption." + v + ".type", p.getProperty("type"), e);
				logger.error(exception.getMessage(), e);
				throw exception;
			} catch (UnsupportedOperationException e) {
				InvalidConfigurationException exception = new InvalidConfigurationException(
						"crypto.encryption." + v + ".*", "Instantiation of a encryption failed", e);
				logger.fatal(exception.getMessage(), e);
				throw exception;
			}
		});

		// Read version number provided by pom.xml
		version = readVersion();

		// read gui configuration
		this.guiConfig = new GUI(props);
	}

	/**
	 * Attempts to read config from path specified in an environment variable
	 *
	 */
	public Properties readConfigFromEnv(String env) {
		Properties props = new Properties();

		if (System.getenv(env) != null) {
			String ConfigDirsAsString = System.getenv(env);
			String[] ConfigDirs =  ConfigDirsAsString.split("::");
			int i = 0;
			props = null;
			while (props == null && i < ConfigDirs.length) {
				File configFile = new File (ConfigDirs[i], "mainzelliste.conf");
				logger.info("Try to read configuration from path " + configFile.getAbsolutePath() + "...");
				try {
					props = readConfigFromFile(configFile.getAbsolutePath());
					i++;
				} catch (IOException e)	{
					logger.fatal("Error reading configuration file. Please configure according to installation manual.", e);
					throw new Error(e);
				}
			}

		}
		return props;
	}

	/**
     * Reads subConfiguration.{n}.uri(s) and adds the values to Config.props
     *
     */
    private void addSubConfigurationPropertiesToProps() {
		// get custom config attributes
		List<String> subConfigurations = props.stringPropertyNames().stream().filter(k -> Pattern.matches("subConfiguration\\.\\d+\\.uri", k.trim()))
				.map(k -> Integer.parseInt(k.split("\\.")[1])).sorted()
				.map(d -> "subConfiguration." + d + ".uri")
				.collect(Collectors.toList());


        for (String attribute : subConfigurations) {
            // read custom configuration properties from url
            Properties subConfigurationProperties;
            try {
                URL subConfigurationURL = new URL(props.getProperty(attribute).trim());
                subConfigurationProperties = readConfigFromUrl(subConfigurationURL);
                logger.info("Sub configuration file " + attribute + " = " + subConfigurationURL + " has been read in.");
            }  catch (MalformedURLException e) {
                logger.fatal("Custom configuration file '" + attribute +
                        "' could not be read from provided URL " + attribute, e);
                throw new Error(e);
            } catch (IOException e) {
                logger.fatal("Error reading custom configuration file '" + attribute +
                        "'. Please configure according to installation manual.", e);
                throw new Error(e);
            }

            // merge configuration files
            for (String currentKey : subConfigurationProperties.stringPropertyNames()) {
                if(props.containsKey(currentKey) && !subConfigurationProperties.getProperty(currentKey).trim()
                        .equals(props.getProperty(currentKey).trim())) {
                    String msg = "Sub configuration tries to override main config or former sub config values. This is not allowed. Property key: " + currentKey +
                            ", custom configuration file: " + attribute;
                    logger.fatal(msg);
                    throw new Error(msg);
                }
            }
            props.putAll(subConfigurationProperties);
        }
    }

    /**
	 * Get the {@link RecordTransformer} instance configured for this instance.
	 * @return The {@link RecordTransformer} instance configured for this instance.
	 */
	public RecordTransformer getRecordTransformer() {
		return recordTransformer;
	}

	/**
	 * Get the {@link BlockingKeyExtractors} instance configured for this instance.
	 * @return The {@link BlockingKeyExtractors} instance configured for this instance.
	 */
	public BlockingKeyExtractors getBlockingKeyExtractors() {
		return this.blockingKeyExtractors;
	}

	/**
	 * Get configuration as Properties object.
	 * @return Properties object as read from the configuration file.
	 */
	public Properties getProperties() {
		return props;
	}

	/**
	 * Get the matcher configured for this instance.
	 * @return The matcher configured for this instance.
	 */
	public Matcher getMatcher() {
		return matcher;
	}

	/**
	 * Get the specified property from the configuration.
	 * @param propKey Property name.
	 * @return The property value or null if no such property exists.
	 */
	public String getProperty(String propKey){
		return props.getProperty(propKey);
	}

	/**
	 * Get the names of fields configured for this instance.
	 * @return The names of fields configured for this instance.
	 */
	public Set<String> getFieldKeys(){
		return FieldTypes.keySet();
	}

	/**
	 * Get the type of the given field.
	 * @param fieldKey Name of the field as defined in configuration.
	 * @return The field type as the matching subclass of Field.
	 */
	public Class<? extends Field<?>> getFieldType(String fieldKey){
		assert FieldTypes.keySet().contains(fieldKey);
		return FieldTypes.get(fieldKey);
	}

	/**
	 * Check whether a field with the given name is configured.
	 * @param fieldName The field name to check
	 * @return true if field fieldName is configured.
	 */
	public boolean fieldExists(String fieldName) {
		return this.FieldTypes.containsKey(fieldName);
	}

	/**
	 * Get the distribution instance (e.g. the name of the project this instance
	 * runs for).
	 *
	 * @return The value of configuration parameter "dist".
	 * */
	public String getDist() {
		return getProperty("dist");
	}

	/**
	 * Get the software version of this instance.
	 *
	 * @return The software version of this instance.
	 *
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Checks whether this instance is run in debug mode, i.e. authentication is
	 * disabled and tokens are not invalidated.
	 *
	 * @return true if debug mode is enabled.
	 */
	public boolean debugIsOn()
	{
		String debugMode = this.props.getProperty("debug");
		return (debugMode != null && debugMode.equals("true"));
	}

	/**
	 * Checks whether this instance has audit trailing enabled by the administrator.
	 *
	 * @return true if audit trail is enabled.
	 */
	public boolean auditTrailIsOn() {
		String audittrail = this.props.getProperty("gcp.audittrail");
		return (audittrail != null && audittrail.equals("true"));
	}

	/**
	 * Checks whether the given origin is allowed. Used to check the origin in
	 * Cross Domain Resource Sharing.
	 *
	 * @param origin
	 *            The origin to check.
	 * @return true if resource sharing with this origin is allowed.
	 */
	public boolean originAllowed(String origin) {
		return this.allowedOrigins.contains(origin);
	}

	/**
	 * Returns the configured allowed Headers for Cross Domain Resource Sharing
	 * @return list of headers set in config servers.allowedHeaders, default is: "mainzellisteApiVersion,mainzellisteApiKey"
	 */
	public String getAllowedHeaders() {
		return String.join(",", this.allowedHeaders);
	}

	/**
	 * Returns the configured allowed methods for Cross Domain Resource Sharing
	 * @return list of headers set in config servers.allowedMethods, default is: "OPTIONS,GET,POST"
	 */
	public String getAllowedMethods() {
		return String.join(",", this.allowedMethods);
	}

	/**
	 * Returns the configured allowed time CORS Preflight requests should be cached
	 * @return list of headers set in config servers.allowedMaxAge, default is: "600"
	 */
	public int getAllowedMaxAge() {
		return this.allowedMaxAge;
	}

	/**
	 * Get the logo file from the path defined by configuration parameter
	 * 'operator.logo'.
	 *
	 * @return The file object. It is checked that the file exists.
	 * @throws FileNotFoundException
	 *             if the logo file cannot be found at the specified location.
	 */
	public URL getLogo() throws FileNotFoundException {
		String logoFileName = this.getProperty("operator.logo");
		if (logoFileName == null || logoFileName.equals(""))
			throw new FileNotFoundException("No logo file configured.");
		File logoFile;
		URL logoURL;
		try {
			logoURL = Initializer.getServletContext().getResource(logoFileName);
		} catch (MalformedURLException e) {
			throw new FileNotFoundException(e.toString());
		}
		if (logoURL != null) {
			return logoURL;
		} else {
			logoFile = new File(logoFileName);

			try {
				if (logoFile.exists())
					return logoFile.toURI().toURL();
				throw new FileNotFoundException("No logo file found at " + logoFileName + ".");
			} catch (MalformedURLException e) {
				throw new FileNotFoundException("No logo file found at " + logoFileName + ". (" + e.toString() + ")");
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
		ServletContext context = Initializer.getServletContext();
		// First, try to read from resource (e.g. within the war file)
		InputStream configInputStream = context.getResourceAsStream(configPath);
		// Else: read from file System
		if (configInputStream == null) {
			File f = new File(configPath);
			if (f.exists())
				configInputStream = new FileInputStream(configPath);
			else return null;
		}
		return readConfigFromInputStream(configInputStream);
	}

	/**
	 * Read configuration from the given URL.
	 *
	 * @param url
	 *            url of the configuration file.
	 * @return The configuration as a Properties object
	 * @throws IOException
	 *             If an I/O error occurs while reading the configuration file.
	 */
	private Properties readConfigFromUrl(URL url) throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(url.openStream()) ) {
			return readConfigFromInputStream(in);
		}
	}

	/**
	 * Read configuration from the given input stream.
	 *
	 * @param configInputStream
	 *            input stream of the configuration file.
	 * @return The configuration as a Properties object
	 * @throws IOException
	 *             If an I/O error occurs while reading the configuration file.
	 */
	private Properties readConfigFromInputStream(InputStream configInputStream) throws IOException {
		Reader reader = new InputStreamReader(configInputStream, StandardCharsets.UTF_8);
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

	/**
	 * Get resource bundle for the best matching locale of the request. The best
	 * matching locale is determined by iterating through the following list of
	 * language codes and choosing the first for which a resource bundle file is
	 * found:
	 * <ol>
	 * <li>If present, value of the URL parameter 'language'.
	 * <li>Any language codes provided in HTTP header "Accept-Language" in the
	 * given order.
	 * <li>"en" as fallback (i.e. English is the default language).
	 * </ol>
	 *
	 * @param req
	 *            The servlet request.
	 * @return The matching resource bundle.
	 */
	public ResourceBundle getResourceBundle(HttpServletRequest req) {

		// Look if there is a cached ResourceBundle instance for this request
		final String storedResourceBundleKey = this.getClass().getName() + ".selectedResourceBundle";
		Object storedResourceBundle = req.getAttribute(storedResourceBundleKey);
		if (storedResourceBundle != null && storedResourceBundle instanceof ResourceBundle)
				return (ResourceBundle) storedResourceBundle;

		// Base name of resource bundle files
		String baseName = "MessageBundle";

		// Build a list of preferred locales
		LinkedList<Locale> preferredLocales = new LinkedList<Locale>();
		// First preference: Fixed language in configuration file
		String languageConfig = Config.instance.getProperty("language");
		if (languageConfig != null) {
			Locale configLocale = new Locale(languageConfig);
			preferredLocales.add(configLocale);
		}
		// Second preference: URL parameter "language", if set.
		String languageParam = req.getParameter("language");
		if (languageParam != null) {
			Locale urlLocale = new Locale(languageParam);
			preferredLocales.add(urlLocale);
		}

		// Next, add all preferred locales from the request (header "Language").
		Enumeration<Locale> requestLocales = req.getLocales();
		while (requestLocales.hasMoreElements()) {
			preferredLocales.add(requestLocales.nextElement());
		}

		// Finally, add English as fallback
		preferredLocales.add(Locale.ENGLISH);

		// Instantiate control object for searching resources without using default locale,
		// as default locale is searched for explicitly.
		ResourceBundle.Control noFallback = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

		// Iterate over list of locales and return the first matching resource bundle
		for (Locale thisLocale : preferredLocales) {
			  // Try to get ResourceBundle for current locale
			   try {
				   ResourceBundle selectedResourceBundle = ResourceBundle.getBundle("MessageBundle", thisLocale, noFallback);
				   // Cache ResourceBundle object for further calls to this method within the same HTTP request
				   req.setAttribute(storedResourceBundleKey, selectedResourceBundle);
				   return selectedResourceBundle;
			   } catch (MissingResourceException e) {
				   // Silently try next preferred locale
			   }
		}

		// If this line is reached, no resource bundle (including system default) could be found, which is an error
		throw new Error ("Could not find resource bundle with base name '" + baseName + "' for any of the locales: " + preferredLocales);
	}

	/**
	 * Returns application name and version for use in HTTP headers Server and
	 * User-Agent. Format: "Mainzelliste/x.y.z", with the version as returned by
	 * {@link #getVersion()}.
	 *
	 * @return The version string.
	 */
	public String getUserAgentString() {
		return "Mainzelliste/" + getVersion();
	}

	/**
	 * Get configured log level (DEBUG by default).
	 * @return The log level.
	 */
	Level getLogLevel() {
		String level = this.props.getProperty("log.level");
		Level ret = Level.DEBUG;

		if (level == null || level.equals("DEBUG"))
			ret = Level.DEBUG;
		else if (level.equals("WARN"))
			ret = Level.WARN;
		else if (level.equals("ERROR"))
			ret = Level.ERROR;
		else if (level.equals("FATAL"))
			ret = Level.FATAL;
		else if (level.equals("INFO"))
			ret = Level.INFO;

		return ret;
	}

	/**
	 * Get gui configuration
	 */
	public GUI getGuiConfiguration()
	{
		return guiConfig;
	}

	/**
	 * Read version string from properties file "version.properties",
	 * which is copied from pom.xml by Maven.
	 * @return The version string.
	 */
	private String readVersion() {
		try {
			Properties props = new Properties();
			InputStream versionInputStream = Initializer.getServletContext().getResourceAsStream("/WEB-INF/classes/version.properties");
			if (versionInputStream == null) {
				// Try alternate way of reading file (necessary for running test via the Jersey Test Framework) 
				versionInputStream = this.getClass().getResourceAsStream("/version.properties");
			}
			if (versionInputStream == null) {
				throw new Error("File version.properties not found!");
			}
			props.load(versionInputStream);
			String version = props.getProperty("version");
			if (version == null)
				throw new Error("property 'version' undefined in version.properties");
			return version;
		} catch (IOException e) {
			throw new Error ("I/O error while reading version.properties", e);
		}
	}

	public Encryption getEncryption(String encryptionName) {
		return encryptionMap.get(encryptionName);
	}

	public static class GUI {
		/** url of control number generator */
		public final String cngUrl;
		/** api key of control number generator */
		public final String cgnApiKey;
		/** version of mainzelliste (ml) rest api */
		public final String mlApiVersion;

		private GUI(Properties properties) {
			this.cngUrl = Optional.ofNullable((String)properties.get("gui.cng.url")).orElse("");
			this.cgnApiKey = Optional.ofNullable((String)properties.get("gui.cng.apiKey")).orElse("");
			this.mlApiVersion = Optional.ofNullable((String)properties.get("gui.ml.apiVersion")).orElse("");
		}
	}

	// HELPERS

	/**
	 * transform a configuration entry in the following format : <br>
	 * 	{@code prefix.<variable>.<propertyKey> = <propertyValue>} <br>
	 * in a map with {@code <variable>} as key and the given suffix {@code <propertyKey>} together
	 * with the value {@code <propertyValue>} in property list as value.
	 * @param prefix configuration key prefix
	 * @return a map with variable name as key and its properties as value
	 */
	private Map<String, Properties> getVariableSubProperties(String prefix) {
		Map<String, Properties> childrenPropertiesMap = new HashMap<>();
		// property key should look like this : prefix.<var>.suffix
		props.stringPropertyNames()
				.stream()
				.filter(k -> Pattern.matches("^" + prefix + "\\.\\w+\\..+", k.trim()))
				.forEach(k -> {
					String subKey = k.substring(prefix.length() + 1); // remove prefix from key
					childrenPropertiesMap.compute(
							subKey.split("\\.")[0], // get "<var>" @see example above
							(newK, newProperties) -> addProperty(
									newProperties,
									subKey.substring(newK.length() + 1), // get "suffix" @see example above
									props.getProperty(k)));         // get property value
				});
		return childrenPropertiesMap;
	}

	private Properties addProperty(Properties properties, String key, String value) {
		if(properties == null) {
			properties = new Properties();
		}
		properties.setProperty(key, value);
		return properties;
	}

	/**
	 * read file from the given url
	 * @param fileURL url of the file (e.g. file:///etc/keys/private.der)
	 * @return byte array
	 */
	private byte[] readFileFromURL(String fileURL) throws IOException {
		try (InputStream inputStream = new URL(fileURL).openStream() ) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[0xFFFF];
			for (int len = inputStream.read(buffer); len != -1; len = inputStream.read(buffer)) {
				outputStream.write(buffer, 0, len);
			}
			return outputStream.toByteArray();
		}
	}
}
