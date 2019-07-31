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
package de.pseudonymisierung.mainzelliste.webservice;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.uri.UriTemplate;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.Session;
import de.pseudonymisierung.mainzelliste.Servers.ApiVersion;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;

/**
 * A temporary "ticket" to realize authorization and/or access to a resource.
 * Tokens are used by providing their token id (e.g. GET
 * /patients/tokenId/{tid}), but also connected to a {@link Session} (e.g.
 * DELETE /sessions/{sid}). Thus, they are created using a session.
 */
public class Token {

	/** A unique identifier. */
	private String id;
	/** URI of the token. */
	private URI uri = null;
	/** Type, such as "addPatient", "readPatient" etc. */
	private String type;
	/**
	 * Data depending on the token type, such as: The ID to create, the patient
	 * to edit, ...
	 */
	private Map<String, ?> data;

	private String parentSessionId;

	private String parentServerName;
	/**
	 * Create emtpy instance. Used internally only.
	 */
	Token() {
	}

	/**
	 * Create token with the given id and type. Initializes empty container for
	 * token data. Performs no checking if the given token id is unique and if
	 * the provided token type is known.
	 *
	 * @param tid
	 *            The token id.
	 * @param type
	 *            The token type.
	 */
	public Token(String tid, String type) {
		this.id = tid;
		this.type = type;
		this.data = new HashMap<String, Object>();
	}

	/**
	 * Check if a token is valid, i.e. it has a known type and the data items
	 * for the specific type have the correct format.
	 *
	 * @param apiVersion
	 *            API version to use for the check.
	 *
	 * @throws InvalidTokenException
	 *             if the format is incorrect. A specific error message is
	 *             returned to the client with status code 400 (bad request).
	 */
	public void checkValidity(ApiVersion apiVersion)
			throws InvalidTokenException {
		if (this.type.equals("addPatient"))
			this.checkAddPatient(apiVersion);
		else if (this.type.equals("readPatients"))
			this.checkReadPatients();
		else if (this.type.equals("editPatient"))
			this.checkEditPatient(apiVersion);
		else if (this.type.equals("deletePatient"));
		else
			throw new InvalidTokenException("Token type " + this.type
					+ " unknown!");
	}

	/**
	 * Check if this token has the expected type.
	 *
	 * @param expected
	 *            The expected token type.
	 * @throws InvalidTokenException
	 *             If this token's type does not match the expected type.
	 */
	public void checkTokenType(String expected) {
		if (expected == null || !expected.equals(this.getType()))
			throw new InvalidTokenException("Invalid token type: Expected: "
					+ expected + ", actual: " + this.getType(), Status.UNAUTHORIZED);
	}

	/**
	 * Get the unique id of this token.
	 *
	 * @return The token id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the unique id of this token. Performs no check of uniqueness.
	 *
	 * @param id
	 *            The new token id.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the URI of this token.
	 *
	 * @return The token URI.
	 */
	public URI getURI() {
		return this.uri;
	}

	/**
	 * Set the URI of this token.
	 *
	 * @param uri
	 *            The new token URI.
	 */
	public void setURI(URI uri) {
		this.uri = uri;
	}

	/**
	 * Get the type of this token, e.g. "addPatient", "editPatient" etc.
	 *
	 * @return The token type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of this token, e.g. "addPatient", "editPatient" etc.
	 *
	 * @param type
	 *            The new token type.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the data container of this token.
	 *
	 * @return A map where keys are the names of data items and values the data
	 *         items. Data items can be map or collection types again.
	 */
	public Map<String, ?> getData() {
		return data;
	}

	/**
	 * Get a particular data element by its key. This method is preferable to
	 * getData().get() as it handles the case data==null safely.
	 *
	 * @param item
	 *            The name of the data item to get.
	 * @return The requested data item. Null if no such item exists or if no
	 *         data is attached to the token (data==null).
	 */
	public String getDataItemString(String item) {
		if (this.data == null)
			return null;
		else
			return (String) data.get(item);
	}

	/**
	 * Get a particular data element by its key. Assumes that the requested data
	 * item is a list.
	 *
	 * @param item
	 *            The name of the data item to get.
	 * @return The requested data item. Null if no such item exists or if no
	 *         data is attached to the token (data==null).
	 * @throws ClassCastException
	 *             If the requested data item exists but is not a list.
	 */
	public List<?> getDataItemList(String item) throws ClassCastException {
		if (this.data == null)
			return null;
		else
			return (List<?>) data.get(item);
	}

	/**
	 * Check whether the token has the given data item.
	 *
	 * @param item
	 *            The name of the data item to check.
	 * @return true if a data item with the given name exists.
	 */
	public boolean hasDataItem(String item) {
		return this.data != null && this.data.containsKey(item);
	}

