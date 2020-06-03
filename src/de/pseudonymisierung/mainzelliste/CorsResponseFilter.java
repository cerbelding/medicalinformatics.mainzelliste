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
package de.pseudonymisierung.mainzelliste;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Adds header "Access-Control-Allow-Origin" for Cross-origin resource sharing
 *
 * If an incoming request includes the header "Origin", the value of it is
 * checked against a list of configured hosts (see {@link Config#originAllowed(String)}).
 * If the host is listed as an allowed origin, the header "Access-Control-Allow-Origin"
 * in the response is set to this value.
 *
 */
public class CorsResponseFilter implements Filter {

	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Not used in this implementation.
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {}

	/**
	 * Not used in this implementation.
	 */
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;

			String origin = httpRequest.getHeader("Origin");
			String thisHostAndScheme = httpRequest.getScheme() + "://" + httpRequest.getHeader("Host");
			if (origin != null) {
				if (origin.equals(thisHostAndScheme) || Config.instance.originAllowed(origin)) {
					logger.debug("Allowing cross domain request from origin " + origin);
					httpResponse.addHeader("Access-Control-Allow-Origin", origin);
				} else {
					logger.info("Rejecting cross domain request from origin " + origin);
					// For illegal origin, cancel request with 403 Forbidden.
					HttpServletResponse resp = (HttpServletResponse) response;
					resp.setStatus(403);
					resp.getWriter().println("Rejecting cross domain request from origin " + origin);
					return;
				}
			}
		}
		chain.doFilter(request, response);
	}
}
