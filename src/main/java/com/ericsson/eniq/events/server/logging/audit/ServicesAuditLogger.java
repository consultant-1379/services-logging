/*
 * ---------------------------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * ---------------------------------------------------------------------------------------
 */

package com.ericsson.eniq.events.server.logging.audit;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
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
import javax.ws.rs.core.UriInfo;

import com.ericsson.eniq.events.server.common.ApplicationConfigConstants;

/**
 * Eniq Services Audit Logger class.
 * Uses the Java Util Logger as thats the one Glassfish uses.
 * Levels can be changes using the Glassfish Admin Console.
 *
 *
 * Default logging levels
 * INFO : intended for use in logging the URI request
 *
 * FINE : intended for use in logging the DB query called for a given URI request
 *
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.WRITE)
public class ServicesAuditLogger {

    protected int fileLimit = 10000;

    /**
     * Timestamp formatter for the output log file
     */
    private final DateFormat LOGFILE_TSTAMP_FORMATTER = new SimpleDateFormat("yyyy_MM_dd");

    /**
     * Current timestamp.
     */
    protected String timeStamp;

    /**
     * Services logger name, same as module name in Glassfish
     */
    public static final String SERVICES_AUDIT_LOGGER_NAME = "EniqEventsServicesAudit";

    /**
     * Services Logger Instance.
     */
    private final Logger SERVICES_AUDIT_LOGGER = Logger.getLogger(SERVICES_AUDIT_LOGGER_NAME);

    /**
     * Default name for services in the DEFAULT_LOG_DIR dir
     */
    private static final String SERVICESAUDIT = "servicesaudit";

    /**
     * Default base log directory
     */
    private final String DEFAULT_LOG_DIR = File.separator + "eniq" + File.separator + "log" + File.separator + "sw_log";

    /**
     * String used in building log messages.
     */
    private static final String DELIMITER = "|";

    /**
     * String used in building log messages.
     */
    private static final String NA = "<N/A>";

    /**
     * String used in building log messages.
     */
    private static final String EQUALS = "=";

    /**
     * String used in building log messages.
     */
    private static final String NULL = "<Null>";

    /**
     * String used in building log messages.
     */
    private static final String EMPTY = "<EMPTY>";

    private static final String ROLLOVER_LIMIT = "ENIQ_EVENTS_AUDITLOG_ROLLOVER";

    /**
     * Services file log handler
     */
    private Handler logFileHandler = null;

    private boolean redirectToStdout = false;

    @PostConstruct
    public void staticInit() {
        resetHandlers();
        redirectToStdout = Boolean.valueOf(System.getProperty(SERVICES_AUDIT_LOGGER_NAME + ".stdout", "false"));
        if (redirectToStdout) {
            final Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.FINE);
            SERVICES_AUDIT_LOGGER.addHandler(consoleHandler);
        }
    }

    @PreDestroy
    public void applicationDestroy() {
        for (final Handler handler : SERVICES_AUDIT_LOGGER.getHandlers()) {
            handler.close();
            SERVICES_AUDIT_LOGGER.removeHandler(handler);
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
                return Integer.valueOf(properties.getProperty(ROLLOVER_LIMIT,
                        (String.valueOf(ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_AUDITLOG_ROLLOVER))));
            } catch (final Exception e) {
                return ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_AUDITLOG_ROLLOVER;
            }
        }
        return ApplicationConfigConstants.DEFAULT_ENIQ_EVENTS_AUDITLOG_ROLLOVER;
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
        return SERVICES_AUDIT_LOGGER;
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
        for (final Handler handler : SERVICES_AUDIT_LOGGER.getHandlers()) {
            handler.close();
            SERVICES_AUDIT_LOGGER.removeHandler(handler);
        }

        // if logger is turned off, do not create directory, or add a new filehandler (this
        // would lead to a situation where the logger is off and yet is creating empty log files)
        if (Level.OFF.equals(SERVICES_AUDIT_LOGGER.getLevel())) {
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
            timeStamp = LOGFILE_TSTAMP_FORMATTER.format(date);
            final int rolloverLimit = lookupRolloverLimitPropertyInJNDI();

            logFileHandler = new FileHandler(dir + File.separator + SERVICESAUDIT + "-" + timeStamp + ".log",
                    rolloverLimit, fileLimit, false);

            logFileHandler.setFormatter(new ServicesAuditLogFormatter());
        } catch (final SecurityException e) {
            Logger.getLogger("").log(Level.SEVERE, "Failed to start Audit Logger" + e.getStackTrace());
        } catch (final IOException e) {
            Logger.getLogger("").log(Level.SEVERE, "Failed to start Audit Logger" + e.getStackTrace());
        }
        SERVICES_AUDIT_LOGGER.addHandler(logFileHandler);
    }

    /**
     * Get the current log level
     * @return Current logging level
     */
    @Lock(LockType.READ)
    public Level getLevel() {
        return SERVICES_AUDIT_LOGGER.getLevel();
    }

    /**
     * Set the current log level
     */
    public void setLevel(final Level logLevel) {
        SERVICES_AUDIT_LOGGER.setLevel(logLevel);
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
        final String baseLogDir = System.getProperty("LOG_DIR", DEFAULT_LOG_DIR);
        return baseLogDir + File.separator + SERVICESAUDIT;
    }

    /**
     * Log information. Should be used for information which will be useful in traces to evaluate what is going on
     * in services. Should be limited to major events so that the trace is not overburdened, e.g. at significant
     * branches in a program flow.
     *
     * @param logLevel The level to log at
     * @param info an array of information objects
     */
    public void detailed(final Level logLevel, final Object... info) {
        log(logLevel, info);
    }

    /**
     * Log the info
     * protected access modifier for testing purposes
     *
     * @param level       The level to log at
     * @param message     additional information to add to the trace.
     */
    protected void log(final Level level, final Object... message) {

        final Date date = new Date();
        final String dstamp = LOGFILE_TSTAMP_FORMATTER.format(date);

        if ((SERVICES_AUDIT_LOGGER.getHandlers().length == 0) && !(Level.OFF.equals(SERVICES_AUDIT_LOGGER.getLevel()))) {
            resetHandlers();
        }

        // handles filename rollover at midnight
        if (!dstamp.equals(timeStamp)) {
            resetHandlers();
        }

        final StringBuilder sb = new StringBuilder();
        if (isLevelActive(level)) {
            final String compiledInfoMessage = buildMessage(message);
            sb.append(compiledInfoMessage);
            SERVICES_AUDIT_LOGGER.log(level, sb.toString());
        }
    }

    /**
    * Add trace information to the supplied StringBuilder.
    *
    * @param info      additional information to add to the trace.
    * @return          the compiled log/trace String.
    */
    private String buildMessage(final Object... info) {
        final StringBuilder sb = new StringBuilder();
        for (final Object o : info) {
            final String objInfo = getObjectInfo(o);
            sb.append(objInfo);
            sb.append(DELIMITER);
        }
        return sb.toString().substring(0, sb.length() - 1); // trim the last DELIMITER
    }

    /**
     * Convert the Object to a String, handles Object[], Collection and single isntances.
     *
     * @param o The Object to convert.
     * @return String representation of the Object.
     */
    private String getObjectInfo(final Object o) {
        String s;
        if (o == null) {
            s = NULL;
        } else {
            try {
                if (o instanceof Object[]) {
                    s = getObjectArrayInfo((Object[]) o);
                } else if (o instanceof Collection) {
                    s = getCollectionInfo((Collection) o);
                } else if (o instanceof Map) {
                    s = getMapInfo((Map) o);
                } else if (o instanceof UriInfo) {
                    s = getUriInfo((UriInfo) o);
                } else {
                    s = o.toString().trim();
                }
            } catch (final Exception e) {
                s = NA;
            }
        }
        return s;
    }

    /**
     * Convert a map of information to a log-able String.
     *
     * @param info a map of information, mapping a String to a printable object.
     * @return a String which can be used in any of the logger methods.
     */
    private String getMapInfo(final Map info) {
        final StringBuilder sb = new StringBuilder();
        final Iterator keys = info.keySet().iterator();
        while (keys.hasNext()) {
            final Object key = keys.next();
            sb.append(key);
            sb.append(EQUALS);
            final Object value = info.get(key);
            sb.append((value == null ? NULL : value.toString()));
            if (keys.hasNext()) {
                sb.append(DELIMITER);
            }
        }
        return sb.toString();
    }

    /**
     * Get a string for the contents of an object array.
     *
     * @param objects the array of objects.
     * @return the contents as a String.
     */
    private String getObjectArrayInfo(final Object[] objects) {
        final StringBuilder innerString = new StringBuilder();
        if (objects.length == 0) {
            innerString.append(EMPTY);
        } else {
            for (int i = 0; i < objects.length; i++) {
                final Object innerObject = objects[i];
                if (innerObject == null) {
                    innerString.append(NULL);
                } else {
                    innerString.append(innerObject.toString().trim());
                }
                if (i < objects.length - 1) {
                    innerString.append(',');
                }
            }
        }
        return innerString.toString();
    }

    /**
     * Get a string for the contents of a Collection
     *
     * @param collection the array of objects.
     * @return the contents as a String.
     */
    private String getCollectionInfo(final Collection collection) {
        final StringBuilder sb = new StringBuilder("");
        final Iterator collectionIter = collection.iterator();
        while (collectionIter.hasNext()) {
            sb.append(collectionIter.next());
            if (collectionIter.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * @param uriInfo to be appended to the log entry
     * @return log text with UriInfo details
     */
    private String getUriInfo(final UriInfo uriInfo) {
        final StringBuilder sb = new StringBuilder();
        sb.append(uriInfo.getRequestUri());
        return sb.toString();
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
        return SERVICES_AUDIT_LOGGER.isLoggable(level);
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
