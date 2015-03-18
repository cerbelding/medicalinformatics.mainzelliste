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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.*;

/**
 * Configuration of the patient list. Implemented as a singleton object, which can be referenced
 * by Config.instance. The configuration is read from the properties file specified as
 * parameter de.pseudonymisierung.mainzelliste.ConfigurationFile in context.xml
 * (see {@link java.util.Properties#load(InputStream) java.util.Properties}).
 */
public enum Config {
	instance;
	
	public enum FieldType {
		PLAINTEXT,
		PLAINTEXT_NORMALIZED,
		HASHED, // Bloomfilter without prior normalization
		HASHED_NORMALIZED; // Bloomfilter with prior normalization
	}
	
	private final String version = "1.3.2";
	
	private final Map<String,Class<? extends Field<?>>> FieldTypes;
	
	private Properties props;
	private RecordTransformer recordTransformer;
	private Matcher matcher;
	
	private Logger logger = Logger.getLogger(Config.class);
	
	@SuppressWarnings("unchecked")
	Config() throws InternalErrorException {
		props = new Properties();
		try {
			ServletContext context = Initializer.getServletContext();
			String configPath = context.getInitParameter("de.pseudonymisierung.mainzelliste.ConfigurationFile");

			if (configPath == null) configPath = "/WEB-INF/classes/mainzelliste.conf";
			logger.info("Reading config from path " + configPath + "...");
			
			// First, try to read from resource (e.g. within the war file)
			InputStream configInputStream = context.getResourceAsStream(configPath);
			// Else: read from file System			
			if (configInputStream == null)
				configInputStream = new FileInputStream(configPath);
			
			Reader reader = new InputStreamReader(configInputStream, "UTF-8");
			props.load(reader);

			/* 
			 * Read properties into Preferences for easier hierarchical access
			 * (e.g. it is possible to get the subtree of all idgenerators.* properties)
			 */
			Preferences prefs = Preferences.userRoot().node("de/pseudonymisierung/mainzelliste");
			for (Object propName : props.keySet()) {
				Preferences prefNode = prefs;
				// Create a path in the preferences according to the property key.
				// (Path separated by ".") The last element is used as parameter name. 
				String prefKeys[] = propName.toString().split("\\.", 0);
				for (int i = 0; i < prefKeys.length - 1; i++)
					prefNode = prefNode.node(prefKeys[i]);
				prefNode.put(prefKeys[prefKeys.length - 1], props.getProperty(propName.toString()));
			}					
			configInputStream.close();
			logger.info("Config read successfully");
			logger.debug(props);
			
		} catch (IOException e)	{
			//TODO: Hilfreichere Fehlermeldung ausgeben. Am besten direkt crashen, damit Meldung ganz unten steht.
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
	}
	
	public RecordTransformer getRecordTransformer() {
		return recordTransformer;
	}

	public Properties getProperties() {
		return props;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public String getProperty(String propKey){
		return props.getProperty(propKey);
	}
	
	public Set<String> getFieldKeys(){
		return FieldTypes.keySet();
	}
	
	public Class<? extends Field<?>> getFieldType(String FieldKey){
		assert FieldTypes.keySet().contains(FieldKey);
		return FieldTypes.get(FieldKey);
	}
	
	public String getDist() {
		return getProperty("dist");
	}
	
	public String getVersion() {
		return version;
	}
	
	public boolean debugIsOn()
	{
		String debugMode = this.props.getProperty("debug");
		return (debugMode != null && debugMode.equals("true"));
	}
	
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
}