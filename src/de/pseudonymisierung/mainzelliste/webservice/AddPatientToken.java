package de.pseudonymisierung.mainzelliste.webservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authorizes to add a patient to the database by his IDAT and receive an ID
 * (pseudonym) back.
 */
public class AddPatientToken extends Token {

	/** Fields transmitted on token creation. */
	private Map<String, String> fields = new HashMap<String, String>();
	/** The ID types that should be returned when making the ID request. */
	private Set<String> requestedIdTypes = new HashSet<String>();

	/**
	 * Create an instance with the given id.
	 * 
	 * @param tid
	 *            The token id.
	 */
	public AddPatientToken(String tid) {
		super(tid, "addPatient");

		// read fields from JSON data
		this.fields = new HashMap<String, String>();
	}
	
	/**
	 * Create an instance without setting the token id.
	 */
	AddPatientToken() {
		super();
		this.setType("addPatient");
	}

	@Override
	public void setData(Map<String, ?> data) {
		super.setData(data);
		// read fields from JSON data
		this.fields = new HashMap<String, String>();
		if (this.getData().containsKey("fields")) {
			Map<String, ?> serverFields = this.getDataItemMap("fields");
			for (String key : serverFields.keySet()) {
				String value = serverFields.get(key).toString();
				fields.put(key, value);
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
	 * Get the ID types that should be returned when making the ID request.
	 * 
	 * @return The set of requested ID types.
	 */
	public Set<String> getRequestedIdTypes() {
		return this.requestedIdTypes;
	}
}
