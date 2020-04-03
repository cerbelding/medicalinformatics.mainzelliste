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
package de.pseudonymisierung.mainzelliste.service;

import com.sun.management.OperatingSystemMXBean;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import org.apache.log4j.Logger;

import javax.persistence.PersistenceException;
import java.lang.management.ManagementFactory;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class MonitoringService {
    private Logger logger = Logger.getLogger(this.getClass());

    public String getIDRequestCount(String startDateStr, String endDateStr) {
        try {
            return Persistor.instance.getIDRequestCount(parseDate(startDateStr), parseDate(endDateStr)) + "";
        } catch (IllegalArgumentException e) {
            logger.warn("Couldn't process request because of invalid input format. Message: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get IDRequestCount. Cause: " +  e.getMessage());
            throw new PersistenceException("An internal error occured: Please contact the administrator.", e);
        }
    }

    private Date parseDate(String dateAsString) {
        try {
            return Date.valueOf(LocalDate.parse(dateAsString));
        } catch (DateTimeParseException exc) {
            throw new IllegalArgumentException("invalid format of the given date parameter : " + dateAsString + ". Please make sure to enter dates in the format YYYY-MM-DD", exc);
        } catch (NullPointerException exc) {
            return null;
        }
    }

    public String getTentativePatientCount() {
        try {
            return Persistor.instance.getTentativePatientCount() + "";
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get patientCount. Cause: " +  e.getMessage());
            throw new PersistenceException("An internal error occured: Please contact the administrator.", e);
        }
    }

    public String getPatientCount() {
        try {
            return Persistor.instance.getPatientCount() + "";
        } catch (RuntimeException e) {
            logger.fatal( "Persistence provider error. Can't get patientCount. Cause: " +  e.getMessage());
            throw new PersistenceException("An internal error occured: Please contact the administrator.", e);
        }
    }

    public String getCpuInfo() {
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        return operatingSystemMXBean.getSystemCpuLoad() + "";
    }

    public String getMemoryInfo() {
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        return (operatingSystemMXBean.getTotalPhysicalMemorySize() - operatingSystemMXBean.getFreePhysicalMemorySize()) + "";
    }
}
