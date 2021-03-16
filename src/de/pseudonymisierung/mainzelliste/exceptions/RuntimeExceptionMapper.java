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
package de.pseudonymisierung.mainzelliste.exceptions;

import de.pseudonymisierung.mainzelliste.Config;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  private static final Logger logger = LogManager.getLogger(RuntimeExceptionMapper.class);

  @Override
  public Response toResponse(RuntimeException exception) {
    logger.fatal("Internal Error", exception);
    String currentTimeMillis = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        .format(System.currentTimeMillis());
    String errorMessage = "An internal error has occurred. Please notify your system administrator "
        + "with the following data:\n  - Timestamp: " + currentTimeMillis + "\n  - Version: "
        + Config.instance.getVersion();
    // return stacktrace if debug or trace level is activated
    if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
      StringWriter stringWriter = new StringWriter();
      exception.printStackTrace(new PrintWriter(stringWriter));
      errorMessage += "\n - Stacktrace: " + stringWriter.toString();
    }
    return Response
        .status(Status.INTERNAL_SERVER_ERROR)
        .entity(errorMessage)
        .build();
  }
}
