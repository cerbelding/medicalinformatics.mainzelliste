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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.spi.resource.Singleton;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.view.Viewable;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.Initializer;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import java.io.IOException;
import java.net.URL;

/**
 * HTML pages (rendered via JSP) to be accessed by a human user
 * are served via this resource.
 */
@Path("/html")
@Singleton
public class HTMLResource {

	/** The logging instance. */
	Logger logger = Logger.getLogger(HTMLResource.class);

	/**
	 * Get the form for entering a new patient.
	 *
	 * @param tokenId
	 *            Id of a valid "addPatient" token.
	 * @param request
	 *            The injected HttpServletRequest.
	 * @return The input form or an error message if the given token is not valid.
	 */
	@GET
	@Path("createPatient")
	@Produces(MediaType.TEXT_HTML)
	public Response createPatientForm(
			@QueryParam("tokenId") String tokenId,
			@Context HttpServletRequest request) {
		String mainzellisteApiVersion = Servers.instance.getRequestApiVersion(request).toString();
		Token t = Servers.instance.getTokenByTid(tokenId);
		if (Config.instance.debugIsOn() ||
				(t != null && t.getType().equals("addPatient")))
		{
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("tokenId", tokenId);
			map.put("mainzellisteApiVersion", mainzellisteApiVersion);
			return Response.ok(new Viewable("/createPatient.jsp", map)).build();
		} else throw new WebApplicationException(Response
				.status(Status.UNAUTHORIZED)
				.entity("Please supply a valid token id as URL parameter 'tokenId'.")
				.build());
	}

	/**
	 * Get the form for changing an existing patient's IDAT.
	 *
	 * @param tokenId
	 *            Id of a valid "editPatient" token.
	 * @return The edit form or an error message if the given token is not
	 *         valid.
	 */
	@Path("editPatient")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response editPatient(@QueryParam("tokenId") String tokenId) {
		Servers.instance.checkToken(tokenId, "editPatient");
		EditPatientToken t = (EditPatientToken) Servers.instance.getTokenByTid(tokenId);
		Patient p = Persistor.instance.getPatient(t.getPatientId());
		Map <String, Object> map = new HashMap<String, Object>();
		map.put("tokenId", tokenId);
		map.putAll(p.getInputFields());

		return Response.ok(new Viewable("/editPatient.jsp", map)).build();
	}

	/**
	 * Get the administrator form for editing an existing patient's IDAT. The
	 * arguments can be omitted, in which case an input form is shown where an
	 * ID of the patient to edit can be input. Authentication is handled by the
	 * servlet container as defined in web.xml.
	 *
	 * @param idType
	 *            Type of the ID of the patient to edit.
	 * @param idString
	 *            ID string of the patient to edit.
	 * @return The edit form or a selection form if one of idType and idString is not provided.
	 */
	@Path("/admin/editPatient")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response editPatientAdmin(
			@QueryParam("idType") String idType,
			@QueryParam("idString") String idString
			) {
		// Authentication by Tomcat

		if (StringUtils.isEmpty(idType) || StringUtils.isEmpty(idString))
			return Response.ok(new Viewable("/selectPatient.jsp")).build();

		ID patId = IDGeneratorFactory.instance.buildId(idType, idString);
		Patient p = Persistor.instance.getPatient(patId);
		
		if (p == null)
			throw new WebApplicationException(Response
					.status(Status.NOT_FOUND)
					.entity(String.format("No patient found with ID of type %s and value %s!",
							idType, idString))
					.build());

		List<Patient> duplicates = Persistor.instance.getDuplicates(patId);
		LinkedList<String> duplicateIds = new LinkedList<String>();
		for (Patient thisPatient : duplicates) {
			duplicateIds.add(thisPatient.getId(IDGeneratorFactory.instance.getDefaultIDType()).getIdString());
		}

		List<Patient> possibleDuplicates = Persistor.instance.getPossibleDuplicates(patId);
		LinkedList<String> possibleDupIds = new LinkedList<String>();
		for (Patient thisPatient : possibleDuplicates) {
			possibleDupIds.add(thisPatient.getId(IDGeneratorFactory.instance.getDefaultIDType()).getIdString());
		}
		
		
		Map <String, Object> map = new HashMap<String, Object>();
		map.putAll(p.getInputFields());
		map.put("id", patId.getIdString());
		map.put("tokenId", "abc");
		map.put("tentative", p.getId(IDGeneratorFactory.instance.getDefaultIDType()).isTentative());
		if (p.getOriginal() != p)
			map.put("original", p.getOriginal());
		
		map.put("duplicates", duplicateIds);
		map.put("possibleDuplicates", possibleDupIds);

		return Response.ok(new Viewable("/editPatientAdmin.jsp", map)).build();
	}

