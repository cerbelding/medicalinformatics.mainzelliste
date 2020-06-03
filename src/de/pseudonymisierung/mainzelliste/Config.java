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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

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

	/** Logging instance */
	private Logger logger = Logger.getLogger(Config.class);

	/** Allowed origins for Cross Domain Resource Sharing. */
	private Set<String> allowedOrigins;

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
		Pattern pattern = Pattern.compile("field\\.(\\w+)\\.type");
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

		// Read allowed origins for cross domain resource sharing (CORS)
		allowedOrigins = new HashSet<String>();
		String allowedOriginsString = props.getProperty("servers.allowedOrigins");
		if (allowedOriginsString != null)
			allowedOrigins.addAll(Arrays.asList(allowedOriginsString.trim().split(";")));

		// Read version number provided by pom.xml
		version = readVersion();
	}

	/**
	 * Get the {@link RecordTransformer} instance configured for this instance.
	 * @return The {@link RecordTransformer} instance configured for this instance.
	 */
	public RecordTransformer getRecordTransformer() {
		return recordTransformer;
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
}
