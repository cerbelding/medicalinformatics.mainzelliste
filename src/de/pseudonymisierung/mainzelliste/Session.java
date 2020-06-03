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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.pseudonymisierung.mainzelliste.webservice.Token;

/**
 * A Session serves as a container for a set of Tokens to be handled (i.e. invalidated) together.
 */
public class Session extends ConcurrentHashMap<String, String>{

	@SuppressWarnings("javadoc")
	private static final long serialVersionUID = -5538915343663250244L;

	/** A unique identifier. */
	private String sessionId;
	/** URI of the session. */
	private URI uri;
	/** The set of tokens belonging to this Session. */
	private Set<Token> tokens = new HashSet<Token>();
	/** The set of patients belonging to this Session */
	private HashMap<ID, Patient> patients = new HashMap<ID, Patient>();

	/**
	 * The time of the last access to this Session. Updateable via
	 * refresh();
	 */
	private Date lastAccess;

	/**
	 * Get the time of the last access to this session.
	 *
	 * @return Time of last access.
	 */
	public Date getLastAccess() {
		return lastAccess;
	}

	/**
	 * Create an instance with the given id. The created session will not be
	 * registered, i.e. reachable by the REST interface. Use
	 * {@link Servers#newSession()} for this purpose.
	 *
	 * @param id
	 *            The session id.
	 */
	public Session(String id) {
		sessionId = id;
		try {
			// Assign provisional URI in case it is not set.
			uri = new URI(Initializer.getServletContext().getContextPath() + "/")
			.resolve("sessions/")
			.resolve(id);
		} catch (URISyntaxException e) {
			throw new Error("Error while creating URI from ServletContext", e);
		}
		this.refresh();
	}

	/**
	 * Set the timestamp of last access to the current time. Called on every
	 * access to the session via the REST interface.
	 */
	public void refresh() {
		lastAccess = Calendar.getInstance().getTime();
	}

	/**
	 * Get the unique id of this session.
	 *
	 * @return The session id.
	 */
	public String getId(){
		return sessionId;
	}

	/**
	 * Set the URI of this session.
	 *
	 * @param uri
	 *            The URI to set.
	 */
	public void setURI(URI uri) {
		this.uri = uri;
	}

	/**
	 * Get the URI of this session.
	 *
	 * @return The session URI.
	 */
	public URI getURI() {
		return uri;
	}

	/**
	 * Delete this session and unregister all of its tokens.
	 */
	void destroy(){
		Servers.instance.deleteSession(getId());
	}

	/**
	 * Get all tokens belonging to this session.
	 *
	 * @return The tokens as an unmodifiable set.
	 */
	public Set<Token> getTokens() {
		synchronized(tokens){
			this.refresh();
			return Collections.unmodifiableSet(tokens);
		}
	}

	/**
	 * Add a token to this session.
	 *
	 * @param t
	 *            The token to add.
	 */
	public void addToken(Token t){
		synchronized(tokens){
			tokens.add(t);
		}
		this.refresh();
	}

	/**
	 * Delete a token from this session. This will not unregister the token from
	 * the server, use {@link Servers#deleteToken(String)} for this purpose.
	 *
	 * @param t
	 *            The token to remove.
	 */
	public void deleteToken(Token t) {
		synchronized(tokens){
			tokens.remove(t);
		}
		this.refresh();
	}

	/**
	 * Remove all tokens in this session. Called by
	 * {@link Servers#deleteSession(String)}. This will not unregister the
	 * tokens from the server.
	 */
	public void deleteAllTokens() {
		synchronized(tokens){
			tokens.clear();
		}
	}
	
	/**
	 * Add a patient to the set of patients related to this session.
	 * 
	 * @param p
	 *            The patient to add.
	 */
	public void addPatient(Patient p) {
		for (ID thisId : p.getIds()) {
			patients.put(thisId, p);
		}
	}

	/**
	 * Remove a patient from the set of patients related to this session.
	 * 
	 * @param p
	 *            The patient to remove.
	 */
	public void deletePatient(Patient p) {
		if (p == null)
			return;
		for (ID thisId : p.getIds()) {
			patients.remove(thisId);
		}
	}

	/**
	 * Clear the set of patients related to this session.
	 */
	public void deleteAllPatients() {
		patients.clear();
	}

	/**
	 * Get the set of patients related to this session.
	 * 
	 * @return The set of patients.
	 */
	public Set<Patient> getPatients() {
		return new HashSet<Patient>(patients.values());
	}
}
