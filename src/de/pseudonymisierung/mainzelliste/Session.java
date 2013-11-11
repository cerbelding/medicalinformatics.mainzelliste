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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.pseudonymisierung.mainzelliste.webservice.Token;

/**
 * Bundle for a set of Tokens to be handled (i.e. invalidated) together.
 */
public class Session extends ConcurrentHashMap<String, String>{
	private String sessionId;
	private Set<Token> tokens = new HashSet<Token>();
	
	/** 
	 * The timestamp when this Session was last accessed.
	 * Updateable via refresh();
	 */
	private Date lastAccess;
	
	/**
	 * @return the lastAccess
	 */
	public Date getLastAccess() {
		return lastAccess;
	}

	public Session(String s) {
		sessionId = s;
		this.refresh();
	}
	
	/**
	 * Set the timestamp of last access to the current time.
	 */
	public void refresh() {
		lastAccess = Calendar.getInstance().getTime();
	}
	
	public String getId(){
		return sessionId;
	}

	/**
	 * Delete this session and unregister all its tokens.
	 */
	void destroy(){
		Servers.instance.deleteSession(getId());
	}
	
	public Set<Token> getTokens() {
		synchronized(tokens){
			this.refresh();
			return Collections.unmodifiableSet(tokens);
		}
	}
	
	public void addToken(Token t){
		synchronized(tokens){
			tokens.add(t);
		}
		this.refresh();
	}

	public void deleteToken(String tokenId) {
		synchronized(tokens){
			tokens.remove(tokenId);
		}
		this.refresh();
	}
}
