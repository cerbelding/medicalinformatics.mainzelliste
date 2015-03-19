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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

/**
 * Factory for IDGenerators. Implemented as a singleton object, which can be
 * referenced by IDGeneratorFactory.instance.
 */
public enum IDGeneratorFactory {

	/** The singleton instance. */
	instance;

	/** Map of generators, with respective ID types as keys. */
	private final Map<String, IDGenerator<? extends ID>> generators;

	/**
	 * The configured ID types. Must be saved separately as the order is
	 * important for determining the default ID type (see getDefaultIDType())
	 */
	private String[] idTypes;

	/** The logging instance */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Initializes the IDGeneratorFactory. Reads the configuration and sets up
	 * the necessary IDGenerator instances, which are initialized with the
	 * settings stored in the database (see {@link IDGeneratorMemory}).
	 */
	private IDGeneratorFactory() {
		HashMap<String, IDGenerator<? extends ID>> temp = new HashMap<String, IDGenerator<? extends ID>>();
		Preferences prefs = Preferences.userRoot().node(
				"de/pseudonymisierung/mainzelliste/idgenerator");
		Properties props = Config.instance.getProperties();

		if (!props.containsKey("idgenerators")
				|| props.getProperty("idgenerators").length() == 0) {
			logger.fatal("No ID generators defined!");
			throw new Error("No ID generators defined!");
		}

		// split list of ID generators: comma-separated, ignore whitespace
		// around commas
		this.idTypes = props.getProperty("idgenerators").split("\\s*,\\s*");

		// Iterate over ID types
		for (String thisIdType : idTypes) {
			String thisIdGenerator = prefs.get(thisIdType, "");
			try {
				// Add mainzelliste package to class name if none is given
				// (check by searching for a dot in the class name)
				if (!thisIdGenerator.contains("."))
					thisIdGenerator = "de.pseudonymisierung.mainzelliste."
							+ thisIdGenerator;
				IDGenerator<?> thisGenerator = (IDGenerator<?>) Class.forName(
						thisIdGenerator).newInstance();
				IDGeneratorMemory mem = Persistor.instance
						.getIDGeneratorMemory(thisIdType);
				if (mem == null) {
					// Create new memory object and persist.
					mem = new IDGeneratorMemory(thisIdType);
					Persistor.instance.updateIDGeneratorMemory(mem);
					/*
					 * Reread from persistence to ensure to get a persisted
					 * entity. Otherwise future updates generate new objects in
					 * the database.
					 */
					mem = Persistor.instance.getIDGeneratorMemory(thisIdType);
				}
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
				logger.fatal("Could not initialize ID generator " + thisIdType,
						e);
				throw new Error(e);
			}
		}
		Logger logger = Logger.getLogger(IDGeneratorFactory.class);
		generators = Collections.unmodifiableMap(temp);

		logger.info("ID generators have initialized successfully.");
	}

	/**
	 * Get the IDGenerator for the given ID type.
	 * 
	 * @param idType
	 *            The ID type for which to get the IDGenerator.
	 * @return The respective IDGenerator instance or null if the given ID type
	 *         is unknown.
	 */
	public IDGenerator<? extends ID> getFactory(String idType) {
		return generators.get(idType);
	}

	/**
	 * Generates a set of IDs for a new patient by calling every ID generator
	 * defined in the configuration.
	 * 
	 * @return The set of generated IDs.
	 */
	public Set<ID> generateIds() {
		HashSet<ID> ids = new HashSet<ID>();
		for (String idType : this.generators.keySet()) {
			ids.add(this.generators.get(idType).getNext());
		}
		return ids;
	}

	/**
	 * Get names of defined id types as an array. The result must not be
	 * modified.
	 * 
	 * @return The defined id types.
	 */
	public String[] getIDTypes() {
		return this.idTypes;
	}

	/**
	 * Get the default id type (currently, the first one defined in the
	 * configuration).
	 * 
	 * @return The default id type.
	 */
	public String getDefaultIDType() {
		return this.idTypes[0];
	}

	/**
	 * Build an ID with the given ID string and type.
	 * 
	 * @param idType
	 *            The ID type.
	 * @param idString
	 *            The ID string.
	 * @return An ID instance with the given properties.
	 * @throws InvalidIDException
	 *             If the given id type is unknown.
	 */
	public ID buildId(String idType, String idString) {
		if (this.getFactory(idType) == null)
			throw new InvalidIDException(String.format(
					"No ID type %s defined!", idType));
		return this.getFactory(idType).buildId(idString);
	}
}
