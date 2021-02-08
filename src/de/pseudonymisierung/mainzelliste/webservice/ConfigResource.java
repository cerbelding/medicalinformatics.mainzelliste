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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Servers;

/**
 * Resource for querying configuration parameters via the REST interface. This resource is for internal use in the OSSE
 * registry system (http://www.osse-register.de) and subject to change.
 */
@Path("/configuration")
public class ConfigResource {

	/**
	 * Get field keys as an array of strings.
	 *
	 * @param request
	 *            The injected HttpSerlvetRequest
	 *
	 * @return Field keys as an array of strings.
	 */
	@Path("/fieldKeys")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFieldKeys(@Context HttpServletRequest request) {
		Servers.instance.checkPermission(request, "readConfiguration");
		JSONArray ret = new JSONArray(Config.instance.getFieldKeys());
		return Response.ok(ret).build();
	}
}
