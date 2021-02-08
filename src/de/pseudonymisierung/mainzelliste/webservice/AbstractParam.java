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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Taken from https://github.com/codahale/dropwizard, file link:
 * https://github.com/codahale/dropwizard/blob/master/dropwizard-jersey/src/main/java/com/codahale/dropwizard/jersey/params/AbstractParam.java
 *
 * See http://codahale.com/what-makes-jersey-interesting-parameter-classes/ for information
 *
 * Originally released under the Apache License 2.0 (see project root).
 *
 */
@SuppressWarnings("javadoc")
public abstract class AbstractParam<V> {
	private final V value;
	private final String originalParam;

	public AbstractParam(String param) throws WebApplicationException {
		this.originalParam = param;
		try {
			this.value = parse(param);
		}
		catch (WebApplicationException e){
			throw e;
		}
		catch (Throwable e) {
			throw new WebApplicationException(onError(param, e));
		}
	}

	public V getValue() {
		return value;
	}

	public String getOriginalParam() {
		return originalParam;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	protected abstract V parse(String param) throws Throwable;

	protected Response onError(String param, Throwable e) {
		return Response.status(Status.BAD_REQUEST)
				.entity(getErrorMessage(param, e)).build();
	}

	protected String getErrorMessage(String param, Throwable e) {
		return "Invalid parameter: " + param + " (" + e.getMessage() + ")";
	}
}
