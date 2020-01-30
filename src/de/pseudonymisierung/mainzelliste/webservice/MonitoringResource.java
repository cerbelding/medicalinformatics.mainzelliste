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

import com.sun.jersey.spi.resource.Singleton;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.ValidatorException;
import de.pseudonymisierung.mainzelliste.service.MonitoringService;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.function.Supplier;

@Path("/monitoring")
@Singleton
public class MonitoringResource {
    private final MonitoringService service = new MonitoringService();

    @Path("metrics/IDRequestCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getIDRequestCount(@QueryParam("start") String startDateStr, @QueryParam("end") String endDateStr,
                                      @Context HttpServletRequest request) {
        return processGetMethod(request, "getIDRequestCount",
                () -> service.getIDRequestCount(startDateStr, endDateStr));
    }

    @Path("metrics/tentativePatientCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTentativePatientCount(@Context HttpServletRequest request) {
        return processGetMethod(request, "getTentativePatientCount", service::getTentativePatientCount);
    }

    @Path("metrics/patientCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPatientCount(@Context HttpServletRequest request) {
        return processGetMethod(request, "getPatientCount", service::getPatientCount);
    }

    /**
     * Returns the cpu usage for the whole system. between 0.0 and 1.0
     * @param request request information for HTTP servlet
     * @return amount of memory in bytes
     */
    @Path("status/cpuInfo")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCpuInfo(@Context HttpServletRequest request) {
        return processGetMethod(request, "getCpuInfo", service::getCpuInfo);
    }

    /**
     * Return the amount of used memory usage in bytes
     * @param request request information for HTTP servlet
     * @return amount of memory in bytes
     */
    @Path("status/memoryInfo")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMemoryInfo(@Context HttpServletRequest request) {
        return processGetMethod(request, "getMemoryInfo", service::getMemoryInfo);
    }

    private Response processGetMethod(HttpServletRequest request, String permission, Supplier<String> serviceFunction) {
        Servers.instance.checkPermission(request, permission);
        try {
            return Response.ok().entity(serviceFunction.get()).build();
        } catch (PersistenceException e) {
            throw new InternalErrorException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ValidatorException(e.getMessage());
        }
    }
}
