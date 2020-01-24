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
 *
 *
 * This file is a Java port of the PID generation code by Klaus Pommerening.
 * The original copyright notice follows:
 *
 **** PIDgen.c *************************************************
 *                                                             *
 * Functions to support a pseudonymization service             *
 *-------------------------------------------------------------*
 * Klaus Pommerening, IMSD, Johannes-Gutenberg-Universitaet,   *
 *   Mainz, 3. April 2001                                      *
 *-------------------------------------------------------------*
 * Version 1.00, 29. Mai 2004                                  *
 ***************************************************************
 */
package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.Random;

/**
 * This generator creates IDs from a vocabulary that can be set with cofnig
 */
public class ElasticIDGenerator implements IDGenerator<ElasticID>{

	/** The ID type this generator instance produces. */
	private String idType;
	/** The IDGeneratorMemory instance for this generator. */
	private IDGeneratorMemory mem;

	/** Counter, increased with every created id. */
	private int counter = 1;
	/** Length of a valid PID */
	private int idLength = 8;

	/**
	 * Vocabulary for the generated IDs. <p>
	 * The value is read from mainzelliste configuration. If no value is applied, 
	 * default will be used: <strong>0123456789ACDEFGHJKLMNPQRTUVWXYZ</strong>
	 */
	private char vocabulary[] = "0123456789ACDEFGHJKLMNPQRTUVWXYZ".toCharArray();

	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Empty constructor. Needed by IDGeneratorFactory in order to instantiate
	 * an object via reflection.
	 */
	public ElasticIDGenerator() {
	}

	@Override
	public void init(IDGeneratorMemory mem, String idType, Properties props) {
		this.mem = mem;

		String memCounter = mem.get("counter");
		if(memCounter == null) memCounter = "0";
		this.counter = Integer.parseInt(memCounter);

		this.idType = idType;

		try {
			if (props.containsKey("length"))
				this.idLength = Integer.parseInt(props.getProperty("length"));
			if(props.containsKey("vocabulary"))
				vocabulary = props.getProperty("vocabulary").toCharArray();
		} catch (NumberFormatException e) {
			logger.fatal("Number format error in configuration of IDGenerator for ID type " + idType, e);
			throw new InternalErrorException(e);
		}
	}

	/**
	 * Create a id for the given counter. IDs are generated from vocabulary for a specific length.
	 * This method returns the i-th Id of this generator.
	 * @param counter Order of the ID to get.
	 * @return The generated PID string.
	 */
	private String createPIDString(int counter) {

		Random randomGenerator = new Random(counter);
		StringBuilder stringBuilder = new StringBuilder();

		for(int i = 0; i < idLength; i++){
			int randomNumber = randomGenerator.nextInt(this.vocabulary.length);
			stringBuilder.append(this.vocabulary[randomNumber]);
		}

		return stringBuilder.toString();
	}

	@Override
	public synchronized ElasticID getNext() {

		String id = null;

		// check if id is new, otherwise generate new id
		while(id == null || checkPidExists(id)){
		 id = createPIDString(this.counter + 1);
		}

		this.counter++;
		mem.set("counter", Integer.toString(this.counter));
		mem.commit();

		return new ElasticID(id, idType);
	}
	/**
		This method checks if a patient exists in database
	 	@return returns true when the patient exists in database, otherwise false
	 */
	private boolean checkPidExists(String pid) {
		Patient patient = Persistor.instance.getPatient(new ElasticID(pid, getIdType()));
		return patient != null;
	}

	@Override
	public boolean verify(String id) {
		return true;
	}

	@Override
	public String correct(String PIDString) {
		throw new NotImplementedException();
	}

	@Override
	public ElasticID buildId(String id) {
		return new ElasticID(id, getIdType());
	}

	@Override
	public String getIdType() {
		return idType;
	}

	@Override
	public boolean isExternal() { return false; }
}