	/**
	 * Receives edit operations from the admin form.
	 *
	 * @param idType
	 *            Type of the ID of the patient to edit.
	 * @param idString
	 *            ID string of the patient to edit.
	 * @param form
	 *            IDAT as provided by the input form.
	 * @param req
	 *            The injected HttpServletRequest.
	 * @return The edit form for the changed patient or the patient selection
	 *         form if the patient was deleted via the form.
	 *
	 */
	@POST
	@Path("/admin/editPatient")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	public synchronized Response editPatient(
			@QueryParam("idType") String idType,
			@QueryParam("idString") String idString,
			MultivaluedMap<String, String> form,
			@Context HttpServletRequest req){

		if (form.containsKey("delete")) {
			logger.info(String.format("Handling delete operation for patient with id of type %s and value %s.",
					idType, idString));
			Persistor.instance.deletePatient(IDGeneratorFactory.instance.buildId(idType, idString));
			return Response.status(Status.SEE_OTHER)
					.location(UriBuilder.fromResource(this.getClass()).path("admin/editPatient").build())
					.build();
		}

		logger.info(String.format("Handling edit operation for patient with id of type %s and value %s.",
					idType, idString));

		ID idPatToEdit = IDGeneratorFactory.instance.buildId(idType, idString);
		Patient pToEdit = Persistor.instance.getPatient(idPatToEdit);
		if (pToEdit == null)
		{
			logger.info(String.format("Request to edit patient with unknown ID of type %s and value %s.",
					idType, idString));
			throw new WebApplicationException(Response
					.status(Status.NOT_FOUND)
					.entity(String.format("No patient found with ID of type %s and value %s!",
							idType, idString))
					.build());
		}

		// read input fields from form
		Patient pInput = new Patient();
		Map<String, Field<?>> chars = new HashMap<String, Field<?>>();

		for(String fieldName : Config.instance.getFieldKeys()){
			// If a field is not in the map, keep the old value
			if (!form.containsKey(fieldName))
				chars.put(fieldName, pToEdit.getInputFields().get(fieldName));
			else {
				chars.put(fieldName, Field.build(fieldName, form.getFirst(fieldName)));
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

		// assign tentative status
		pToEdit.setTentative(form.getFirst("tentative") != null);
		// assign original
		String idStringOriginal = form.getFirst("idStringOriginal");
		String idTypeOriginal = form.getFirst("idTypeOriginal");
		if (!StringUtils.isEmpty(idStringOriginal) && ! StringUtils.isEmpty(idTypeOriginal))
		{
			ID originalId = IDGeneratorFactory.instance.buildId(idTypeOriginal, idStringOriginal);
			Patient pOriginal = Persistor.instance.getPatient(originalId);
			pToEdit.setOriginal(pOriginal);
		} else
		{
			pToEdit.setOriginal(pToEdit);
		}


		Persistor.instance.updatePatient(pToEdit);

		return Response
				.status(Status.SEE_OTHER)
				.header("Cache-control", "must-revalidate")
				.location(UriBuilder
						.fromUri(req.getRequestURL().toString())
						.path("")
						.queryParam("idType", idType)
						.queryParam("idString", idString)
						.build())
						.build();
	}

	/**
	 * Returns the logo file from the configured path (configuration parameter operator.logo).
	 *
	 * @return A "200 Ok" response containing the file, or an appropriate error code and message on failure.
	 */
	@GET
	@Path("/logo")
	@Produces("image/*")
	public Response getLogo() {
		try {
			URL logoURL = Config.instance.getLogo();
			// getPath() is sufficient since getMimeType() is actually checking the file's extension only
			String contentType = Initializer.getServletContext().getMimeType(logoURL.getPath().toLowerCase());
			if (contentType == null || !contentType.startsWith("image/")) {
				logger.error("Logo file has incorrect mime type: " + contentType);
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity("The logo file has incorrect mime type. See server log for details.").build());
			}
			return Response.ok().type(contentType).entity(logoURL.openStream()).build();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("Logo file could not be opened. Check server log for more information.").build());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("Logo file could not be opened. Check server log for more information.").build());
		}
	}
}
