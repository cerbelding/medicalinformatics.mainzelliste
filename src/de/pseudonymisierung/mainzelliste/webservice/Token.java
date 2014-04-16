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
package de.pseudonymisierung.mainzelliste.webservice;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.uri.UriTemplate;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.PID;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.Servers.ApiVersion;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;

/**
 * A temporary "ticket" to realize authorization and/or access to a resource.
 * Tokens are accessible via their token id (e.g. GET /patients/tempid/{tid}),
 * but also connected to a {@link Session} (e.g. DELETE /sessions/{sid}).
 * Thus, they are created using a session.
 */
public class Token {
	private String id;
	private String type;
	private Map<String, ?> data;
	
	Token() {}
	
	public Token(String tid, String type) {
		this.id = tid;
		this.type = type;
		this.data = new HashMap<String, Object>();		
	}
	
	/** 
	 * Check if a token is valid, i.e. it has a known type and
	 * the data items for the specific type have the correct format.
	 * 
	 * @throws InvalidTokenException if the format is incorrect. A specific error message
	 * is returned to the client with status code 400 (bad request).
	 */
	public void checkValidity(ApiVersion apiVersion) throws InvalidTokenException {
		if (this.type.equals("addPatient"))
			this.checkAddPatient(apiVersion);
		else if (this.type.equals("readPatients"))
			this.checkReadPatients();
		else
			throw new InvalidTokenException("Token type " + this.type + " unknown!");		
	}
	
