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

import java.util.Optional;
import java.util.Properties;

/**
 * Generator for a given type of ID (e.g. PID, SIC, LabID, ...).
 *
 * Each type of ID needs an own generator - even if based on the same algorithm,
 * in which case there would be several instances of the same generator
 * implementation.
 *
 * Implementations of this interface must provide an empty constructor and
 * perform necessary initializations via
 * {@link #init(IDGeneratorMemory, String, boolean, Properties)}.
 *
 * @param <I>
 *            Type of ID to be generated by this Generator.
 */
public interface IDGenerator<I extends ID> {

	/**
	 * Called by the IDGeneratorFactory.
	 *
	 * @param mem                    This allows the generator to "memorize" values, e.g. sequence
	 *                               counters.
	 * @param idType                 "Name" of the generated IDs, e.g. "gpohid".
	 * @param eagerGenRelatedIdTypes indicate with which id types the configured ID will be generated
	 *                               eagerly
	 * @param props                  Properties for this generator.These are the properties defined
	 *                               for this generator in the configuration, with the prefix
	 *                               idgenerators.{idtype} removed.
	 */
	void init(IDGeneratorMemory mem, String idType, String[] eagerGenRelatedIdTypes,
			Properties props);

	/**
	 * This is the method to call to generate a new unique (in its type) ID.
	 *
	 * @return A new ID instance.
	 */
	I getNext();

	/**
	 * Generates (and, if possible, verifies) an ID instance based on an
	 * existing IDString.
	 *
	 * @param id
	 *            String representation of the ID to be instantiated.
	 * @return An ID instance initialized with the given ID string and the ID
	 *         type of this IDGenerator.
	 */
	public I buildId(String id);

	/**
	 * Checks whether a given String is a valid ID. Implementations can consist
	 * of a simple data type check or on more sophisticated algorithms like
	 * {@link PIDGenerator#verify(String)}
	 *
	 * @param idString
	 *            The ID string which to check.
	 * @return true If id is a correct ID, false otherwise.
	 */
	public boolean verify(String idString);

	/**
	 * Tries to correct the given ID String. This method is only useful if the
	 * implementation uses an error-tolerant code which allows corrections of
	 * errors, for example {@link PIDGenerator#correct(String)}.
	 *
	 * @param idString The ID string which to correct.
	 * @return Correct IDString or null if impossible to correct.
	 */
	public String correct(String idString);

	/**
	 * Gets the type ("name") of IDs this generator produces, e.g. "gpohid".
	 *
	 * @return Name of ID.
	 */
	public String getIdType();

	/**
	 * Check whether an ID generator is a dummy generator for externally
	 * generated IDs.
	 */
	public boolean isExternal();

	/**
	 * Check whether an ID generator persists generated IDs
	 */
	public boolean isPersistent();

	/**
	 * Get the {@link IDGeneratorMemory} if it exists.
	 * This method could have a default implementation with Optional.empty(),
	 * but it does not to prevent developers from forgetting to override it.
	 * @return Optional of id generator memory
	 */
	Optional<IDGeneratorMemory> getMemory();

	/**
	 * return whether ID of the configured type should be created eagerly
	 *
	 * @return true if eager generation of this ID type is enabled
	 */
  boolean isEagerGenerationOn(String idType);
}
