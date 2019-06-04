package de.pseudonymisierung.mainzelliste;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import de.pseudonymisierung.mainzelliste.Servers.ApiVersion;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.webservice.AddPatientToken;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import java.util.Date;


/**
 * Backend methods for handling patients. Implemented as a singleton object,
 * which can be referenced by PatientBackend.instance.
 */
public enum PatientBackend {

	/** The singleton instance. */
	instance;

	/** The logging instance */
	private Logger logger = Logger.getLogger(this.getClass());
        
	/** The TLS context depending on the configuration parameters */
	private SSLConnectionSocketFactory sslsf;
        
	/**
	 * Creates an instance. Invoked on first access to {@link PatientBackend#instance}.
	 */
	private PatientBackend() {
		try {

			SSLContextBuilder builder = new SSLContextBuilder();
			SSLContext sslCtx;

			if ("true".equals(Config.instance.getProperty("callback.allowSelfsigned"))) {
				builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
				sslCtx = builder.build();

			} else {
				sslCtx = SSLContexts.createSystemDefault();
			}

			sslsf = new SSLConnectionSocketFactory(sslCtx, new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" }, null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier());

		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
		} catch (KeyStoreException ex) {
			Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
		} catch (KeyManagementException ex) {
			Logger.getLogger(PatientBackend.class).error("Error initializing client Transport Layer Security", ex);
		}
	}

	/**
	 * Session to be used when in debug mode.
	 */
	private Session debugSession = null;

	/**
	 * PID request. Looks for a patient with the specified data in the database.
	 * If a match is found, the ID of the matching patient is returned. If no
	 * match or possible match is found, a new patient with the specified data
	 * is created. If a possible match is found and the form has an entry
	 * "sureness" whose value can be parsed to true (by Boolean.parseBoolean()),
	 * a new patient is created. Otherwise, return null.
	 * 
	 * @param tokenId
	 *            ID of a valid "addPatient" token.
	 * @param form
	 *            Input fields from the HTTP request.
	 * @param apiVersion
	 *            The API version to use.
	 * @return A representation of the request and its result as an instance of
	 *         {@link IDRequest}.
	 */
	public IDRequest createNewPatient(
			String tokenId,
			MultivaluedMap<String, String> form,
			ApiVersion apiVersion) {

		HashMap<String, Object> ret = new HashMap<String, Object>();
		// create a token if started in debug mode
		AddPatientToken t;

		Token tt = Servers.instance.getTokenByTid(tokenId);
		// Try reading token from session.
		if (tt == null) {
			// If no token found and debug mode is on, create token, otherwise fail
			if (Config.instance.debugIsOn())
			{
				Session s = getDebugSession();
				try {
					s.setURI(new URI("debug"));
				} catch (URISyntaxException e) {
					throw new Error();
				}

				t = new AddPatientToken(null);
				Servers.instance.registerToken(s.getId(), t, "127.0.0.1");
				tokenId = t.getId();
			} else {
				logger.error("No token with id " + tokenId + " found");
				throw new InvalidTokenException("Please supply a valid 'addPatient' token.", Status.UNAUTHORIZED);
			}
		} else { // correct token type?
			if (!(tt instanceof AddPatientToken)) {
				logger.error("Token " + tt.getId() + " is not of type 'addPatient' but '" + tt.getType() + "'");
				throw new InvalidTokenException("Please supply a valid 'addPatient' token.", Status.UNAUTHORIZED);
			} else {
				t = (AddPatientToken) tt;
			}
		}

		List<ID> returnIds = new LinkedList<ID>();
		MatchResult match;
		IDRequest request;

		// synchronize on token
		synchronized (t) {
			/* Get token again and check if it still exist.
			 * This prevents the following race condition:
			 *  1. Thread A gets token t and enters synchronized block
			 *  2. Thread B also gets token t, now waits for A to exit the synchronized block
			 *  3. Thread A deletes t and exits synchronized block
			 *  4. Thread B enters synchronized block with invalid token
			 */
			
			t = (AddPatientToken) Servers.instance.getTokenByTid(tokenId);

			if(t == null){
				String infoLog = "Token with ID " + tokenId + " is invalid. It was invalidated by a concurrent request or the session timed out during this request.";
				logger.info(infoLog);
				throw new WebApplicationException(Response
					.status(Status.UNAUTHORIZED)
					.entity("Please supply a valid 'addPatient' token.")
					.build());
			}
			logger.info("Handling ID Request with token " + t.getId());
			Patient p = new Patient();
			Map<String, Field<?>> chars = new HashMap<String, Field<?>>();
			
			// get fields transmitted from MDAT server
			for (String key : t.getFields().keySet())
			{
				form.add(key, t.getFields().get(key));
			}
			
			// get externally generated ids transmitted from MDAT server
			for (String key : t.getIds().keySet())
			{
				form.add(key, t.getIds().get(key));
			}

			Validator.instance.validateForm(form, true);
			
			for(String s: Config.instance.getFieldKeys()){
				try {
				    chars.put(s, Field.build(s, form.getFirst(s)));
				} catch (WebApplicationException we) {
				    logger.error(String.format("Error while building field %s with input %s", s, form.getFirst(s)));
				    throw we;
				}
			}

			p.setFields(chars);
			
			// Normalization, Transformation
			Patient pNormalized = Config.instance.getRecordTransformer().transform(p);
			pNormalized.setInputFields(chars);
			
			match = Config.instance.getMatcher().match(pNormalized, Persistor.instance.getPatients());
			logger.debug("Best matching weight: " + match.getBestMatchedWeight());
			Patient assignedPatient; // The "real" patient that is assigned (match result or new patient) 
			
			// If a list of ID types is given in token, return these types
			Set<String> idTypes;
			idTypes = t.getRequestedIdTypes();
			if (idTypes.isEmpty()) { // otherwise use the default ID type
				idTypes = new CopyOnWriteArraySet<String>();
				idTypes.add(IDGeneratorFactory.instance.getDefaultIDType());
			}

			switch (match.getResultType())
			{
			case MATCH :
				for (String idType : idTypes)
					returnIds.add(match.getBestMatchedPatient().getOriginal().getId(idType));
				
				assignedPatient = match.getBestMatchedPatient();
				// log token to separate concurrent request in the log file
				logger.info("Found match with ID " + returnIds.get(0).getIdString() + " for ID request " + t.getId()); 
				break;
				
			case NON_MATCH :
			case POSSIBLE_MATCH :
				if (match.getResultType() == MatchResultType.POSSIBLE_MATCH 
				&& (form.getFirst("sureness") == null || !Boolean.parseBoolean(form.getFirst("sureness")))) {
					return new IDRequest(p.getFields(), idTypes, match, null);
				}

				// Generate internal IDs
				Set<ID> newIds = IDGeneratorFactory.instance.generateIds();			

				// Import external IDs
				for (String extIDType : IDGeneratorFactory.instance.getExternalIdTypes()) {
					String extIDString = form.getFirst(extIDType);
					if (extIDString != null) {
						ID extId = IDGeneratorFactory.instance.buildId(extIDType, extIDString);
						if (Persistor.instance.getPatient(extId) != null) {
								logger.info("Request to add patient with existing external ID " + extId.toString());
								throw new WebApplicationException(
										Response.status(Status.CONFLICT)
												.entity("Cannot create a new patient with the supplied external ID, " +
														"because it is already in use. " +
														"Please check external ID and repeat the request.")
												.build());

						}
						newIds.add(extId);
					}
				}
				pNormalized.setIds(newIds);

				for (String idType : idTypes) {
					ID thisID = pNormalized.getId(idType);
					returnIds.add(thisID);				
					logger.info("Created new ID " + thisID.getIdString() + " for ID request " + (t == null ? "(null)" : t.getId()));
				}
				if (match.getResultType() == MatchResultType.POSSIBLE_MATCH)
				{
					pNormalized.setTentative(true);
					for (ID thisId : returnIds)
						thisId.setTentative(true);
					logger.info("New ID " + returnIds.get(0).getIdString() + " is tentative. Found possible match with ID " + 
							match.getBestMatchedPatient().getId(IDGeneratorFactory.instance.getDefaultIDType()).getIdString());
				}
				assignedPatient = pNormalized;
				break;
		
			default :
				logger.error("Illegal match result: " + match.getResultType());
				throw new InternalErrorException();
			}

			logger.info("Weight of best match: " + match.getBestMatchedWeight());
			
			request = new IDRequest(p.getFields(), idTypes, match, assignedPatient);
			
			ret.put("request", request);

			Persistor.instance.addIdRequest(request);

			if (Config.instance.auditTrailIsOn()) {
				for (ID id : assignedPatient.getIds()) {
					AuditTrail at = buildAuditTrailRecord(tokenId,
							id.getIdString(),
							id.getType(),
							"create",
							null,
							request.getAssignedPatient().toString()
					);
					Persistor.instance.createAuditTrail(at);
				}
			}



			if(t != null && ! Config.instance.debugIsOn())
				Servers.instance.deleteToken(t.getId());
		}
		// Callback request
		String callback = t.getDataItemString("callback");
		if (callback != null && callback.length() > 0)
		{
			try {
				logger.debug("Sending request to callback " + callback);

				JSONObject reqBody = new JSONObject();

				if (apiVersion.majorVersion >= 2) {
					// Collect ids for Callback object
					JSONArray idsJson = new JSONArray(); 
					
					for (ID thisID : returnIds) {
						idsJson.put(thisID.toJSON());
					}
					
					reqBody.put("tokenId", t.getId())
					.put("ids", idsJson);

				} else {  // API version 1.0 
					if (returnIds.size() > 1) {
						throw new WebApplicationException(
								Response.status(Status.BAD_REQUEST)
								.entity("Selected API version 1.0 permits only one ID in callback, " +
										"but several were requested. Set mainzellisteApiVersion to a " +
										"value >= 2.0 or request only one ID type in token.")
										.build());
					}					
					reqBody.put("tokenId", t.getId())
						.put("id", returnIds.get(0).getIdString());
				}
                                
				HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();                                
				HttpPost callbackReq = new HttpPost(callback);
				callbackReq.setHeader("Content-Type", MediaType.APPLICATION_JSON);
				callbackReq.setHeader("User-Agent", Config.instance.getUserAgentString());
				StringEntity reqEntity = new StringEntity(reqBody.toString());
				reqEntity.setContentType("application/json");
				callbackReq.setEntity(reqEntity);
				HttpResponse response = httpClient.execute(callbackReq);
				StatusLine sline = response.getStatusLine();
				// Accept callback if OK, CREATED or ACCEPTED is returned
				if ((sline.getStatusCode() < 200) || sline.getStatusCode() >= 300) {
					logger.error("Received invalid status form mdat callback: " + response.getStatusLine());
					throw new InternalErrorException("Request to callback failed!");
				}
				// TODO: Server-Antwort auslesen
                
			} catch (JSONException e) {
				logger.error("Internal serializitaion error: ", e);
				throw new InternalErrorException("Internal serializitaion error!");
			} catch (IOException e) {
				logger.error("Internal error building the request: ", e);
				throw new InternalErrorException("Internal error building the request!");
			}
		}
		return request;
	}

	/**
	 * Set fields of a patient to new values.
	 * 
	 * @param patientId
	 *            ID of the patient to edit.
	 * @param tokenId
	 *            ID of a valid "editPatient" token.
	 * @param newFieldValues
	 *            Field values to set. Fields that do not appear as map keys are
	 *            left as they are. In order to delete a field value, provide an
	 *            empty string. All values are processed by field transformation
	 *            as defined in the configuration file.
	 */
	public void editPatient(ID patientId, Map<String, String> newFieldValues, String tokenId) {
		// Check that provided ID is valid
		if (patientId == null) {
			// Calling methods should provide a legal id, therefore log an error if id is null 
			logger.error("editPatient called with null id.");
			throw new InternalErrorException("An internal error occured: editPatients called with null. Please contact the administrator.");
		}
		Patient pToEdit = Persistor.instance.getPatient(patientId);
		if (pToEdit == null)
		{
			logger.info("Request to edit patient with unknown ID " + patientId.toString());
			throw new InvalidIDException("No patient found with ID " + patientId.toString());
		}

		// validate input
		Validator.instance.validateForm(newFieldValues, false);
		// read input fields from form
		Patient pInput = new Patient();
		Map<String, Field<?>> chars = new HashMap<String, Field<?>>();

		for (String fieldName : Config.instance.getFieldKeys()) {
			// If a field is not in the map, keep the old value
			if (!newFieldValues.containsKey(fieldName))
				chars.put(fieldName, pToEdit.getInputFields().get(fieldName));
			else {
				chars.put(fieldName, Field.build(fieldName, newFieldValues.get(fieldName)));
			}
		}

		pInput.setFields(chars);

		// transform input fields
		Patient pNormalized = Config.instance.getRecordTransformer().transform(pInput);
		// set input fields
		pNormalized.setInputFields(chars);
		// Save existing patient as string for audit trail
		String pOld = pToEdit.toString();

		// assign changed fields to patient in database, persist
		pToEdit.setFields(pNormalized.getFields());
		pToEdit.setInputFields(pNormalized.getInputFields());

		for (String idType : IDGeneratorFactory.instance.getExternalIdTypes()) {
			if (newFieldValues.containsKey(idType)) {
				// check if a patient already has this external ID (not null)
				ID patientExtId = pToEdit.getId(idType);
				if (patientExtId != null) {
					logger.error("External ID " + patientExtId.getIdString() + " of this type (" + 
							patientExtId.getType() + ") already exists and cannot be overwritten");
					throw new WebApplicationException(
							Response.status(Status.CONFLICT)
									.entity("Cannot edit a patient, because external ID cannot be overwritten. " +
											"Please exclude existing external ID from input and repeat the request.")
									.build());
				}
				// check if this external id is already in use
				ID extId = IDGeneratorFactory.instance.buildId(idType, newFieldValues.get(idType));
				if (Persistor.instance.getPatient(extId) != null) {
					logger.info("Request to add patient with existing external ID " + extId.toString());
					throw new WebApplicationException(
							Response.status(Status.CONFLICT)
									.entity("Cannot create a new patient with the supplied external ID, " +
											"because it is already in use. " +
											"Please check external ID and repeat the request.")
									.build());

				}
				pToEdit.addId(extId);
			}
		}

		// Save to database
		if (Config.instance.auditTrailIsOn()) {
			for (ID id : pToEdit.getIds()) {
				//Prepare the audit trail record
				AuditTrail at = buildAuditTrailRecord(tokenId,
						id.getIdString(),
						id.getType(),
						"edit",
						pOld,
						pToEdit.toString());
				//Persist patient and audit trail accordingly
				Persistor.instance.createAuditTrail(at);
			}
		}

		Persistor.instance.updatePatient(pToEdit);
	}

	public AuditTrail buildAuditTrailRecord(String tokenId, String idString, String idType, String changeType, String oldRecord, String newRecord) {
		// Get token for this action, its ID has allready been checked by the caller's parent
		Token t = Servers.instance.getTokenByTid(tokenId);

		// Get remote IP for this token
		String remoteIP = Servers.instance.getRemoteIpByTid(tokenId);
		if (remoteIP == null) {
			String infoLog = "Remote IP for Token with ID " + tokenId + " could not be determined. Token was invalidated by a concurrent request or the session timed out during this request.";
			logger.info(infoLog);
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Please supply a valid 'editPatient' token.")
					.build());
		}
		//Build audit trail record, debug aware to prevent NPE and keep audit trail consistent
		AuditTrail at = new AuditTrail( new Date(),
				idString,
				idType,
				(Config.instance.debugIsOn()) ? "debug" : t.getDataItemMap("auditTrail")
						.get("username")
						.toString(),
				(Config.instance.debugIsOn()) ? "debug" : t.getDataItemMap("auditTrail")
						.get("remoteSystem")
						.toString(),
				remoteIP,
				changeType,
				(Config.instance.debugIsOn()) ? "debug" : t.getDataItemMap("auditTrail")
						.get("reasonForChange")
						.toString(),
				oldRecord,
				newRecord);
		return at;
	}

	/**
	 * Get a session for use in debug mode.
	 * @return The debug session.
	 */
	private Session getDebugSession() {
		if (debugSession == null
				|| Servers.instance.getSession(debugSession.getId()) == null) {
			debugSession = Servers.instance.newSession();
		}
		return debugSession;
	}
}
