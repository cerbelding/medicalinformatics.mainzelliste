/*
 * Copyright (C) 2013 Martin Lablans, Andreas Borg, Frank Ückert
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.dto.Persistor;

/**
 * Factory for IDGenerators.
 */
public enum IDGeneratorFactory {
	instance;
	
	private final Map<String, IDGenerator<? extends ID>> generators;
	
	private String[] idTypes;

	private Logger logger = Logger.getLogger(this.getClass());
	
	private IDGeneratorFactory() {
		HashMap<String, IDGenerator<? extends ID>> temp = new HashMap<String, IDGenerator<? extends ID>>();
		Preferences prefs = Preferences.userRoot().node("de/pseudonymisierung/mainzelliste/idgenerator");
		Properties props = Config.instance.getProperties();
		
		if(!props.containsKey("idgenerators") || props.getProperty("idgenerators").length() == 0) {
			logger.fatal("No ID generators defined!");
			throw new Error("No ID generators defined!");
		}
		
		// split list of ID generators: comma-separated, ignore whitespace around commas
		this.idTypes = props.getProperty("idgenerators").split("\\s*,\\s*");
		
		// Iterate over ID types
		for (String thisIdType : idTypes) {
			String thisIdGenerator = prefs.get(thisIdType, "");
			try {
				// Add mainzelliste package to class name if none is given 
				// (check by searching for a dot in the class name)
				if (!thisIdGenerator.contains("."))
					thisIdGenerator = "de.pseudonymisierung.mainzelliste." + thisIdGenerator;
				IDGenerator<?> thisGenerator = (IDGenerator<?>) Class.forName(thisIdGenerator).newInstance();
				IDGeneratorMemory mem = Persistor.instance.getIDGeneratorMemory(thisIdType);
				if (mem == null)
					mem = new IDGeneratorMemory(thisIdType);				
				// Get properties for this ID generator from Preferences 
				Properties thisIdProps = new Properties();
				Preferences thisIdPrefs = prefs.node(thisIdType);
				for (String key : thisIdPrefs.keys()) {
					thisIdProps.put(key, thisIdPrefs.get(key, ""));
				}
				thisGenerator.init(mem, thisIdType, thisIdProps);
				temp.put(thisIdType, thisGenerator);
			} catch (ClassNotFoundException e) {
				logger.fatal("Unknown ID generator: " + thisIdType);
				throw new Error(e);
			} catch (Exception e) {
				logger.fatal("Could not initialize ID generator " + thisIdType, e);
				throw new Error(e);
			}
		}
		Logger logger = Logger.getLogger(IDGeneratorFactory.class);
		generators = Collections.unmodifiableMap(temp);
		
		logger.info("ID generators have initialized successfully.");
	}
	
	public IDGenerator<? extends ID> getFactory(String idType){
		return generators.get(idType);
	}
	
	/**
	 * Generates a set of IDs for a new patient by calling every ID generator defined
	 * in the configuration.
	 */
	public Set<ID> generateIds() {
		HashSet<ID> ids = new HashSet<ID>();
		for (String idType : this.generators.keySet()) {
			ids.add(this.generators.get(idType).getNext());
		}
		return ids;
	}
	
	/**
	 * Get names of defined id types as array. The result must not be modified.
	 */
	public String[] getIDTypes() {
		return this.idTypes;
	}
	
	/**
	 * Get the default id type (as of 02/2013, the first one defined in the config).
	 */
	public String getDefaultIDType() {
		return this.idTypes[0];
	}
}
