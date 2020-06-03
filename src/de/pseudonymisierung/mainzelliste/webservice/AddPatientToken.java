package de.pseudonymisierung.mainzelliste.webservice;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidFieldException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Authorizes to add a patient to the database by his IDAT and receive an ID
 * (pseudonym) back.
 */
public class AddPatientToken extends Token {

	/** Fields transmitted on token creation. */
	private Map<String, String> fields;
	/** Ids transmitted on token creation (externally generated ids) */
	private Map<String, String> ids;
	/** The ID types that should be returned when making the ID request. */
	private Set<String> requestedIdTypes;

	/**
	 * Create a Token with type "addPatient".
	 */
	public AddPatientToken() {
		this(1);
	}

	public AddPatientToken(int allowedUses) {
		super("addPatient", allowedUses);
		this.fields = new HashMap<>();
		this.ids = new HashMap<>();
		this.requestedIdTypes = new HashSet<>();
	}

	@Override
	public void setData(Map<String, ?> data) {
		super.setData(data);
		// read fields from JSON data
		this.fields = new HashMap<>();
		if (this.getData().containsKey("fields")) {
			Map<String, ?> serverFields = this.getDataItemMap("fields");
			for (Map.Entry<String, ?> entry : serverFields.entrySet()) {
				if (!Config.instance.fieldExists(entry.getKey()))
					throw new InvalidFieldException("Unknown field '" + entry.getKey() + "'.");
				fields.put(entry.getKey(), entry.getValue().toString());
			}
		}

		// read external ids from JSON data
		this.ids = new HashMap<>();
		if (this.getData().containsKey("ids")) {
			Map<String, ?> serverIds = this.getDataItemMap("ids");
			for (Map.Entry<String, ?> entry : serverIds.entrySet()) {
				if (!IDGeneratorFactory.instance.getExternalIdTypes().contains(entry.getKey()))
					throw new InvalidIDException("Unknown id type '" + entry.getKey() + "'.");
				ids.put(entry.getKey(), entry.getValue().toString());
			}
		}

		this.requestedIdTypes = new HashSet<String>();
		if (this.hasDataItem("idTypes")) {
			List<?> idtypes = this.getDataItemList("idTypes");
			for (Object o : idtypes) {
				this.requestedIdTypes.add(o.toString());
			}
		} else if (this.hasDataItem("idtypes")) { // pre-2.0 API
			List<?> idtypes = this.getDataItemList("idtypes");
			for (Object o : idtypes) {
				this.requestedIdTypes.add(o.toString());
			}
		}
		else if (this.hasDataItem("idtype")) { // even older api
				requestedIdTypes.add(this.getDataItemString("idtype"));
		}
	}

	/**
	 * Return the fields transmitted on token creation.
	 *
	 * @return A map where keys are field names and values the respective field
	 *         values.
	 */
	public Map<String, String> getFields() {
		return this.fields;
	}

	/**
	 * Return the ids transmitted on token creation.
	 *
	 * @return A map where keys are id types and values the respective id strings
	 */
	public Map<String, String> getIds() {
		return this.ids;
	}

	/**
	 * Get the ID types that should be returned when making the ID request.
	 *
	 * @return The set of requested ID types.
	 */
	public Set<String> getRequestedIdTypes() {
		return this.requestedIdTypes;
	}
	
	/**
	 * Query whether this token permits to return possible matches for an unsure
	 * record linkage result. I.e., when POST /patients is performed with this
	 * token and the record linkage returns
	 * {@link MatchResultType#POSSIBLE_MATCH}, the IDs of patients that are
	 * similar to the requested patient are returned in the response.
	 * 
	 * @return True if possible matches are returned.
	 */
	public boolean showPossibleMatches() {
		return Boolean.TRUE.equals(this.getData().get("showPossibleMatches"));
	}
}
