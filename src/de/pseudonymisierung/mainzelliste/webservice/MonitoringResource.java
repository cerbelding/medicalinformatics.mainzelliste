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
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.ValidatorException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.time.DateTimeException;
import java.time.LocalDate;

@Path("/monitoring")
@Singleton
public class MonitoringResource {
    /** The logging instance. */
    Logger logger = Logger.getLogger(this.getClass());

    @Path("IDRequestCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getIDRequestCount(@QueryParam("start") String startDateStr, @QueryParam("end") String endDateStr,
                                      @Context HttpServletRequest request) {
        Servers.instance.checkPermission(request, "getIDRequestCount");

        // validate start date
        Date startDate;
        try {
            startDate = parseDate(startDateStr);
        } catch ( IllegalArgumentException exc) {
            throw new ValidatorException(startDateStr + " is not a valid start date");
        }

        // validate end date
        Date endDate;
        try {
            endDate = parseDate(endDateStr);
        } catch ( IllegalArgumentException exc) {
            throw new ValidatorException(endDateStr + " is not a valid end date");
        }

        try {
            long count = Persistor.instance.getIDRequestCount(startDate, endDate);
            return Response.ok().entity(count+"").build();
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get IDRequestCount. Cause: " +  e.getMessage());
            throw new InternalErrorException("An internal error occured: Please contact the administrator.");
        }
    }

    @Path("tentativePatientCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTentativePatientCount(@Context HttpServletRequest request) {
        Servers.instance.checkPermission(request, "getTentativePatientCount");
        try {
            long count = Persistor.instance.getTentativePatientCount();
            return Response.ok().entity(count+"").build();
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get patientCount. Cause: " +  e.getMessage());
            throw new InternalErrorException("An internal error occured: Please contact the administrator.");
        }
    }

    @Path("patientCount")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPatientCount(@Context HttpServletRequest request) {
        Servers.instance.checkPermission(request, "getPatientCount");
        try {
            long count = Persistor.instance.getPatientCount();
            return Response.ok().entity(count+"").build();
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get patientCount. Cause: " +  e.getMessage());
            throw new InternalErrorException("An internal error occured: Please contact the administrator.");
        }
    }

    private Date parseDate(String dateAsString) {
        try {
            return Date.valueOf(LocalDate.parse(dateAsString));
        } catch (DateTimeException exc) {
            throw new IllegalArgumentException(exc);
        } catch (NullPointerException exc) {
            return null;
        }
    }
}
