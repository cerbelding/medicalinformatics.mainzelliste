package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.blocker.BlockingKey;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.matcher.Matcher;
import de.pseudonymisierung.mainzelliste.matcher.NullMatcher;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import de.sessionTokenSimulator.PatientRecords;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Backend methods for handling patients. Implemented as a singleton object,
 * which can be referenced by PatientBackend.instance.
 */
public enum PatientBackend {

	/** The singleton instance. */
	instance;

	/** The logging instance */
	private Logger logger = LogManager.getLogger(this.getClass());

	/** The TLS context depending on the configuration parameters */
	private SSLConnectionSocketFactory sslsf;

	/**
	 * Creates an instance. Invoked on first access to {@link PatientBackend#instance}.
	 */
	PatientBackend() {
    }

  private IDRequest createNewPatient(Patient inputPatient, Set<String> requestedIdTypes,
      boolean sureness, Set<BlockingKey> blockingKeys, String tokenId) {
        // find match with the given IDAT of the input patient
        MatchResult match = findMatch(inputPatient, blockingKeys);

        Patient assignedPatient; // The "real" patient that is assigned (match result or new patient)

        Set<String> transientIdTypes = IDGeneratorFactory.instance.getTransientIdTypes();
        Set<String> idTypes = requestedIdTypes.stream().filter(o -> (!transientIdTypes.contains(o))).collect(Collectors.toSet());
        Set<String> derivedIdTypes = requestedIdTypes.stream().filter(o -> (transientIdTypes.contains(o))).collect(Collectors.toSet());
        if (requestedIdTypes.isEmpty()) { // otherwise use the default ID type
          idTypes = new CopyOnWriteArraySet<>();
          idTypes.add(IDGeneratorFactory.instance.getDefaultIDType());
        }

        // generate requested ids and update fields if patient found
        switch (match.getResultType()) {
          case MATCH:
            assignedPatient = match.getBestMatchedPatient();
            // Firstly Generate/update from existing patient the persistent id types
            assignedPatient.updateFrom(inputPatient);
            for (String idType : idTypes) {
              assignedPatient.getOriginal().createId(idType);
            }
            for (String idType : derivedIdTypes) {
              assignedPatient.getOriginal().createId(idType);
            }

            // log token to separate concurrent request in the log file
            // Log message is not informative if new ID types were requested
            // TODO: Discuss, which ID should we log in this case
            ID returnedId;
            if (!idTypes.isEmpty()) {
              returnedId = assignedPatient.getOriginal().getId(idTypes.iterator().next());
            } else {
              returnedId = assignedPatient.getOriginal().getTransientId(derivedIdTypes.iterator().next());
            }
            logger.info("Found match with ID {} for ID request {}", returnedId.getIdString(), tokenId);
            break;

          case NON_MATCH:
          case POSSIBLE_MATCH:
            if (match.getResultType() == MatchResultType.POSSIBLE_MATCH && !sureness) {
              return new IDRequest(inputPatient.getInputFields(), idTypes, match, null);
            }

            // Generate internal IDs
            // Note: inputPatient already contain all externals ids
            if(IDGeneratorFactory.instance.isEagerGenerationOn()) {
              IDGeneratorFactory.instance.generateIds().forEach(inputPatient::addId);
            } else {
              // first generate all persistent ids
              idTypes.forEach(inputPatient::createId);
              transientIdTypes.forEach(inputPatient::createId);
            }

			// Send requests for SRL IDs
			for (String srlIDType : IDGeneratorFactory.instance.getSrlIdTypes()) {
				  PatientRecords records = new PatientRecords();
				  records.linkPatient(inputPatient, srlIDType, inputPatient.createId(srlIDType).getIdString());
			}

            for (String idType : idTypes) {
              ID currentId = inputPatient.createId(idType);
              logger.debug("Created new ID {} for ID request {}", currentId.getIdString(), tokenId);
            }

            for (String idType : transientIdTypes) {
              ID currentId = inputPatient.createId(idType);
              logger.debug("Created new transient ID {} for ID request {}", currentId.getIdString(), tokenId);
            }
            if (match.getResultType() == MatchResultType.POSSIBLE_MATCH) {
              inputPatient.setTentative(true);
              Patient bestMatchedPatient = match.getBestMatchedPatient();
              // log tentative and possible match ids
              inputPatient.getIds().stream()
                  .filter(id -> bestMatchedPatient.getId(id.getType()) != null)
                  .forEach(id -> logger.info("New ID {} is tentative. Found possible match "
                      + "with ID {}", id.getIdString(), bestMatchedPatient.getId(id.getType()).getIdString()));
            }
            assignedPatient = inputPatient;
            break;

          default:
            logger.error("Illegal match result: {}", match.getResultType());
            throw new InternalErrorException();
        }

        logger.info("Weight of best match: {}", match.getBestMatchedWeight());

        // persist id request and new patient
        return new IDRequest(inputPatient.getInputFields(), requestedIdTypes, match, assignedPatient);
  }

