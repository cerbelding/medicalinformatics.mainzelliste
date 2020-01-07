package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.Servers.ApiVersion;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.webservice.AddPatientToken;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;


/**
 * Backend methods for handling patients. Implemented as a singleton object,
 * which can be referenced by PatientBackend.instance.
 */
public enum PatientBackend {

    /**
     * The singleton instance.
     */
    instance;

    /**
     * The logging instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * Creates an instance. Invoked on first access to {@link PatientBackend#instance}.
     */
    private PatientBackend() {
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
     * @param tokenId    ID of a valid "addPatient" token.
     * @param form       Input fields from the HTTP request.
     * @param apiVersion The API version to use.
     * @return A representation of the request and its result as an instance of
     * {@link IDRequest}.
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
            if (Config.instance.debugIsOn()) {
                Session s = getDebugSession();
                try {
                    s.setURI(new URI("debug"));
                } catch (URISyntaxException e) {
                    throw new Error();
                }

                t = new AddPatientToken();
                Servers.instance.registerToken(s.getId(), t);
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
        MatchResult match = new MatchResult(MatchResultType.NON_MATCH, null, 0);
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

            if (t == null) {
                String infoLog = "Token with ID " + tokenId + " is invalid. It was invalidated by a concurrent request or the session timed out during this request.";
                logger.info(infoLog);
                throw new WebApplicationException(Response
                        .status(Status.UNAUTHORIZED)
                        .entity("Please supply a valid 'addPatient' token.")
                        .build());
            }
            logger.info("Handling ID Request with token " + t.getId());
            Patient p = new Patient();
            Patient pNormalized = new Patient();

            // get fields transmitted from MDAT server
            for (String key : t.getFields().keySet()) {
                form.add(key, t.getFields().get(key));
            }

            // get externally generated ids transmitted from MDAT server
            for (String key : t.getIds().keySet()) {
                form.add(key, t.getIds().get(key));
            }

            List<ID> externalIds = IDGeneratorFactory.instance.getExternalIdTypes().stream()
                    .map(idType -> form.containsKey(idType)
                            ? IDGeneratorFactory.instance.buildId(idType, form.getFirst(idType))
                            : null)
                    .filter(id -> id != null).collect(Collectors.toList());
            pNormalized.setIds(new HashSet<>(externalIds));

            boolean hasIdat = form.keySet().containsAll(Validator.instance.getRequiredFields());
            boolean hasExternalId = !externalIds.isEmpty();

            if (!hasIdat && !hasExternalId) {
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                        .entity("Neither complete IDAT nor an external ID has been given as input!").build());
            }

            final Patient idMatch;
            MatchResult idatMatch = null;

            if (hasIdat) {
                Validator.instance.validateForm(form, true);

				Map<String, Field<?>> chars = mapMapWithConfigFields(form);

                p.setFields(chars);

                // Normalization, Transformation
                pNormalized = Config.instance.getRecordTransformer().transform(p);
                pNormalized.setInputFields(chars);

                idatMatch = Config.instance.getMatcher().match(pNormalized, Persistor.instance.getPatients());
                logger.debug("Best matching weight for IDAT matching: " + idatMatch.getBestMatchedWeight());
            }
            if (hasExternalId) {
                // Find matches with all given external IDs
                Set<Patient> idMatches = externalIds.stream().map(id -> Persistor.instance.getPatient(id))
                        .filter(patient -> patient != null).collect(Collectors.toSet());
                if (idMatches.size() > 1) // Found multiple patients with matching external ID
                    throw new WebApplicationException(Response.status(Status.CONFLICT)
                            .entity("Multiple patients found with the given external IDs!").build());

                if (idMatches.size() == 0) { // No patient with matching external ID
                    if (hasIdat && idatMatch != null && idatMatch.getResultType() == MatchResultType.MATCH) {
                        // No match with ID, but with IDAT.
                        // Check that no conflicting external ID exists
                        MatchResult finalIdatMatch = idatMatch; // Make final copy for using in stream
                        boolean conflict = !externalIds.stream().allMatch(id -> {
                            ID idOfMatch = finalIdatMatch.getBestMatchedPatient().getId(id.getType());
                            return (idOfMatch == null || id.getIdString().equals(idOfMatch.getIdString()));
                        });

                        if (conflict) {
                            throw new WebApplicationException(
                                    Response.status(Status.CONFLICT)
                                            .entity("Found existing patient with matching IDAT but conflicting external ID(s).")
                                            .build());
                        }

                        // TODO: Felder / IDs aktualisieren -> auslagern in eigene Methode
                        match = idatMatch;
                    } else { // No id match, no IDAT match
                        match = new MatchResult(MatchResultType.NON_MATCH, null, 0);
                    }
                }

                if (idMatches.size() == 1) { // Found patient with matching external ID
                    idMatch = idMatches.iterator().next();
                    //boolean idMatchHasIdat = idMatch.getFields().keySet().containsAll(Validator.instance.getRequiredFields());
                    boolean idMatchHasIdat = Validator.instance.getRequiredFields().stream().allMatch(f ->
                            idMatch.getFields().containsKey(f) && !idMatch.getFields().get(f).isEmpty());
                    // Check if IDAT of input and match (if present) matches
                    if (hasIdat && idMatchHasIdat) {
                        MatchResult matchWithIdMatch = Config.instance.getMatcher().match(pNormalized, Arrays.asList(idMatch));
                        if (matchWithIdMatch.getResultType() != MatchResultType.MATCH) {
                            throw new WebApplicationException(
                                    Response.status(Status.CONFLICT)
                                            .entity("Found existing patient with matching external ID but conflicting IDAT!")
                                            .build());
                        }
                    }
                    // If an IDAT match exists, check if it is the same patient
                    if (idatMatch != null && (idatMatch.getResultType() == MatchResultType.MATCH
                    ) &&
                            !idatMatch.getBestMatchedPatient().equals(idMatch)) {
                        throw new WebApplicationException(
                                Response.status(Status.CONFLICT)
                                        .entity("External ID and IDAT match with different patients, respectively!")
                                        .build());
                    }

                    // TODO: Felder / IDs aktualisieren
                    match = new MatchResult(MatchResultType.MATCH, idMatch, 1.0);
                } else {
                    idMatch = null;
                }
            } else {
                match = idatMatch;
            }

            Patient assignedPatient; // The "real" patient that is assigned (match result or new patient)

            // If a list of ID types is given in token, return these types
            Set<String> idTypes;
            idTypes = t.getRequestedIdTypes();
            if (idTypes.size() == 0) { // otherwise use the default ID type
                idTypes = new CopyOnWriteArraySet<String>();
                idTypes.add(IDGeneratorFactory.instance.getDefaultIDType());
            }

            switch (match.getResultType()) {
                case MATCH:
                    for (String idType : idTypes)
                        returnIds.add(match.getBestMatchedPatient().getOriginal().getId(idType));

                    assignedPatient = match.getBestMatchedPatient();
                    assignedPatient.updateFrom(pNormalized);
                    Persistor.instance.updatePatient(assignedPatient);
                    // log token to separate concurrent request in the log file
                    logger.info("Found match with ID " + returnIds.get(0).getIdString() + " for ID request " + t.getId());

                    // Add optional fields if they are not already entered
//				Map<String, String> newFieldValues = new HashMap<String, String>();
                    Map<String, Field<?>> fieldSet = new HashMap<String, Field<?>>(assignedPatient.getFields());
                    Map<String, Field<?>> inputFieldSet = new HashMap<String, Field<?>>(assignedPatient.getInputFields());
                    boolean updatePatient = false;

                    for (String key : pNormalized.getFields().keySet()) {
                        if (!assignedPatient.getFields().containsKey(key)) {
                            updatePatient = true;
                            fieldSet.put(key, Field.build(key, pNormalized.getFields().get(key).toString()));
                            inputFieldSet.put(key, Field.build(key, pNormalized.getInputFields().get(key).toString()));
                        }
                    }

                    if (updatePatient) {
                        assignedPatient.setFields(fieldSet);
                        assignedPatient.setInputFields(inputFieldSet);
                        Persistor.instance.updatePatient(assignedPatient);
                    }

                    break;

                case NON_MATCH:
                case POSSIBLE_MATCH:
                    if (match.getResultType() == MatchResultType.POSSIBLE_MATCH
                            && (form.getFirst("sureness") == null || !Boolean.parseBoolean(form.getFirst("sureness")))) {
                        return new IDRequest(p.getFields(), idTypes, match, null, t);
                    }

                    // Generate internal IDs
                    boolean eagerGeneration = Boolean
                            .parseBoolean(Config.instance.getProperty("idgenerators.eagerGeneration"));
                    Set<ID> newIds = eagerGeneration ? IDGeneratorFactory.instance.generateIds()
                            : IDGeneratorFactory.instance.generateIds(idTypes);

                    // Import external IDs
                    // TODO Integrieren in Matchbehandlung
                    for (String extIDType : IDGeneratorFactory.instance.getExternalIdTypes()) {
                        String extIDString = form.getFirst(extIDType);
                        if (extIDString != null) {
                            ID extId = IDGeneratorFactory.instance.buildId(extIDType, extIDString);
                            newIds.add(extId);
                        }
                    }
                    pNormalized.setIds(newIds);

                    for (String idType : idTypes) {
                        ID thisID = pNormalized.getId(idType);
                        returnIds.add(thisID);
                        logger.info("Created new ID " + thisID.getIdString() + " for ID request " + (t == null ? "(null)" : t.getId()));
                    }
                    if (match.getResultType() == MatchResultType.POSSIBLE_MATCH) {
                        pNormalized.setTentative(true);
                        for (ID thisId : returnIds)
                            thisId.setTentative(true);
                        logger.info("New ID " + returnIds.get(0).getIdString() + " is tentative. Found possible match with ID " +
                                match.getBestMatchedPatient().getId(IDGeneratorFactory.instance.getDefaultIDType()).getIdString());
                    }
                    assignedPatient = pNormalized;
                    break;

                default:
                    logger.error("Illegal match result: " + match.getResultType());
                    throw new InternalErrorException();
            }

            logger.info("Weight of best match: " + match.getBestMatchedWeight());

            request = new IDRequest(p.getFields(), idTypes, match, assignedPatient, t);

            ret.put("request", request);

            Persistor.instance.addIdRequest(request);

        }

