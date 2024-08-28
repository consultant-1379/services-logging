/**
 * ---------------------------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * ---------------------------------------------------------------------------------------
 */

package com.ericsson.eniq.events.server.logging.performance;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ericsson.eniq.events.server.common.ApplicationConfigConstants;

/**
 * Eniq Services performance trace Logger class.
 * Uses the Java Util Logger as thats the one Glassfish uses.
 * Levels can be changes using the Glassfish Admin Console.
 *
 *
 * Default logging levels
 * OFF : No performance data will be logged
 *
 * INFO,FINE : performance data will be logged
 *
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.WRITE)
public class ServicePerformanceTraceLogger {

    protected int fileLimit = 10000;

    /**
     * Timestamp formatter for the output log file
     */
    private final DateFormat logfileTimeStampFormatter = new SimpleDateFormat("yyyy_MM_dd");

    /**
     * Current timestamp.
     */
    protected String timeStamp;

    /**
     * Services logger name, same as module name in Glassfish
     */
    public static final String SERVICES_PERFORMANCE_TRACE_LOGGER_NAME = "EniqEventsServicesPerformanceTrace";

    /**
     * Services Logger Instance.
     */
    private final Logger servicesPerformanceTraceLogger = Logger.getLogger(SERVICES_PERFORMANCE_TRACE_LOGGER_NAME);

    /**
     * Default name for services in the DEFAULT_LOG_DIR dir
     */
    private static final String SERVICES_PERFORMANCE_TRACE_DIR = "servicesperformancetrace";

    /**
     * Default base log directory
     */
    private final String defaultLogDir = File.separator + "eniq" + File.separator + "log" + File.separator + "sw_log";

    /**
     * String used in building log messages.
     */
    private static final String DELIMITER = "|";

    private static final String ROLLOVER_LIMIT = "ENIQ_EVENTS_PERFORMANCE_TRACE_ROLLOVER_IN_MB";

    private static final int BYTES_IN_MB = 1048576;

    /**
     * Services file log handler
     */
    private Handler logFileHandler = null;

    private boolean redirectToStdout = false;

    @PostConstruct
    public void init() {
        resetHandlers();
        redirectToStdout = Boolean.valueOf(System.getProperty(SERVICES_PERFORMANCE_TRACE_LOGGER_NAME + ".stdout",
                "false"));
        if (redirectToStdout) {
            final Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.FINE);
            servicesPerformanceTraceLogger.addHandler(consoleHandler);
        }
    }

    @PreDestroy
    public void applicationDestroy() {
        for (final Handler handler : servicesPerformanceTraceLogger.getHandlers()) {
            handler.close();
            servicesPerformanceTraceLogger.removeHandler(handler);
        }
    }

    /**
     * The method returns the log file rollover limit stored in glassfish / JNDI if available,
     * or hard-coded default if not available
     * @return Rollover limit in Bytes
     */
    private int lookupRolloverLimitPropertyInJNDI() {
        final Properties properties = lookupJNDIProperties();
        if (properties != null) {
            try {
                return Integer.valueOf(properties.getProperty(ROLLOVER_LIMIT, (String
                        .valueOf(ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_PERFORMANCE_TRACE_ROLLOVER_IN_MB))));
            } catch (final Exception e) {
                return ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_PERFORMANCE_TRACE_ROLLOVER_IN_MB;
            }
        }
        return ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_PERFORMANCE_TRACE_ROLLOVER_IN_MB;
    }

    /**
     * The method the custom JNDI properties stored glassfish
     * @return Properties object containing values from glassfish
     */
    private Properties lookupJNDIProperties() {
        try {

            final Properties properties = (Properties) (new InitialContext())
                    .lookup(ApplicationConfigConstants.ENIQ_EVENT_PROPERTIES);
            return properties;

        } catch (final NamingException e) {
            //if we can't access the jndi store, we're in trouble anyhow, just keep going
        }
        return null;
    }

    /**
     * Used for testing purposes only.
     * @return direct reference to logger
     */
    Logger getRawLogger() {
        return servicesPerformanceTraceLogger;
    }

    /**
     * Used for testing purposes only.
     *
     */
    void setFileLimit(final int newFileLimit) {
        fileLimit = newFileLimit;
    }

    /**
     * This method is called from the static block at (re-)deployment time to manage the log filehandlers,
     * including log directory creation (if necessary) and log file rollover
     */
    void resetHandlers() {

        //Remove old handlers, if the app gets redeployed the static{} initialiser will get called again.
        for (final Handler handler : servicesPerformanceTraceLogger.getHandlers()) {
            handler.close();
            servicesPerformanceTraceLogger.removeHandler(handler);
        }

        // if logger is turned off, do not create directory, or add a new filehandler (this
        // would lead to a situation where the logger is off and yet is creating empty log files)
        if (Level.OFF.equals(servicesPerformanceTraceLogger.getLevel())) {
            return;
        }

        try {
            final File dir = new File(getServicesLogDirectory());
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Logger.getLogger("").log(Level.SEVERE, "Failed to create the directory tree " + dir);
                }
            }
            final Date date = new Date();
            timeStamp = logfileTimeStampFormatter.format(date);
            final int rolloverLimit = lookupRolloverLimitPropertyInJNDI() * BYTES_IN_MB;

            logFileHandler = new FileHandler(dir + File.separator + SERVICES_PERFORMANCE_TRACE_DIR + "-" + timeStamp
                    + ".log", rolloverLimit, fileLimit, false);

            logFileHandler.setFormatter(new ServicesPerformanceTraceLogFormatter());
        } catch (final SecurityException e) {
            Logger.getLogger("").log(Level.SEVERE, "Failed to start Performance Trace Logger" + e.getStackTrace());
        } catch (final IOException e) {
            Logger.getLogger("").log(Level.SEVERE, "Failed to start Performance Trace Logger" + e.getStackTrace());
        }
        servicesPerformanceTraceLogger.addHandler(logFileHandler);
    }

    /**
     * Get the current log level
     * @return Current logging level
     */
    @Lock(LockType.READ)
    public Level getLevel() {
        return servicesPerformanceTraceLogger.getLevel();
    }

    /**
     * Set the current log level
     */
    public void setLevel(final Level logLevel) {
        servicesPerformanceTraceLogger.setLevel(logLevel);
    }

    /**
     * Close any open log files.
     * Mainly used in tests.
     */
    public void closeLogFiles() {
        if (logFileHandler != null) {
            logFileHandler.close();
            logFileHandler = null;
        }
    }

    /**
     * Get the log output directory
     * @return Log output dir
     */
    @Lock(LockType.READ)
    public String getServicesLogDirectory() {
        final String baseLogDir = System.getProperty("LOG_DIR", defaultLogDir);
        return baseLogDir + File.separator + SERVICES_PERFORMANCE_TRACE_DIR;
    }

    /**
     * Log information. Should be used for information which will be useful in traces to evaluate performance
     * in services. 
     *
     * @param logLevel The level to log at
     * @param servicesContext an context object having performance information
     */
    public void detailed(final Level logLevel, final ServicePeformanceContextInformation servicesContext) {
        log(logLevel, servicesContext);
    }

    /**
     * Log the info
     * protected access modifier for testing purposes
     *
     * @param level       The level to log at
     * @param message     additional information to add to the trace.
     */
    protected void log(final Level level, final ServicePeformanceContextInformation servicesContext) {

        final Date date = new Date();
        final String dstamp = logfileTimeStampFormatter.format(date);

        if ((servicesPerformanceTraceLogger.getHandlers().length == 0)
                && !(Level.OFF.equals(servicesPerformanceTraceLogger.getLevel()))) {
            resetHandlers();
        }

        // handles filename rollover at midnight
        if (!dstamp.equals(timeStamp)) {
            resetHandlers();
        }

        if (isLevelActive(level)) {
            servicesPerformanceTraceLogger.log(level, servicesContext.getContextInformation(DELIMITER));
        }
    }

    /**
     * Determine if the supplied log level is active.
     * <p/>
     * Should be used prior to costly string construction, but otherwise is
     * not needed as the underlying log functionality will not process any data
     * un-necessarily.
     *
     * @param level the level to check if logging is active for
     * @return a boolean indication is the logging is active
     */
    @Lock(LockType.READ)
    public boolean isLevelActive(final Level level) {
        return servicesPerformanceTraceLogger.isLoggable(level);
    }

    /**
     * getter for timeStamp - used for testing purposes only
     * @return current timeStamp value
     */
    String getTimeStamp() {
        return timeStamp;
    }

    /**
     * setter for timeStamp - used for testing purposes only
     * @param newTimeStamp new value for timeStamp
     */
    void setTimeStamp(final String newTimeStamp) {
        timeStamp = newTimeStamp;

    }
}