	/**
	 * Check if this token has the expected type.
	 * @param expected The expected token type.
	 * @throws InvalidTokenException If this token's type does not match the expected type.
	 */
	public void checkTokenType(String expected) {
		if (expected == null || !expected.equals(this.getType()))
			throw new InvalidTokenException("Invalid token type: Expected: " + expected + ", actual: " + this.getType());
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Map<String, ?> getData() {
		return data;
	}
	
	/**
	 * Get a particular data element by its key.
	 * This method is preferable to getData().get() as it handles the case data==null safely. 
	 * @param item
	 * @return The requested data item. Null if no such item exists or if no data is attached to
	 * the token (data==null). 
	 */
	public String getDataItemString(String item) {
		if (this.data == null)
			return null;
		else
			return (String) data.get(item);
	}

	public List<?> getDataItemList(String item) throws ClassCastException {
		if (this.data == null)
			return null;
		else
			return (List<?>) data.get(item);
	}
	
	public boolean hasDataItem(String item) {
		return this.data.containsKey(item);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, ?> getDataItemMap(String item) throws ClassCastException {
		if (this.data == null)
			return null;
		else
			return (Map<String, Object>) data.get(item);
	}

	public void setData(Map<String, ?> data) {
		this.data = data;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Token)) return false;
		
		Token t2 = (Token)obj;
		return t2.id.equals(this.id);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@SuppressWarnings("unchecked")
	private void checkAddPatient(ApiVersion apiVersion)  {
		// check requested id types
		if (apiVersion.majorVersion >= 2) {
			this.checkIdTypes((List<String>) this.getDataItemList("idTypes"));
		} else if (this.hasDataItem("idtypes")) {
			this.checkIdTypes((List<String>) this.getDataItemList("idtypes"));
		} else if (this.hasDataItem("idtype")) {
			LinkedList<String> idTypes= new LinkedList<String>();
			String requestedIdType = this.getDataItemString("idtype");
			// If id type is not specified in api version 1.0, the default id type is used
			if (requestedIdType != null) {
				idTypes.add(requestedIdType);
				this.checkIdTypes(idTypes);
			}
		}

		
		// Check callback URL
		String callback = this.getDataItemString("callback");
		if (callback != null && !callback.equals("")) {
			if (!Pattern.matches(Config.instance.getProperty("callback.allowedFormat"), callback)) {
				throw new InvalidTokenException("Callback address " + callback + " does not conform to allowed format!");						
			}
			try {
				@SuppressWarnings("unused")
				URI callbackURI = new URI(callback);
			} catch (URISyntaxException e) {
				throw new InvalidTokenException("Callback address " + callback + " is not a valid URI!");
			}
		}
		
		// Check redirect URL
		if (this.hasDataItem("redirect")) {
			UriTemplate redirectURITempl;
			try {
				redirectURITempl= new UriTemplate(this.getDataItemString("redirect"));
			} catch (Throwable t) {
				throw new InvalidTokenException("The URI template for the redirect address seems to be malformed: " + 
						this.getDataItemString("redirect"));
			}
			
			List<String> definedIdTypes = Arrays.asList(IDGeneratorFactory.instance.getIDTypes());
			for (String templateVar : redirectURITempl.getTemplateVariables()) {
				if (!templateVar.equals("tokenId") && !definedIdTypes.contains(templateVar))
					throw new InvalidTokenException("The URI template for the redirect address contains the undefined id type " + templateVar + ".");
			}
		}
		
		// Check provided field values
		if (this.getData().containsKey("fields"))
		{			
			try {
				Map<String, ?> fields = this.getDataItemMap("fields");
				for (String thisField : fields.keySet()) {
					if (!Config.instance.getFieldKeys().contains(thisField))
						throw new InvalidTokenException("Field " + thisField + " provided in field list is unknown!");
				}
			} catch (ClassCastException e) {
				throw new InvalidTokenException("Illegal format for data item 'fields'! " +
						"Please provide a list of fields as key-value pairs.");
			}
		}
	}
	
	private void checkReadPatients() {

		// check that IDs to search for are provided
		if (!this.getData().containsKey("searchIds"))
			throw new InvalidTokenException(
					"Please provide an array of IDs as field 'searchIds' in token data!");
		List<?> searchIds;
		try {
			searchIds = this.getDataItemList("searchIds");
		} catch (ClassCastException e) {
			throw new InvalidTokenException("Field 'searchIds' has wrong format. Expected array of IDs, received: "
					+ this.getData().get("searchIds"));
		}
		
		for (Object item : searchIds) {
			String idType;
			String idString;
			try {
				@SuppressWarnings("unchecked")
				Map<String, String> thisSearchId = (Map<String, String>) item;
				idType = thisSearchId.get("idType");
				idString = thisSearchId.get("idString");
				if (idType == null || idString == null)
					throw new Exception();
			} catch (Exception e) {
				throw new InvalidTokenException("Every item in 'searchIds' must be an object with fields 'idType' and 'idString'. Found: " + item.toString());
			}
			checkIdType(idType);
	
			Patient p = Persistor.instance.getPatient(new PID(idString, idType));
			if (p == null) {
				throw new InvalidTokenException("No patient found with provided "
						+ idType + " '" + idString + "'!");
			}
		}

		// Check fields
		if (this.getData().containsKey("fields")) {
			
			try {
				List<?> fields = this.getDataItemList("fields");
				for (Object thisField : fields) {
					if (!Config.instance.getFieldKeys().contains(thisField.toString()))
						throw new InvalidTokenException("Field " + thisField + " provided in field list is unknown!");
				}
			} catch (ClassCastException e) {
				throw new InvalidTokenException("Illegal format for data item 'fields'! " +
						"Please provide a list of field names.");
			}
		}
	}
	
	/**
	 * Check that the provided list contains only valid id types.
	 * 
	 * @throws InvalidTokenException if the check fails.
	 */
	private void checkIdTypes(Collection<String> listIdTypes) throws InvalidTokenException {
		// if no idTypes are defined, there is nothing to check
		if (listIdTypes == null)
			return;
		
		try {
			// Get defined ID types
			// Check for every type supplied in the token if it is in the list of defined types
			for (Object thisIdType : listIdTypes) {
				String thisIdTString = (String) thisIdType;
				checkIdType(thisIdTString);
			}
			// if we end up here, everything is okay
			return;
		} catch (ClassCastException e) {
			// If one of the casts went wrong, the format of the input JSON data was probably incorrect.
			throw new InvalidTokenException("Illegal format for data item 'idtypes': Must be array of Strings.");
		}
	}
	
	private void checkIdType(String idType) throws InvalidTokenException {
		List<String> definedIdTypes = Arrays.asList(IDGeneratorFactory.instance.getIDTypes());
		if (!definedIdTypes.contains(idType))
			throw new InvalidTokenException("'" + idType + "'" + " is not a known ID type!");
	}
	
	public JSONObject toJSON(ApiVersion apiVersion) throws Exception {
		JSONObject ret = new JSONObject();
		// uri not known in this context -> assing in SessionsResource
		if (apiVersion.majorVersion >= 2) {
			ret.put("id", this.id)
			.put("type", this.type);
			
			ObjectMapper mapper = new ObjectMapper();
			String dataString = mapper.writeValueAsString(data);
			ret.put("data", new JSONObject(dataString));
		} else {
			ret.put("tokenId", this.id);		
		}
		return ret;
	}

}