        return request;
    }

    /**
     * Set fields of a patient to new values.
     *
     * @param patientId      ID of the patient to edit.
     * @param newFieldValues Field values to set. Fields that do not appear as map keys are
     *                       left as they are. In order to delete a field value, provide an
     *                       empty string. All values are processed by field transformation
     *                       as defined in the configuration file.
     */
    public void editPatient(ID patientId, Map<String, String> newFieldValues) {
        // Check that provided ID is valid
        if (patientId == null) {
            // Calling methods should provide a legal id, therefore log an error if id is null
            logger.error("editPatient called with null id.");
            throw new InternalErrorException("An internal error occured: editPatients called with null. Please contact the administrator.");
        }
        Patient pToEdit = Persistor.instance.getPatient(patientId);
        if (pToEdit == null) {
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

        // assign changed fields to patient in database, persist
        pToEdit.setFields(pNormalized.getFields());
        pToEdit.setInputFields(pNormalized.getInputFields());

        for (String idType : IDGeneratorFactory.instance.getExternalIdTypes()) {
            if (newFieldValues.containsKey(idType)) {
                // check if this external id is already in use
                ID extId = IDGeneratorFactory.instance.buildId(idType, newFieldValues.get(idType));
                Patient pDuplicate = Persistor.instance.getPatient(extId);
                if (pDuplicate != null && !pDuplicate.sameAs(pToEdit)) {
                    logger.info("Request to add patient with existing external ID " + extId.toString());
                    throw new WebApplicationException(
                            Response.status(Status.CONFLICT)
                                    .entity("Cannot create a new patient with the supplied external ID, " +
                                            "because it is already in use. " +
                                            "Please check external ID and repeat the request.")
                                    .build());

                }
                if (pDuplicate == null) {
                    // check if a patient already has this external ID (not null)
                    ID patientExtId = pToEdit.getId(idType);
                    if (patientExtId != null) {
                        Set<ID> patientIds = pToEdit.getIds();
                        Set<ID> newIds = new HashSet<>();
                        for (ID id : patientIds) {
                            if (!id.getType().equals(idType)) {
                                newIds.add(id);
                            } else {
                                Persistor.instance.deleteId(id);
                            }
                        }
                        pToEdit.setIds(newIds);
                    }
                    if (!extId.getIdString().trim().isEmpty()) {
                        pToEdit.addId(extId);
                    } else {
                        Persistor.instance.deleteId(extId);
                    }
                }
            }
        }

        // Save to database
        Persistor.instance.updatePatient(pToEdit);
    }

	/**
	 * Get a session for use in debug mode.
	 * @return The debug session.
	 */
	private Session getDebugSession() {
		if (debugSession == null
				|| Servers.instance.getSession(debugSession.getId()) == null) {
			debugSession = Servers.instance.newSession("");
		}
		return debugSession;
	}

	public Map<String, Field<?>> mapMapWithConfigFields(MultivaluedMap<String, String> form) {
		Map<String, Field<?>> chars = new HashMap<String, Field<?>>();
		//Compare Fields from Config with requested Fields a
		for(String s: Config.instance.getFieldKeys()){
			if (form.containsKey(s)) {
				try {
					chars.put(s, Field.build(s, form.getFirst(s)));
				} catch (WebApplicationException we) {
					logger.error(String.format("Error while building field %s with input %s", s, form.getFirst(s)));
					throw we;
				}
			}
		}
		return chars;
	}
}
