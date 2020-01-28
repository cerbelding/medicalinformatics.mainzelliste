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
package de.pseudonymisierung.mainzelliste.matcher;

import java.util.Properties;

import de.pseudonymisierung.mainzelliste.Patient;

/**
 * This interface is reponsible for comparing a given patient to those present
 * in the local database.
 */
public interface Matcher {

	/**
	 * Initialize with configuration properties.
	 *
	 * @param props
	 *            Properties as read from the configuration file.
	 */
	public void initialize(Properties props);

	/**
	 * Compare a patient to a list of existing patiens.
	 *
	 * @param a
	 *            The patient too look up.
	 * @param patientList
	 *            The list or patients to match against.
	 * @return The match result.
	 *
	 * @see MatchResult
	 */
	public MatchResult match(Patient a, Iterable<Patient> patientList);
}