	/**
	 * Get a particular data element by its key. Assumes that the requested data
	 * item is a map.
	 *
	 * @param item
	 *            The name of the data item to get.
	 * @return The requested data item. Null if no such item exists or if no
	 *         data is attached to the token (data==null).
	 * @throws ClassCastException
	 *             If the requested data item exists but is not a map.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, ?> getDataItemMap(String item) throws ClassCastException {
		if (this.data == null)
			return null;
		else
			return (Map<String, Object>) data.get(item);
	}

	/**
	 * Set the data container to the provided map.
	 *
	 * @param data
	 *            The new data container, copied by reference.
	 */
	public void setData(Map<String, ?> data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Token))
			return false;

		Token t2 = (Token) obj;
		return t2.id.equals(this.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * Check whether this is a valid addPatient token.
	 *
	 * @param apiVersion
	 *            The API version to use.
	 */
	@SuppressWarnings("unchecked")
	private void checkAddPatient(ApiVersion apiVersion) {
		// check requested id types
		if (this.hasDataItem("idTypes")) {
			this.checkIdTypes((List<String>) this.getDataItemList("idTypes"));
		} else if (this.hasDataItem("idtypes")) {
			this.checkIdTypes((List<String>) this.getDataItemList("idtypes"));
		} else if (this.hasDataItem("idtype")) {
			LinkedList<String> idTypes = new LinkedList<String>();
			String requestedIdType = this.getDataItemString("idtype");
			// If id type is not specified in api version 1.0, the default id
			// type is used
			if (requestedIdType != null) {
				idTypes.add(requestedIdType);
				this.checkIdTypes(idTypes);
			}
		}

		// Check callback URL
		String callback = this.getDataItemString("callback");
		if (callback != null && !callback.equals("")) {
			if (!Pattern.matches(
					Config.instance.getProperty("callback.allowedFormat"),
					callback)) {
				throw new InvalidTokenException("Callback address " + callback
						+ " does not conform to allowed format!");
			}
			try {
				@SuppressWarnings("unused")
				URI callbackURI = new URI(callback);
			} catch (URISyntaxException e) {
				throw new InvalidTokenException("Callback address " + callback
						+ " is not a valid URI!");
			}
		}

		// Check redirect URL
		if (this.hasDataItem("redirect")) {
			UriTemplate redirectURITempl;
			try {
				redirectURITempl = new UriTemplate(
						this.getDataItemString("redirect"));
			} catch (Throwable t) {
				throw new InvalidTokenException(
						"The URI template for the redirect address seems to be malformed: "
								+ this.getDataItemString("redirect"));
			}

			List<String> definedIdTypes = Arrays
					.asList(IDGeneratorFactory.instance.getIDTypes());
			for (String templateVar : redirectURITempl.getTemplateVariables()) {
				if (!templateVar.equals("tokenId")
						&& !definedIdTypes.contains(templateVar))
					throw new InvalidTokenException(
							"The URI template for the redirect address contains the undefined id type "
									+ templateVar + ".");
			}
		}

		// Check provided field values
		if (this.getData().containsKey("fields")) {
			try {
				Map<String, ?> fields = this.getDataItemMap("fields");
				for (String thisField : fields.keySet()) {
					if (!Config.instance.getFieldKeys().contains(thisField))
						throw new InvalidTokenException("Field " + thisField
								+ " provided in field list is unknown!");
				}
			} catch (ClassCastException e) {
				throw new InvalidTokenException(
						"Illegal format for data item 'fields'! "
								+ "Please provide a list of fields as key-value pairs.");
			}
		}
	}

	/**
	 * Check whether this is a valid readPatients token.
	 */
	private void checkReadPatients() {

		// check that IDs to search for are provided
		if (!this.getData().containsKey("searchIds"))
			throw new InvalidTokenException(
					"Please provide an array of IDs as field 'searchIds' in token data!");
		List<?> searchIds;
		try {
			searchIds = this.getDataItemList("searchIds");
		} catch (ClassCastException e) {
			throw new InvalidTokenException(
					"Field 'searchIds' has wrong format. Expected array of IDs, received: "
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
				throw new InvalidTokenException(
						"Every item in 'searchIds' must be an object with fields 'idType' and 'idString'. Found: "
								+ item.toString());
			}
			checkIdType(idType);

			if (!Persistor.instance.patientExists(idType, idString)) {
				throw new InvalidTokenException(
						"No patient found with provided " + idType + " '"
								+ idString + "'!");
			}
		}

		// Check fields
		checkResultFields();
		checkResultIds();
	}

	/**
	 *	Check if "resultFields" contains only valid field names.
	 */
	private void checkResultFields() {
		Set<String> fieldList = Config.instance.getFieldKeys();
		try {
			List<?> fields = this.getDataItemList("resultFields");
			
			if (fields == null)
				return; // Allow omitting resultFields (same semantics as providing empty array).
			
			for (Object thisField : fields) {
				if (!fieldList.contains(thisField.toString()))
					throw new InvalidTokenException("Field '" + thisField
							+ "' provided in field list is unknown!");
			}
		} catch (ClassCastException e) {
			throw new InvalidTokenException(
					"Illegal format for data item 'resultFields'! "
							+ "Please provide a list of field names.");
		}
	}

	/**
	 * Check if "resultIds" contains only valid ID types.
	 */
	private void checkResultIds() {
		Set<String> definedIdTypes = new HashSet<String>(Arrays.asList(IDGeneratorFactory.instance.getIDTypes()));
		try {
			List<?> resultIdTypes = this.getDataItemList("resultIds");

			if (resultIdTypes == null)
				return; // Allow omitting resultIds (same semantics as providing empty array).
			
			for (Object thisIdType : resultIdTypes) {
				if (!definedIdTypes.contains(thisIdType.toString()))
					throw new InvalidTokenException("ID type '" + thisIdType
							+ "' provided in ID type list is unknown!");
			}
		} catch (ClassCastException e) {
			throw new InvalidTokenException(
					"Illegal format for data item 'resultIds'! "
							+ "Please provide a list of ID types.");
		}
	}

	/**
	 * Check whether this is a valid editPatient token.
	 * Incompatible change in version 3:
	 * It is not possible anymore to use the token without fields in order to edit all fields
	 * Edit token without ids and fields throws an exception
	 */
	private void checkEditPatient(ApiVersion apiVersion) {
        // if API version < 3 and there are no fields in token, all fields can be edited
        // in this case all fields from configuration are added to token
        if (!this.getData().containsKey("fields") && apiVersion.majorVersion < 3) {
            Map<String, Object> dataWithAllFields = (Map<String, Object>)this.getData();

			ArrayList<String> fieldKeys = new ArrayList<String>();
            for (String fieldKey : Config.instance.getFieldKeys()) {
                fieldKeys.add(fieldKey);
            }

            dataWithAllFields.put("fields", fieldKeys);
            this.setData(dataWithAllFields);
        }

        // if there are no fields and ids in token, throw an exception
		if (!this.getData().containsKey("ids") && !this.getData().containsKey("fields")) {
			throw new InvalidTokenException("Token must contain at least one field or id to edit");
		}
		return;
	}

	/**
	 * Check that the provided list contains only valid id types.
	 *
	 * @param listIdTypes
	 *            The list of ID types to check.
	 * @throws InvalidTokenException
	 *             if the check fails.
	 */
	private void checkIdTypes(Collection<String> listIdTypes)
			throws InvalidTokenException {
		// if no idTypes are defined, there is nothing to check
		if (listIdTypes == null)
			return;

		try {
			// Get defined ID types
			// Check for every type supplied in the token if it is in the list
			// of defined types
			for (Object thisIdType : listIdTypes) {
				String thisIdTString = (String) thisIdType;
				checkIdType(thisIdTString);
			}
			// if we end up here, everything is okay
			return;
		} catch (ClassCastException e) {
			// If one of the casts went wrong, the format of the input JSON data
			// was probably incorrect.
			throw new InvalidTokenException(
					"Illegal format for data item 'idtypes': Must be array of Strings.");
		}
	}

	/**
	 * Check if an ID with the given type is configured.
	 *
	 * @param idType
	 *            The name of the ID type to check.
	 * @throws InvalidTokenException
	 *             If the given ID type is not known.
	 */
	private void checkIdType(String idType) throws InvalidTokenException {
		List<String> definedIdTypes = Arrays.asList(IDGeneratorFactory.instance
				.getIDTypes());
		if (!definedIdTypes.contains(idType))
			throw new InvalidTokenException("'" + idType + "'"
					+ " is not a known ID type!");
	}

	/**
	 * Get this token as JSON.
	 *
	 * @param apiVersion
	 *            The API version to use.
	 * @return A JSON representation of the token.
	 */
	public JSONObject toJSON(ApiVersion apiVersion) {
		JSONObject ret = new JSONObject();
		try {
			if (apiVersion.majorVersion >= 2) {
				ret.put("id", this.id).put("type", this.type);
				ObjectMapper mapper = new ObjectMapper();
				String dataString = mapper.writeValueAsString(data);
				ret.put("data", new JSONObject(dataString));
			} else {
				ret.put("tokenId", this.id);
			}
			ret.put("uri", this.getURI().toString());
			return ret;
		} catch (Exception e) {
			// As no external data is processed, this method should not fail.
			// If it does, this indicates a bug
			throw new Error(e);
		}
	}

    public String getParentSessionId() {
        return parentSessionId;
    }

    public void setParentSessionId(String parentSessionId) {
        this.parentSessionId = parentSessionId;
    }

    public String getParentServerName() {
        return parentServerName;
    }

    public void setParentServerName(String parentServerName) {
        this.parentServerName = parentServerName;
    }
}
