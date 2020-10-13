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
package de.pseudonymisierung.mainzelliste;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.sun.jersey.spi.container.servlet.WebComponent;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Context listener.
 * This class is responsible for setting up all singletons in the right order
 * and to fail early if anything goes wrong.
 * It is also responsible for calling various shutdown methods on context
 * shutdown, to stop background threads and remove objects in order to prevent
 * memory leaks etc.
 */
public class Initializer implements ServletContextListener {

	/** The injected ServletContext. */
	private static ServletContext context;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		context = sce.getServletContext();
		initialize();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		shutdown();
	}

	/**
	 * Initialization method. Called automativally by the servlet container.
	 * Initializes the following components:
	 * <ul>
	 * <li>Configuration (see {@link Config}.
	 * <li>Logging as defined in the configuration.
	 * <li>Persistency (see {@link Persistor}).
	 * <li>ID generators (see {@link IDGeneratorFactory}).
	 * <li>Server settings (see {@link Servers}.
	 * <li>Form validator (see {@link Validator}).
	 * </ul>
	 */
	@SuppressWarnings("unused")
	private void initialize() {
		Logger logger = LogManager.getLogger(Initializer.class);
		logger.info("#####Initializing...");

		// Output effective config to logfile.
		Enumeration<String> en = context.getInitParameterNames();
		while (en.hasMoreElements()) {
			String paramName = en.nextElement();
			logger.debug("Init param " + paramName + "="
					+ context.getInitParameter(paramName));
		}

		Config c = Config.instance;
		log4j2Setup();
		Persistor p = Persistor.instance;
		IDGeneratorFactory idgf = IDGeneratorFactory.instance;
		Servers s = Servers.instance;
		Validator v = Validator.instance;

		Config.instance.getBlockingKeyExtractors().updateBlockingKeyExtractors();
		/* 
		 * Limit Jersey logging to avoid spamming the log with "the request body has been consumed" messages
		 * (see http://stackoverflow.com/questions/2011895/how-to-fix-jersey-post-request-parameters-warning).
		 * This applies to use cases where all fields are transmitted via the "addPatient" token and the 
		 * POST /patients request is intentionally empty. 
		 */
		java.util.logging.Logger webComponentLogger = java.util.logging.Logger.getLogger(WebComponent.class.getName());
		webComponentLogger.setLevel(Level.SEVERE);
		logger.info("#####Startup succeeded. Ready to take requests.");
	}

	/**
	 * Shutdown method. Gets called when this web application is about to shutdown. It calls the showdown methods of the
	 * Persistor- and the Server-class. This is mainly used to release resources that would not be released
	 * automatically leading to memory leaks on context shutdowns.
	 */
	private void shutdown() {
		Logger logger = LogManager.getLogger(Initializer.class);
		logger.info("#####Shutting down...");

		// sut down persistor
		Persistor.instance.shutdown();

		// shut down server manager
		Servers.instance.shutdown();

		logger.info("#####Shut down complete.");
	}


	private void log4j2Setup () {
		Logger logger = LogManager.getLogger(Initializer.class);
		String logFileName = Config.instance.getProperty("log.filename");
		if (logFileName == null) {
			logger.info("Using default logging output.");
		} else {
			Path filePath = Paths.get(logFileName);
			String fileDir = filePath.getParent().toString();
			String fileName = filePath.getFileName().toString();
			logger.info(fileDir+ "," + fileName);
			LoggerContext lc = (LoggerContext) LogManager.getContext(false);
			Configuration config  = lc.getConfiguration();
			PatternLayout patternLayout = PatternLayout.newBuilder().withPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%t] %logger{36} - %msg%n").build();
			TimeBasedTriggeringPolicy timebasedPolicy =  TimeBasedTriggeringPolicy.newBuilder().build();

			RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder().withFileName(logFileName)
					.withFilePattern(fileDir+ "/%d{yyyy-MM}/%d{yyyy-MM-dd}-"+fileName)
					.setName("MainzellisteFileAppender")
					.setLayout(patternLayout)
					.setConfiguration(config)
					.withPolicy(timebasedPolicy)
					.build();

			rollingFileAppender.start();

			if (!Config.instance.debugIsOn()) {
				logger.warn("Redirecting mainzelliste log to " + logFileName + ".");
				config.getLoggers().forEach((loggerKey, loggerValue) -> loggerValue.getAppenders()
						.forEach((appenderKey, appenderValue) -> loggerValue.removeAppender(appenderValue.getName())));

			}
			lc.getConfiguration().addAppender(rollingFileAppender);
			lc.getRootLogger().addAppender(lc.getConfiguration().getAppender(rollingFileAppender.getName()));
			lc.updateLoggers();
			logger.info("Logger setup to log on level "
					+ Config.instance.getLogLevel() + " to " + logFileName);

		}
	}


	/**
	 * Gets the injected ServletContext.
	 * @return The injected ServletContext.
	 */
	public static ServletContext getServletContext() {
		return context;
	}
}
