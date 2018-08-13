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
package de.pseudonymisierung.mainzelliste.dto;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.persistence.*;

import de.pseudonymisierung.mainzelliste.*;
import de.pseudonymisierung.mainzelliste.exceptions.IllegalUsedCharacterException;
import org.apache.log4j.Logger;


import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;

/**
 * Handles reading and writing from and to the database. Implemented as a
 * singleton object, which can be referenced by Persistor.instance.
 */
public enum Persistor {
	
	/** The singleton instance. */
	instance;
	
	/** Factory for EntityManager. */
	private EntityManagerFactory emf;
	/** EntityManager. Instance that stays open (for cases where entities cannot be detached). */
	private EntityManager em;
	
	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());
	
	/** String with which database identifers are quoted. */
	private String identifierQuoteString = null;
	
	/** Number of retries for initializing database connection. */
	private int dbconnect_retry_count = 100;

	/** Number of milliseconds to wait between retries for initializing database connection. */
	private int dbconnect_retry_wait = 100;

	/** Creates the singleton instance with the configured database connection. */
	private Persistor() {
		
		this.initPropertiesTable();
		
		HashMap<String, String> persistenceOptions = new HashMap<String, String>();
		
		// Settings from config
		persistenceOptions.put("javax.persistence.jdbc.driver", Config.instance.getProperty("db.driver"));
		persistenceOptions.put("javax.persistence.jdbc.url", Config.instance.getProperty("db.url"));
		if (Config.instance.getProperty("db.username") != null)
			persistenceOptions.put("javax.persistence.jdbc.user", Config.instance.getProperty("db.username"));
		if (Config.instance.getProperty("db.password") != null)
			persistenceOptions.put("javax.persistence.jdbc.password", Config.instance.getProperty("db.password"));
		
		// Other settings
		persistenceOptions.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
		persistenceOptions.put("openjpa.jdbc.DriverDataSource", "dbcp");
		
		// For Apache Derby (used in automated tests) an alternate validation query is necessary
		String validationQuery;
		if (Config.instance.getProperty("db.driver").equals("org.apache.derby.jdbc.EmbeddedDriver"))
			validationQuery = "VALUES 1";
		else
			validationQuery = "SELECT 1";
		persistenceOptions.put("openjpa.ConnectionProperties", "testOnBorrow=true, validationQuery=" + validationQuery);
		
		emf = Persistence.createEntityManagerFactory("mainzelliste", persistenceOptions);
		em = emf.createEntityManager();
		
		new org.apache.openjpa.jdbc.schema.DBCPDriverDataSource();
		
		// update database schema (post-JPA)
		String dbVersion = this.getSchemaVersion();
		this.updateDatabaseSchemaJPA(dbVersion);
		
		// Check database connection
		getPatients();
		
		Logger.getLogger(Persistor.class).info("Persistence has initialized successfully.");
	}

	/**
	 * Shut down instance. This method is called upon undeployment and releases
	 * resources, such as stopping background threads or removing objects that
	 * would otherwise persist and cause a memory leak. Called by
	 * {@link de.pseudonymisierung.mainzelliste.webservice.ContextShutdownHook}.
	 */
	public void shutdown() {
		ClassLoader contextClassLoader = Initializer.getServletContext().getClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			Class<?> driverClass = driver.getClass();

			if (checkClassLoader(driver, contextClassLoader)) {
				if (driverClass.getName().equals("com.mysql.jdbc.Driver")) {
					// special mysql handling
					handleMySQLShutdown();
				}
				try {
					logger.info("Deregistering JDBC driver " + driver);
					DriverManager.deregisterDriver(driver);
				} catch (SQLException ex) {
					logger.debug("An error occured during deregistering JDBC driver " + driver, ex);
				}
			}
		}
	}

	/**
	 * Check if the driver was loaded by the web app classloader or by one of its children.
	 *
	 * @param driver The jdbc driver to test
	 * @param contextClassLoader The web applications classloader
	 * @return Returns true if the driver was loaded by the web app classloader or by one of its children
	 */
	private boolean checkClassLoader(Driver driver, ClassLoader contextClassLoader) {
		ClassLoader cl = driver.getClass().getClassLoader();
		while (cl != null) {
			if (cl == contextClassLoader)
				return true; // the driver was loaded by the context class loader or by one of its successor
			cl = cl.getParent();
		}
		return false;
	}

	/**
	 * Special handling when shutting down the mysql jdbc driver. The mysql driver starts a thread that will not exit
	 * automatically when the driver gets unloaded. Stop that thread explicitly. Using reflections will not cause any
	 * errors when mysql is not used and the driver is not within the classpath.
	 */
	private void handleMySQLShutdown() {
		try {
			Class<?> threadClass = Class.forName("com.mysql.jdbc.AbandonedConnectionCleanupThread");
			logger.info("Calling MySQL AbandonedConnectionCleanupThread shutdown");
			Method shutdownMethod = threadClass.getMethod("shutdown");
			shutdownMethod.invoke(null);
		} catch (ClassNotFoundException ex) {
		} catch (NoSuchMethodException ex) {
		} catch (SecurityException ex) {
		} catch (IllegalAccessException ex) {
		} catch (IllegalArgumentException ex) {
		} catch (InvocationTargetException ex) {
		}
	}

	/**
	 * Get a patient by one of its IDs.
	 * 
	 * @param pid
	 *            An identifier of the patient to get.
	 * @return The patient with the given ID or null if no patient with the
	 *         given ID exists.
	 * 
	 */
	public Patient getPatient(ID pid){
		EntityManager em = emf.createEntityManager();
		TypedQuery<Patient> q = em.createQuery("SELECT p FROM Patient p JOIN p.ids id WHERE id.idString = :idString AND id.type = :idType", Patient.class);
		q.setParameter("idString", pid.getIdString());
		q.setParameter("idType", pid.getType());
		List<Patient> result = q.getResultList();
		if (result.size() > 1) {
			em.close();
			logger.fatal("Found more than one patient with ID: " + pid.toString());
			throw new InternalErrorException("Found more than one patient with ID: " + pid.toString());
		} 
		
		if (result.size() == 0) {
			em.close();
			return null;
		}

		Patient p = result.get(0);
		// Fetch lazy loaded IDs
		p.getIds();
		p.getOriginal().getIds();
		//em.refresh(p.getOriginal());
		em.close();
		return p;
	}
	
	/**
	 * Returns all patients currently persisted in the patient list. This is not
	 * a copy! Caller MUST NOT perform write operations on the return value or
	 * its linked objects.
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
	 * Check whether a patient with the given ID exists.
	 * 
	 * @param idType
	 *            The ID type.
	 * @param idString
	 *            The ID string.
	 * @return The patient with the given ID or null if no patient with the
	 *         given ID exists.
	 */
	public boolean patientExists(String idType, String idString) {
		EntityManager em = emf.createEntityManager();
		TypedQuery<Long> q = em.createQuery("SELECT COUNT(p) FROM Patient p JOIN p.ids id WHERE id.idString = :idString AND id.type = :idType", Long.class);
		q.setParameter("idString", idString);
		q.setParameter("idType", idType);
		Long count = q.getSingleResult();
		if (count > 0)
			return true;
		else 
			return false;
	}
	
	/**
	 * Check whether a patient with the given ID exists.
	 * 
	 * @param id
	 *            The ID to check.
	 * @return true if a patient with the given ID exists.
	 */
	public boolean patientExists(ID id) {
		return this.patientExists(id.getType(), id.getIdString());
	}

	/**
	 * Returns a detached list of the IDs of all patients.
	 * 
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
	 * 
	 * @param req
	 *            The ID request to persist.
	 */
	public synchronized void addIdRequest(IDRequest req) {
		em.getTransaction().begin();
		em.persist(req); //TODO: Fehlerbehandlung, falls PID schon existiert.		
		em.getTransaction().commit();
	}
	
	/**
	 * Update the persisted properties of an ID generator (e.g. the counter from
	 * which PIDs are generated).
	 * 
	 * @param mem
	 *            The properties to persist.
	 */
	public synchronized void updateIDGeneratorMemory(IDGeneratorMemory mem) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(mem);
		em.getTransaction().commit();
		em.close();
	}
	
	/**
	 * Mark a patient as duplicate of another.
	 * 
	 * @param idOfDuplicate
	 *            ID of the patient to be marked as duplicate.
	 * @param idOfOriginal
	 *            ID of the patient of which the other one is a duplicate.
	 *            
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
	 * 
	 * @param idType
	 *            Identifier of the ID generator.
	 * @return The persisted properties or null if no properties have been
	 *         persisted for the given ID generator.
	 */
	public IDGeneratorMemory getIDGeneratorMemory(String idType) {
		EntityManager em = emf.createEntityManager();
		TypedQuery<IDGeneratorMemory> q = em.createQuery("SELECT m FROM IDGeneratorMemory m WHERE m.idType = :idType", IDGeneratorMemory.class);
		q.setParameter("idType", idType);
		try {
			IDGeneratorMemory result = q.getSingleResult();
			em.close();
			return result;
		} catch (NoResultException e) { // No result -> No IDGeneratorMemory object persisted yet.
			em.close();
			return null;
		}
	}
	
	/**
	 * Persist changes made to a patient.
	 * 
	 * @param p
	 *            The patient to persist.
	 */
	public synchronized void updatePatient(Patient p) {
		em.getTransaction().begin();
		Patient edited = em.merge(p);
		em.getTransaction().commit();
		// Refreshes cached entity 
		em.refresh(edited); 
	}


	public synchronized  void deletePatient(ID id){
        Set<ID> allPatientIDs = getAllPatientIDs(id);

	    anonymizeIdRequests(id);
        deletePatientIDAT(id);

        for (ID specificPatientID : allPatientIDs){
            deleteId(specificPatientID);
        }

    }

    /**
     * Remove a patient from the database, including duplicates.
     * In addition, all patients that are either a duplicate of the given patient
     * or those of which the patient is a duplicate are deleted. This includes transitive
     * relations (duplicate of duplicate).
     *
     * @param id An ID of the patient to delete.
     */
    public synchronized void deletePatientWithDuplicates(ID id) {
        /* The subgraph of duplicates is a tree whose root can be found by following
         * the "original" link recursively. From there, determine all connected patients
         * by breadth-first search.
         */
        List<Patient> allInstances = getPatientWithDuplicates(id);
        if (allInstances == null)
            return;

        for (int i = 0; i < allInstances.size(); i++) {
            deletePatient(allInstances.get(i).getId(id.getType()));

        }

    }

	/**
	 * Remove a patient from the database.
	 * 
	 * @param id An ID of the patient to persist.
	 */
	public synchronized void deletePatientIDAT(ID id) {
        checkForSuspectSQLCharacters(id.getIdString());

		em.getTransaction().begin();
		TypedQuery<Patient> q = em.createQuery("SELECT p FROM Patient p JOIN p.ids id WHERE id.idString = :idString AND id.type = :idType", Patient.class);
		q.setParameter("idString", id.getIdString());
		q.setParameter("idType", id.getType());
		Patient p = q.getSingleResult();
		if (p != null)
			em.remove(p);
		em.getTransaction().commit();
	}

	public synchronized void deleteId (ID id) {
        checkForSuspectSQLCharacters(id.getIdString());

	    em.getTransaction().begin();
	    Query query = em.createQuery("DELETE FROM ID id WHERE id.idString like :id").setParameter("id", id.getIdString());
	    query.executeUpdate();
	    em.getTransaction().commit();
	}

    private synchronized void anonymizeIdRequests(ID id){
        em.getTransaction().begin();
        try {
            Patient patient = getPatient(id);
            // Anonymize fields of all IDRequests that yielded this patient as result
            TypedQuery<IDRequest> qIdRequest = em.createQuery("SELECT r FROM IDRequest r WHERE r.assignedPatient = :p", IDRequest.class);
            qIdRequest.setParameter("p", patient);
            for (IDRequest idRequest : qIdRequest.getResultList()) {
                for (Field<?> f : idRequest.getInputFields().values()) {
                    if (f instanceof PlainTextField)
                        f.setValue("ANONYMIZED");
                    else if (f instanceof IntegerField)
                        f.setValue("0");
                    else
                        f.setValue("");
                }
            }
            // Finally, remove the patient record
            //em.remove(patient);
            em.getTransaction().commit();
        } catch (Throwable t) {
            logger.error("Error while deleting patients", t);
            em.getTransaction().rollback();
            throw new InternalErrorException(t);
        }

    }

    private synchronized Set<ID> getAllPatientIDs(ID id){

        Patient patient = getPatient(id);
        return patient.getIds();

    }

	/** Get patient with duplicates. Works like
	 * {@link Persistor#getDuplicates(ID)}, but the requested patient is
	 * included in the result.
	 * 
	 * @param id
	 *            An ID of the patient to get.
	 * @return A list containing the requested patient and its duplicates.
	 * @throws InvalidIDException
	 *             If no patient with the given ID exists. */
	public synchronized List<Patient> getPatientWithDuplicates(ID id) throws InvalidIDException {
		List<Patient> duplicates = getDuplicates(id);
		Patient p = getPatient(id);
		duplicates.add(p);
		return duplicates;
	}

	/** Get duplicates of a patient.
	 * 
	 * Returns a list of all patients that are marked as duplicates of the given
	 * patient or of which the given patient is a duplicate. This includes
	 * transitive relations (duplicate of duplicate), but not the patient which
	 * is queried.
	 * 
	 * @param id
	 *            An ID of the patient for which to get duplicates.
	 * @return A list containing the duplicates of the requested patients (empty
	 *         if none exist).
	 * @throws InvalidIDException
	 *             If no patient with the given ID exists. */
	public synchronized List<Patient> getDuplicates(ID id) throws InvalidIDException {
		Patient p = getPatient(id);
		if (p == null)
			throw new InvalidIDException("No patient found with ID " + id.getIdString() + " of type " + id.getType());
		Patient root = p.getOriginal();
		LinkedList<Patient> allInstances = new LinkedList<Patient>();
		LinkedList<Patient> queue = new LinkedList<Patient>();
		queue.add(root);
		TypedQuery<Patient> duplicateQuery = em
				.createQuery("SELECT p FROM Patient p JOIN p.original o WHERE o=:original", Patient.class);
		while (!queue.isEmpty()) {
			Patient thisPatient = queue.remove();
			if (!thisPatient.equals(p)) {
				allInstances.add(thisPatient);
			}
			duplicateQuery.setParameter("original", thisPatient);
			queue.addAll(duplicateQuery.getResultList());			
		}
		
		return allInstances;
	}
	
	/**
	 * Get possible duplicates of a patient. 
	 * @param id ID of the patient for which to find possible duplicates.
	 * @return The list of possible duplicates.
	 */
	public List<Patient> getPossibleDuplicates(ID id) {
		Patient p = getPatient(id);
		if (p == null)
			return new LinkedList<Patient>();
		TypedQuery<Patient> q = em.createQuery("SELECT pa FROM IDRequest r JOIN r.assignedPatient pa JOIN r.matchResult m JOIN m.bestMatchedPatient pb "
				+ "WHERE m.type=:matchResultType AND pa.isTentative=true AND pb=:thisPatient", Patient.class);
		q.setParameter("matchResultType", MatchResultType.POSSIBLE_MATCH);
		q.setParameter("thisPatient", p);
		LinkedList<Patient> result = new LinkedList<Patient>(q.getResultList());
		if (p.isTentative()) {
			q = em.createQuery("SELECT pb FROM IDRequest r JOIN r.assignedPatient pa JOIN r.matchResult m JOIN m.bestMatchedPatient pb "
					+ "WHERE m.type=:matchResultType AND pa.isTentative=true AND pa=:thisPatient", Patient.class);
			q.setParameter("matchResultType", MatchResultType.POSSIBLE_MATCH);
			q.setParameter("thisPatient", p);
			result.addAll(q.getResultList());
		}
		return result;
	}
	
	/**
	 * Performs database updates after JPA initialization.
	 * @param fromVersion The version from which to update.
	 */
	private void updateDatabaseSchemaJPA(String fromVersion)
	{
		EntityManager em = emf.createEntityManager();

		if ("1.0".equals(fromVersion)) { // 1.0 -> 1.1
			em.getTransaction().begin();
			em.createNativeQuery("UPDATE IDGeneratorMemory SET idType=idstring").executeUpdate();
			em.createNativeQuery("ALTER TABLE IDGeneratorMemory DROP COLUMN idString").executeUpdate();

			/*
			 * Delete invalid instances of IDGeneratorMemory (caused by Bug #3007
			 * Both id generators of version 1.0 use a field "counter". The memory object
			 * with the highest value has to be retained.
			 */
			List<String> idTypes = em.createQuery("SELECT DISTINCT m.idType FROM IDGeneratorMemory m", String.class).getResultList();
			for (String idType: idTypes) {
				if (idType != null) {
					TreeMap<Integer, IDGeneratorMemory> genMap = new TreeMap<Integer, IDGeneratorMemory>();
					List<IDGeneratorMemory> generators = em.createQuery("SELECT im FROM IDGeneratorMemory im WHERE im.idType = :idType", IDGeneratorMemory.class)
							.setParameter("idType", idType).getResultList();
					for (IDGeneratorMemory thisGen : generators) {
						genMap.put(Integer.parseInt(thisGen.get("counter")), thisGen);
					}
					// Remove the object with the highest counter. This should be kept. 
					genMap.pollLastEntry();
					// Remove the others.
					for (IDGeneratorMemory thisGen : genMap.values()) {
						em.remove(thisGen);
					}
				}
			}
			this.setSchemaVersion("1.1", em);
			fromVersion = "1.1";
			em.getTransaction().commit();
		} // End of update 1.0 -> 1.1
		if ("1.1".equals(fromVersion)) { // 1.1 -> 1.3.1
			em.getTransaction().begin();
			// Add index on idString for more efficient access to patients by ID
			em.createNativeQuery("CREATE INDEX i_id_idstring ON ID (idString)").executeUpdate();

			// Update schema version. Corresponds to Mainzelliste version, therefore the gap
			this.setSchemaVersion("1.3.1", em);
			fromVersion = "1.3.1";
			
			em.getTransaction().commit();
		} // End of update 1.1 -> 1.3.1
		
		// Update schema version to release version, even if no changes are necessary
		em.getTransaction().begin();
		this.setSchemaVersion(Config.instance.getVersion(), em);
		em.getTransaction().commit();
		em.close();
	}
	
	/**
	 * Reads the release version from the database (1.0 is assumed if
	 * this information cannot be found).
	 * 
	 * This function does not make use of JPA in order to be compatible
	 * with updates that have to be made before JPA initialization 
	 * (e.g. if the Object-DB mapping would be broken without the update).
	 * 
	 * Run initPropertiesTable() first to ensure that version information exists.
	 * 
	 * @return The persisted release version.
	 */
	private String getSchemaVersion() {
		Connection conn = getJdbcConnection();
		try {
			// Check if there is a properties table 
			ResultSet rs = conn.createStatement().executeQuery("SELECT " + quoteIdentifier("value") + " FROM mainzelliste_properties " +
					"WHERE property='version'");
			if (!rs.next()) {
				logger.fatal("Properties table not initialized correctly!");
				throw new Error("Properties table not initialized correctly!");	
			}				
			return rs.getString("value");
		} catch (SQLException e) {
			logger.fatal("Could not update database schema!", e);
			throw new Error(e);			
		}			
	}
	
	/**
	 * Update version information in the database. Should be run in one
	 * transaction on the provided EntityManager together with the changes made
	 * for this version so that no inconsistencies arise if any of the update
	 * statements fail.
	 * 
	 * @param toVersion
	 *            The version string to set.
	 * @param em
	 *            A valid EntityManager object.
	 */
	private void setSchemaVersion(String toVersion, EntityManager em) {
		em.createNativeQuery("UPDATE mainzelliste_properties SET " + quoteIdentifier("value") + "='" + toVersion + 
				"' WHERE property='version'").executeUpdate(); 
	}
	
	/**
	 * Create mainzelliste_properties if not exists. Check if JPA schema was
	 * initialized. If no, set version to current, otherwise, it is assumed that
	 * the database schema was created by version 1.0 (where the properties
	 * table did not exist) and this version is set.
	 * 
	 * Must be called before JPA initialization, i.e. before an EntityManager is
	 * created.
	 */
	private void initPropertiesTable() {
		Connection conn = getJdbcConnection();
		try {
			// Check if there is a properties table 
			DatabaseMetaData metaData = conn.getMetaData();
			// Look for patients table to determine if schema is yet to be created
			String tableName;
			if (metaData.storesLowerCaseIdentifiers())
				tableName = "patient";
			else
				tableName = "Patient";
			ResultSet rs = metaData.getTables(null, null, tableName, null);
			boolean firstRun = !rs.next(); // First invocation with this database 
			
			// Check if there is a properties table 
			rs = metaData.getTables(null, null, "mainzelliste_properties", null);
			// Assume version 1.0 if none is provided
			if (!rs.next()) {
				// Create table				
				conn.createStatement().execute("CREATE TABLE mainzelliste_properties" +
						"(property varchar(256), " + quoteIdentifier("value") +" varchar(256))");
			} 
			rs = conn.createStatement().executeQuery("SELECT " + quoteIdentifier("value") + 
					" FROM mainzelliste_properties WHERE property='version'");
			if (!rs.next()) {
				// Properties table exists, but no version information
				String setVersion = firstRun ? Config.instance.getVersion() : "1.0";
				conn.createStatement().execute("INSERT INTO mainzelliste_properties" +
						"(property, " + quoteIdentifier("value") + ") VALUES ('version', '" + setVersion + "')");
			}
		} catch (SQLException e) {
			logger.fatal("Could not update database schema!", e);
			throw new Error(e);			
		}			
	}
	
	/**
	 * Get JDBC connection to database. Fails with an Error if the driver class cannot be found or an error occurs while
	 * connecting.
	 * 
	 * @return The JDBC connection.
	 */
	private Connection getJdbcConnection() {
		Properties connectionProps = new Properties();
		if (Config.instance.getProperty("db.username") != null) connectionProps.put("user",  Config.instance.getProperty("db.username"));
		if (Config.instance.getProperty("db.password") != null) connectionProps.put("password",  Config.instance.getProperty("db.password"));
		String url = Config.instance.getProperty("db.url");
		for(int count=0; true; count++) {
			try {
				Class.forName(Config.instance.getProperty("db.driver"));
				return DriverManager.getConnection(url, connectionProps);
			} catch (ClassNotFoundException e) {
				logger.fatal("Could not find database driver!", e);
				throw new Error(e);
			} catch (SQLException e) {
				if (count < dbconnect_retry_count) {
					logger.warn("SQL error while getting database connection; retrying.");
				} else {
					logger.fatal("SQL error while getting database connection; giving up.", e);
					throw new Error(e);
				}
			}
			try{
				Thread.sleep(dbconnect_retry_wait);
			} catch (InterruptedException e){
				continue;
			}
		}
	}
	
	/**
	 * Quote an identifier (e.g. table name) for use in an SQL query. Selects the appropriate quotation character.
	 * 
	 * @param identifier
	 *            The identifier to quote.
	 * @return The quoted identifier.
	 */
	private String quoteIdentifier(String identifier) {
		// Retrieve from database only once to reduce connections
		if (identifierQuoteString == null) {
			try {
				Connection conn = getJdbcConnection();
				DatabaseMetaData metaData = conn.getMetaData();
				identifierQuoteString = metaData.getIdentifierQuoteString();
				conn.close();
			} catch (SQLException e) {
				logger.fatal("Could not get quote string for database identifiers!", e);
				throw new Error(e);
			}
		}
		return identifierQuoteString + identifier + identifierQuoteString;
	}

    /**
     * Utility function to prevent illegal use of SQL features
     * @param checkValue
     */

	private void checkForSuspectSQLCharacters(String checkValue){
	    if (checkValue.contains("%") || checkValue.contains(";")|| checkValue.contains("--")){
	        logger.error("Found illegal character in " + checkValue);
	        throw new IllegalUsedCharacterException();

        }
    }
}