  /**
   * PID request. Looks for a patient with the specified data in the database. If a match is found,
   * the ID of the matching patient is returned. If no match or possible match is found, a new
   * patient with the specified data is created. If a possible match is found and the given
   * "sureness" value is true, a new patient is created.
   *
   * @param form  input data from the HTTP request.
   * @param requestedIdTypes  Input fields from the HTTP request.
   * @param sureness if true, add possible match patient.
   * @return A representation of the request and its result as an instance of {@link IDRequest}.
   */
  public synchronized IDRequest createAndPersistPatient(MultivaluedMap<String, String> form,
      Set<String> requestedIdTypes, boolean sureness, String tokenId) {
    // deserialize patient from form
    Patient inputPatient = createPatientFrom(form);

    // run record linkage
    final Set<BlockingKey> blockingKeys = new HashSet<>();
    IDRequest request = createNewPatient(inputPatient, requestedIdTypes, sureness, blockingKeys, tokenId);
    if (request.getAssignedPatient() == null) {
      return request;
    }

    // persist
    if (request.getMatchResult().getResultType().equals(MatchResultType.MATCH)) {
      Persistor.instance.addIdRequest(request);
    } else {
      Persistor.instance.addIdRequest(request, blockingKeys);
    }

    // audit trail entry
    if (Config.instance.auditTrailIsOn()) {
      for (ID id : request.getAssignedPatient().getIds()) {
        AuditTrail at = buildAuditTrailRecord(tokenId,
            id.getIdString(),
            id.getType(),
            request.getMatchResult().getResultType(),
            null,
            request.getAssignedPatient().toString()
        );
        Persistor.instance.createAuditTrail(at);
      }
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
	public void editPatient(ID patientId, Map<String, String> newFieldValues, String tokenId) {
        // Check that provided ID is valid
        if (patientId == null) {
            // Calling methods should provide a legal id, therefore log an error if id is null
            logger.error("editPatient called with null id.");
            throw new InternalErrorException("An internal error occured: editPatients called with null. Please contact the administrator.");
        }
        Patient pToEdit = Persistor.instance.getPatient(patientId);
        if (pToEdit == null) {
            logger.info("Request to edit patient with unknown ID {}", patientId.toString());
            throw new InvalidIDException("No patient found with ID " + patientId.toString());
        }

        // validate input
        Validator.instance.validateForm(newFieldValues, false);
        // read input fields from form
        Patient pInput = new Patient();
        Map<String, Field<?>> chars = new HashMap<String, Field<?>>();

		for (String fieldName : pToEdit.getFields().keySet()) {
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
                // check if this external id is already in use
                ID extId = IDGeneratorFactory.instance.buildId(idType, newFieldValues.get(idType));
                Patient pDuplicate = Persistor.instance.getPatient(extId);
                if (pDuplicate != null && !pDuplicate.sameAs(pToEdit)) {
                    logger.info("Request to add patient with existing external ID {}", extId.toString());
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

    /**
     * Check if the patient with the given idat exist
     * @param inputFields idat
     * @return best match patient with matching weight
     */
    public MatchResult findMatch(MultivaluedMap<String, String> inputFields) {
        return findMatch(createPatientFrom(inputFields), Collections.emptySet());
    }

    private MatchResult findMatch(Patient inputPatient, Set<BlockingKey> bks) {
        // input patient should contain only external Ids
        Set<ID> externalIds = inputPatient.getIds();
        // External Id matching
        if (!externalIds.isEmpty()) {
            // Find matches with all given external IDs
            Set<Patient> idMatches = externalIds.stream().map(Persistor.instance::getPatient)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            if (idMatches.size() > 1) {// Found multiple patients with matching external ID
                throw new WebApplicationException(Response.status(Status.CONFLICT)
                        .entity("Multiple patients found with the given external IDs!").build());
            } else if (idMatches.isEmpty()) { // No patient with matching external ID
                MatchResult idatMatchResult = findMatchWithIdat(inputPatient, bks);
                if (idatMatchResult != null && idatMatchResult.getResultType() == MatchResultType.MATCH) {
                    // No match with ID, but with IDAT.
                    // Check that no conflicting external ID exists
                    boolean conflict = !externalIds.stream().allMatch(id -> {
                        ID idOfMatch = idatMatchResult.getBestMatchedPatient().getId(id.getType());
                        return (idOfMatch == null || id.getIdString().equals(idOfMatch.getIdString()));
                    });

                    if (conflict) {
                        throw new WebApplicationException(
                                Response.status(Status.CONFLICT)
                                        .entity("Found existing patient with matching IDAT but conflicting external ID(s).")
                                        .build());
                    }

                    return idatMatchResult;
                } else { // No id match, no IDAT match
                    return new MatchResult(MatchResultType.NON_MATCH, null, 0);
                }
            } else { // idMatches.size() == 1 : Found patient with matching external ID
                final Patient idMatch = idMatches.iterator().next();
                boolean idMatchHasIdat = Validator.instance.getRequiredFields().stream().allMatch(f ->
                        idMatch.getFields().containsKey(f) && !idMatch.getFields().get(f).isEmpty());
                // Check if IDAT of input and match (if present) matches
                if (!inputPatient.getFields().isEmpty() && idMatchHasIdat) {
                    MatchResult matchWithIdMatch = Config.instance.getMatcher().match(inputPatient, Collections.singletonList(idMatch));
                    if (matchWithIdMatch.getResultType() != MatchResultType.MATCH) {
                        logger.debug("Best matching weight on ID Matching: {}", matchWithIdMatch.getBestMatchedWeight());
                        throw new WebApplicationException(
                                Response.status(Status.CONFLICT)
                                        .entity("Found existing patient with matching external ID but conflicting IDAT!")
                                        .build());
                    }
                }
                // If an IDAT match exists, check if it is the same patient
                MatchResult idatMatchResult = findMatchWithIdat(inputPatient, bks);
                if (idatMatchResult != null && idatMatchResult.getResultType() == MatchResultType.MATCH &&
                        !idatMatchResult.getBestMatchedPatient().equals(idMatch)) {
                    throw new WebApplicationException(
                            Response.status(Status.CONFLICT)
                                    .entity("External ID and IDAT match with different patients, respectively!")
                                    .build());
                }

                return new MatchResult(MatchResultType.MATCH, idMatch, 1.0);
            }
        } else if (inputPatient.getFields().isEmpty()) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity("Neither complete IDAT nor an external ID has been given as input!").build());
        }
        return findMatchWithIdat(inputPatient, bks);
    }

    private MatchResult findMatchWithIdat(Patient inputPatient, Set<BlockingKey> bks) {
        MatchResult result = null;
        if (!inputPatient.getFields().isEmpty()) {
            // Blocking key extraction
            bks.addAll(Config.instance.getBlockingKeyExtractors().extract(inputPatient));

            // Matching
            final Matcher matcher = Config.instance.getMatcher();
            List<Patient> candidatePatients;
            if (matcher instanceof NullMatcher) {
                candidatePatients = new ArrayList<>();
            } else {
                candidatePatients = Persistor.instance.getPatients(bks);
            }
            result = matcher.match(inputPatient, candidatePatients);
            logger.debug("Best matching weight for IDAT matching: {}", result.getBestMatchedWeight());
        }
        return result;
    }

    private Patient createPatientFrom(MultivaluedMap<String, String> forms) {
        // create external Id list
        List<ID> externalIds = IDGeneratorFactory.instance.getExternalIdTypes().stream()
                .filter(forms::containsKey)
                .map(idType -> IDGeneratorFactory.instance.buildId(idType, forms.getFirst(idType)))
                .collect(Collectors.toList());

        // check if required fields are available
        if (forms.keySet().containsAll(Validator.instance.getRequiredFields())) {
            Validator.instance.validateForm(forms, false);

            // normalize and transform fields of the new patient
            Map<String, Field<?>> inputFields = mapMapWithConfigFields(forms);
            Patient inputPatient =  Config.instance.getRecordTransformer()
                    .transform(new Patient(new HashSet<>(externalIds), inputFields));
            inputPatient.setInputFields(inputFields);
            return inputPatient;
        } else {
            return new Patient(new HashSet<>(externalIds), null);
        }
    }

  public AuditTrail buildAuditTrailRecord(String tokenId, String idString, String idType,
      MatchResultType matchResult, String oldRecord, String newRecord) {
    String changeType;
    switch (matchResult) {
      case POSSIBLE_MATCH:
        changeType = "tentative";
        break;
      case MATCH:
        changeType = "match";
        break;
      default: // if = "NON_MATCH" Note: "ambiguous" is never user
        changeType = "create";
        break;
    }
    return buildAuditTrailRecord(tokenId, idString, idType, changeType, oldRecord, newRecord);
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

	private Map<String, Field<?>> mapMapWithConfigFields(MultivaluedMap<String, String> form) {
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
