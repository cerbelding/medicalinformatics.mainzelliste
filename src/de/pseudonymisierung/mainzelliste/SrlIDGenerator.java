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
/**
 *
 */
package de.pseudonymisierung.mainzelliste;

import org.codehaus.jettison.json.JSONObject;

import java.util.Properties;


/**
 * Simple ID generator that outputs consecutive SrlIDs. For testing purposes
 * or to produce IDs for a database.
 */
public class SrlIDGenerator implements IDGenerator<SrlID> {

	/** Internal counter. Incremented on every ID creation. */
	int counter;
	/** The state information of this generator. */
	IDGeneratorMemory mem;
	/** The ID type this generator instance creates. */
	String idType;

	@Override
	public void init(IDGeneratorMemory mem, String idType, Properties props) {
		this.mem = mem;

		String memCounter = mem.get("counter");
		if(memCounter == null) memCounter = "0";
		this.counter = Integer.parseInt(memCounter);
		this.idType = idType;
	}

	@Override
	public void reset(String idType) {
		this.mem.set("counter", "0");
		this.mem.commit();
	}

	@Override
	public synchronized SrlID getNext() {
		SrlID newID = new SrlID(Integer.toString(this.counter + 1), idType);
		this.counter++;
		this.mem.set("counter", Integer.toString(this.counter));
		this.mem.commit();
		return newID;
	}

	@Override
	public SrlID buildId(String id) {
		return new SrlID(id, this.idType);
	}

	@Override
	public boolean verify(String id) {
		try {
			Integer.parseInt(id);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String correct(String IDString) {
		try {
			Integer.parseInt(IDString);
		} catch (NumberFormatException e) {
			return null;
		}
		return IDString;
	}

	@Override
	public String getIdType() {
		return idType;
	}

	@Override
	public boolean isExternal() { return false; }

	@Override
	public boolean isSrl() { return true; }
}
