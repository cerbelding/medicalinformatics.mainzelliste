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
package de.pseudonymisierung.mainzelliste.dto;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
//import java.util.LinkedList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorMemory;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;

/**
 * Handles reading and writing from and to the database.
 */
public enum Persistor {
	instance;
	
	private EntityManagerFactory emf;
	
	private EntityManager em;
	
	private Logger logger = Logger.getLogger(this.getClass());
	
	private Persistor() {
		HashMap<String, String> persistenceOptions = new HashMap<String, String>();
		
		// Settings from config
		persistenceOptions.put("javax.persistence.jdbc.driver", Config.instance.getProperty("db.driver"));
		persistenceOptions.put("javax.persistence.jdbc.url", Config.instance.getProperty("db.url"));
		persistenceOptions.put("javax.persistence.jdbc.user", Config.instance.getProperty("db.username"));
		persistenceOptions.put("javax.persistence.jdbc.password", Config.instance.getProperty("db.password"));
		
		// Other settings
		persistenceOptions.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
		persistenceOptions.put("openjpa.jdbc.DriverDataSource", "dbcp");
		persistenceOptions.put("openjpa.ConnectionProperties", "testOnBorrow=true, validationQuery=SELECT 1");
		
		emf = Persistence.createEntityManagerFactory("mainzelliste", persistenceOptions);
		em = emf.createEntityManager();
		
		new org.apache.openjpa.jdbc.schema.DBCPDriverDataSource();
		// Check database connection
		getPatients();
		
		Logger.getLogger(Persistor.class).info("Persistence has initialized successfully.");
	}
	
	/**
	 * Get a patient by id.
	 */
	public Patient getPatient(ID pid){
		EntityManager em = emf.createEntityManager();
		TypedQuery<Patient> q = em.createQuery("SELECT p FROM Patient p JOIN p.ids id WHERE id.idString = :idString AND id.type = :idType", Patient.class);
		q.setParameter("idString", pid.getIdString());
		q.setParameter("idType", pid.getType());
		List<Patient> result = q.getResultList();
		if (result.size() > 1) {
			logger.fatal("Found more than one patient with ID: " + pid.toString());
			throw new InternalErrorException("Found more than one patient with ID: " + pid.toString());
		}
		Patient p = result.get(0);
		// Fetch lazy loaded IDs
		p.getIds();
		em.close();
		if (result.size() == 0)
			return null;
		else
			return result.get(0);
	}
	
	/**
	 * Returns all patients currently persisted in the patient list. This is not a copy!
	 * Caller MUST NOT perform write operations on the return value or its linked objects.
	 * 
	 * @return All persisted patients.
	 */
	public synchronized List<Patient> getPatients() { //TODO: Filtern
		// Entities are not detached, because the IDs are lazy-loaded
		List<Patient> pl;
		pl = this.em.createQuery("select p from Patient p", Patient.class).getResultList();
		return pl;
	}

	/**
	 * Returns a detached list of the IDs of all patients.
	 * @return A list where every item represents the IDs of one patient.
	 */
	public synchronized List<Set<ID>> getAllIds() {
		List<Patient> patients = this.getPatients();
		List<Set<ID>> ret = new LinkedList<Set<ID>>();
		for (Patient p : patients) {
			Set<ID> thisPatientIds = p.getIds();
			this.em.detach(thisPatientIds);
			ret.add(thisPatientIds);
		}
		return ret;
	}

	/**
	 * Add an ID request to the database. In cases where a new ID is created, a
	 * new Patient object is persisted.
	 */
	public synchronized void addIdRequest(IDRequest req) {
		em.getTransaction().begin();
		em.persist(req); //TODO: Fehlerbehandlung, falls PID schon existiert.		
		em.getTransaction().commit();
	}
	
	/**
	 * Update the persisted properties of an ID generator (e.g. the counter 
	 * from which PIDs are generated).
	 */
	public synchronized void updateIDGeneratorMemory(IDGeneratorMemory mem) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(mem);
		em.getTransaction().commit();
		em.close();
	}
	
	/**
	 * Mark the patient with ID idOfDuplicate as a duplicate of idOfOriginal.
	 * @see de.pseudonymisierung.mainzelliste.Patient#isDuplicate()
	 * @see de.pseudonymisierung.mainzelliste.Patient#getOriginal()
	 * @see de.pseudonymisierung.mainzelliste.Patient#setOriginal(Patient)
	 */
	public synchronized void markAsDuplicate(ID idOfDuplicate, ID idOfOriginal) {
		Patient pDuplicate = getPatient(idOfDuplicate);
		Patient pOriginal = getPatient(idOfOriginal);
		pDuplicate.setOriginal(pOriginal);
		updatePatient(pDuplicate);
	}
	
	/**
	 * Load the persisted properties for an ID generator.
	 * @param idString Identifier of the ID generator.
	 */
	public IDGeneratorMemory getIDGeneratorMemory(String idString) {
		EntityManager em = emf.createEntityManager();
		TypedQuery<IDGeneratorMemory> q = em.createQuery("SELECT m FROM IDGeneratorMemory m WHERE m.idString = :idString", IDGeneratorMemory.class);
		q.setParameter("idString", idString);
		List<IDGeneratorMemory> result = q.getResultList();
		em.close();
		if (result.size() == 0)
			return null;
		else
			return result.get(0);
	}
	
	/**
	 * Persist changes made to a patient.
	 */
	public synchronized void updatePatient(Patient p) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(p);
		em.getTransaction().commit();
		em.close();
	}
}
