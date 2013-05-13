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
package de.pseudonymisierung.mainzelliste.webservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.pseudonymisierung.mainzelliste.Session;

/**
 * A temporary "ticket" to realize authorization and/or access to a resource.
 * Tokens are accessible via their token id (e.g. GET /patients/tempid/{tid}),
 * but also connected to a {@link Session} (e.g. DELETE /sessions/{sid}).
 * Thus, they are created using a session.
 */
public class Token {
	private String id;
	private String type;
	private Map<String, ?> data;
	
	Token() {}
	
	public Token(String tid, String type) {
		this.id = tid;
		this.type = type;
		this.data = new HashMap<String, Object>();		
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Map<String, ?> getData() {
		return data;
	}
	
	/**
	 * Get a particular data element by its key.
	 * This method is preferable to getData().get() as it handles the case data==null safely. 
	 * @param item
	 * @return The requested data item. Null if no such item exists or if no data is attached to
	 * the token (data==null). 
	 */
	public String getDataItemString(String item) {
		if (this.data == null)
			return null;
		else
			return (String) data.get(item);
	}

	public List getDataItemList(String item) {
		if (this.data == null)
			return null;
		else
			return (List) data.get(item);
	}
	
	public Map<String, ?> getDataItemMap(String item) {
		if (this.data == null)
			return null;
		else
			return (Map) data.get(item);
	}

	public void setData(Map<String, ?> data) {
		this.data = data;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Token)) return false;
		
		Token t2 = (Token)obj;
		return t2.id.equals(this.id);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
