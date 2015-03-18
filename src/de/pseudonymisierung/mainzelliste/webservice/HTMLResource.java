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

import java.util.HashMap;
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.view.Viewable;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.dto.Persistor;

@Path("/html")
/**
 * HTML pages (rendered via JSP) to be accessed by a human
 * are served via this resource.
 */
public class HTMLResource {
	Logger logger = Logger.getLogger(HTMLResource.class);
	
	@GET
	@Path("createPatient")
	@Produces(MediaType.TEXT_HTML)
	public Response createPatientForm(
			@QueryParam("tokenId") String tokenId,
			@QueryParam("callback") String callback){
		Token t = Servers.instance.getTokenByTid(tokenId);
		if (Config.instance.debugIsOn() ||
				(t != null && t.getType().equals("addPatient")))
		{
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("tokenId", tokenId);
			map.put("callback", callback);
			map.put("adminPhone", Config.instance.getProperty("adminPhone"));
			return Response.ok(new Viewable("/createPatient.jsp", map)).build();
		} else throw new WebApplicationException(Response
				.status(Status.UNAUTHORIZED)
				.entity("Please supply a valid token id as URL parameter 'tokenId'.")
				.build());
	}
	
	@Path("/admin/editPatient")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response editPatientForm(
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

		Map <String, Object> map = new HashMap<String, Object>();
		map.putAll(p.getInputFields());
		map.put("id", patId.getIdString());
		map.put("tentative", p.getId("pid").isTentative());
		if (p.getOriginal() != p)
			map.put("original", p);

		return Response.ok(new Viewable("/editPatient.jsp", map)).build();
	}

	/** Submit form for editing a patient. */
	// Eigentlich wäre das PUT auf /pid/{pid}, aber PUT aus HTML-Formular geht nicht.
	@POST
	@Path("/admin/editPatient")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	public Response editPatient(
			@QueryParam("idType") String idType,
			@QueryParam("idString") String idString,
			MultivaluedMap<String, String> form,
			@Context HttpServletRequest req){
		
		logger.info(String.format("Handling edit operation for patient with id of type %s and value %s.",
					idType, idString));
		
		// TODO: Generalisieren für mehrere IDs
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

		for(String s: Config.instance.getFieldKeys()){
			if (!form.containsKey(s)) {
				logger.error("Field " + s + " not found in input data!");
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Field " + s + " not found in input data!").build());
			}
			chars.put(s, Field.build(s, form.getFirst(s)));
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
		// TODO: andere IDs, Checkbox dazu
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
		
//		return Response.ok("Patient edited successfully!").build();
		// TODO: Redirect auf Edit-Formular für diesen Patienten
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
}
