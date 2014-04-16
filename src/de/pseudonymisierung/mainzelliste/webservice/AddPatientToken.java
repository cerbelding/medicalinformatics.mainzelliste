package de.pseudonymisierung.mainzelliste.webservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddPatientToken extends Token {

	/**
	 * Fields transmitted on token creation
	 */
	private Map<String, String> fields = new HashMap<String, String>();
	private Set<String> requestedIdTypes = new HashSet<String>();

	public AddPatientToken(String tid, String type) {
		super(tid, type);

		// read fields from JSON data
		this.fields = new HashMap<String, String>();
	}
	
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
	 */
	public Map<String, String> getFields() {
		return this.fields;
	}
	
	public Set<String> getRequestedIdTypes() {
		return this.requestedIdTypes;
	}
}
