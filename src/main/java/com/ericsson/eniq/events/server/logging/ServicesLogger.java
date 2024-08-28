/*
 * ---------------------------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * ---------------------------------------------------------------------------------------
 */

package com.ericsson.eniq.events.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.UriInfo;

import com.ericsson.eniq.events.server.common.ApplicationConfigConstants;

/**
 * Eniq Services Logger class.
 * No other loggers should be used, if more logging types is needed, update and call this class.
 * Uses the Java Util Logger as thats the one Glassfish uses.
 * Levels can be changes using the Glassfish Admin Console.
 *
 * Enter / Exit methods are woven into the code at compile time using AspectJ, see TraceAspect.aj
 *
 * Default logging levels
 * INFO : Enter/Exit on API in resource classes only
 *          (API method is defined by methods having the annotations '@Path @GET @Produces' or just '@Path')
 *
 * FINE : Enter/Exit on all public methods only.
 *
 * FINER : Enter/Exit on all public, protected & private methods.
 *
 * FINEST : Same as FINER but also includes the ServicesLogger.detailed() logs
 *
 * The only tracing you should be including in the code is the detailed, error and maybe warning calls.
 * 
 * Uses custom handler @see ServicesLoggingHandler
 */
public class ServicesLogger {

    public static final String TRACE_MAX_MESSAGE_LENGTH = "trace.max_message_length";

    /**
     * Services logger name, same as module name in Glassfish
     */
    public static final String SERVICES_LOGGER_NAME = "EniqEventsServices";

    /**
     * Services Logger Instance.
     */
    private static final Logger SERVICES_LOGGER = Logger.getLogger(SERVICES_LOGGER_NAME);

    /**
     * String used in building log messages.
     */
    private static final String SEMICOLON = "; ";

    /**
     * String used in building log messages.
     */
    private static final String NEWLINE = "\n\t";

    /**
     * Maximum amount of characters in a single line sent to trace message.
     */
    private static final int MAX_LINE_LENGTH = 100;

    /**
     * Max size string to print if Level is not FINEST
     */
    private static int maxMessageLength = 512; // NOPMD : Can be set through system property 

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

    /**
     * String used in building log messages.
     */
    private static final String ENTER = "-->";

    /**
     * String used in building log messages.
     */
    private static final String EXIT = "<--";

    /**
     * String used in building log messages.
     */
    private static final String PARAMS = "Parameters: ";

    /**
     * String used in building log messages.
     */
    private static final String RETURNS = "Returning: ";

    /**
     * String used in building log messages.
     */
    private static final String INFO = "Info: ";

    private static final int MAX_MESSAGE_LENGTH_DEFAULT = 512;

    /**
     * Services file log handler
     */
    private static ServicesLoggingHandler logFileHandler = null;

    private static boolean redirectToStdout = false;

    static {
        initializePropertiesAndLoggers();
    }

    public static void initializePropertiesAndLoggers() {
        resetHandlers();
        redirectToStdout = Boolean.valueOf(System.getProperty(SERVICES_LOGGER_NAME + ".stdout", "false"));
        if (redirectToStdout) {
            final Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.FINEST);
            SERVICES_LOGGER.addHandler(consoleHandler);
        }
        setMaxMessageLengthProperty();
    }

    /**
     * Set the maxMessageLength field
     * This is sourced from (in this order):
     * 1) a system property (really intended for junit testing, as junit tests won't have access to JNDI)
     * 2) property in JNDI
     * 3) a default value
     */
    private static void setMaxMessageLengthProperty() {
        String maxMessageLengthProperty = System.getProperty(TRACE_MAX_MESSAGE_LENGTH);
        if (maxMessageLengthProperty == null) {
            maxMessageLengthProperty = lookupMaxMessageLengthPropertyInJNDI();
        }
        if (maxMessageLengthProperty == null) {
            maxMessageLength = MAX_MESSAGE_LENGTH_DEFAULT;
        } else {
            try {
                maxMessageLength = Integer.parseInt(maxMessageLengthProperty);
            } catch (final NumberFormatException e) {
                maxMessageLength = MAX_MESSAGE_LENGTH_DEFAULT;
            }
        }
    }

    private static String lookupMaxMessageLengthPropertyInJNDI() {
        String maxMessageLengthProperty = null;
        try {
            final Properties properties = (Properties) (new InitialContext())
                    .lookup(ApplicationConfigConstants.ENIQ_EVENT_PROPERTIES);
            maxMessageLengthProperty = (String) properties.get(TRACE_MAX_MESSAGE_LENGTH);
        } catch (final NamingException e) {
            //if we can't access the jndi store, we're in trouble anyhow, just keep going
        }
        return maxMessageLengthProperty;
    }

    /**
     * Private constructor
     */
    private ServicesLogger() {
    }

    /**
     * Used for testing purposes only.
     * @return
     */
    static Logger getRawLogger() {
        return SERVICES_LOGGER;
    }

    static void resetHandlers() {
        //Remove old handlers, if the app get redeployed the static{} initilizer will get called again.
        for (final Handler handler : SERVICES_LOGGER.getHandlers()) {
            SERVICES_LOGGER.removeHandler(handler);
        }
        logFileHandler = new ServicesLoggingHandler();
        SERVICES_LOGGER.addHandler(logFileHandler);
    }

    /**
     * Get the log output directory
     * Only used in tests
     * @return Log output dir
     */
    public static String getServicesLogDirectory() {
        return logFileHandler.getLogDirectory();
    }

    /**
     * Get the current log level
     * @return Current loggins level
     */
    public static Level getLevel() {
        return SERVICES_LOGGER.getLevel();
    }

    public static void setLevel(final Level logLevel) {
        SERVICES_LOGGER.setLevel(logLevel);
    }

    /**
     * Close any open log files.
     * Mainly used in tests.
     */
    public static void closeLogFiles() {
        if (logFileHandler != null) {
            logFileHandler.close();
        }
    }

    /**
     * Log the fact that a public method has been called
     * Should only be called for public methods which are exposed to applications.
     *
     * @param logLevel   The level to log the enter statement at
     * @param className  the name of the class or interface where the method was called
     * @param methodName the method name
     * @param parameters parameter information, as a list of strings. Up to the caller
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    // Used in TracaAspect.aj
    public static void enter(final Level logLevel, final String className, final String methodName,
            final Object... parameters) {
        log(logLevel, className, methodName + ENTER, PARAMS, parameters);
    }

    /**
     * Log the fact that a public method has ended.
     *
     * @param className  the name of the class or interface where the method was called
     * @param methodName the method name
     * @param parameters parameter information, as a list of strings. Up to the caller
     */
    public static void warn(final String className, final String methodName, final Object... parameters) {
        log(Level.WARNING, className, methodName, INFO, parameters);
    }

    /**
     * Log the fact that a public method has ended.
     *
     * @param logLevel     The level to log the exit statement at
     * @param className    the name of the class or interface where the method was called
     * @param methodName   the method name
     * @param returnResult The result being returned by the method
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    // Used in TracaAspect.aj
    public static void exit(final Level logLevel, final String className, final String methodName,
            final Object returnResult) {
        if (returnResult == null) {
            log(logLevel, className, methodName + EXIT, null);
        } else {
            log(logLevel, className, methodName + EXIT, RETURNS, returnResult);
        }
    }

    /**
     * Log that an error has occurred in services. Should only be used for exceptional cases. I.e. do not use this
     * for invalid data entered by the user.
     *
     * @param className  the name of the class or interface where the error occurred
     * @param methodName the method name where the error occurred
     * @param info       an array of information strings
     */
    public static void error(final String className, final String methodName, final Object... info) {
        log(Level.SEVERE, className, methodName, INFO, info);
    }

    /**
     * Log information. Should be used for information which will be useful in traces to evaluate what is going on
     * in services. Should be limited to major events so that the trace is not overburdened, e.g. at significant
     * branches in a program flow.
     *
     * @param className  the name of the class or interface where the info applies
     * @param methodName the method name where the information applies
     * @param info       an array of information strings
     */
    public static void info(final String className, final String methodName, final Object... info) {
        log(Level.INFO, className, methodName, INFO, info);
    }

    /**
     * Log information at FINEST level. Should be used for information which will be useful in traces to evaluate what is going on
     * in services. Should be limited to major events so that the trace is not overburdened, e.g. at significant
     * branches in a program flow.
     *
     * @param className  the name of the class or interface where the info applies
     * @param methodName the method name where the information applies
     * @param info       an array of information strings
     */
    public static void detailed(final String className, final String methodName, final Object... info) {
        detailed(Level.FINEST, className, methodName, info);
    }

    /**
     * Log information. Should be used for information which will be useful in traces to evaluate what is going on
     * in services. Should be limited to major events so that the trace is not overburdened, e.g. at significant
     * branches in a program flow.
     *
     * @param logLevel The level to log at
     * @param className the name of the class or interface where the info applies
     * @param methodName the method name where the information applies
     * @param info an array of information objects
     */
    public static void detailed(final Level logLevel, final String className, final String methodName,
            final Object... info) {
        log(logLevel, className, methodName, INFO, info);
    }

    /**
     * Log an exception which has been caught.
     *
     * @param className  the class where the exception was caught
     * @param methodName the method where the exception was caught
     * @param info       Information about the exception
     * @param th         the Throwable which was caught
     */
    public static void exception(final String className, final String methodName, final String info, final Throwable th) {
        final StringBuilder sb = new StringBuilder(info);
        sb.append(NEWLINE);
        final StringWriter stkString = new StringWriter();
        th.printStackTrace(new PrintWriter(stkString));
        sb.append(stkString);
        log(Level.SEVERE, className, methodName, sb.toString());
    }

    /**
     * Log the info
     *
     * @param level       The level to log at
     * @param className   the name of the class doing the tracing
     * @param methodName  the name of the method doing the tracing
     * @param infoMessage the main message
     * @param message     additional information to add to the trace.
     */
    private static void log(final Level level, final String className, final String methodName,
            final String infoMessage, final Object... message) {
        final StringBuilder sb = new StringBuilder();
        if (isLevelActive(level)) {
            sb.append(Thread.currentThread().getName());
            sb.append(SEMICOLON);
            sb.append("{").append(System.currentTimeMillis()).append("}");
            sb.append(SEMICOLON);
            sb.append(className);
            sb.append(SEMICOLON);
            sb.append(methodName);
            final String compiledInfoMessage = buildMessage(level, infoMessage, message);
            sb.append(compiledInfoMessage);
            SERVICES_LOGGER.log(level, sb.toString());
        }
    }

    /**
     * Add trace information to the supplied StringBuilder.
     *
     * @param level The level to log the message at
     * @param infoMessage the main message to log
     * @param info        additional information to add to the trace.
     * @return the compiled log/trace String.
     */
    private static String buildMessage(final Level level, final String infoMessage, final Object... info) {
        final StringBuilder sb = new StringBuilder();
        int lineLength = 0;
        if (infoMessage != null) {
            sb.append(NEWLINE);
            sb.append(infoMessage);
            lineLength = infoMessage.length();
        }
        for (final Object o : info) {
            if (lineLength >= MAX_LINE_LENGTH) {
                sb.append(NEWLINE);
                lineLength = 0;
            }
            String objInfo = getObjectInfo(o);
            if (Level.FINEST != level && objInfo.length() > maxMessageLength) {
                objInfo = objInfo.substring(0, maxMessageLength);
            }
            sb.append(objInfo);
            if (objInfo.startsWith(NEWLINE)) {
                lineLength = objInfo.length();
            } else {
                lineLength += objInfo.length();
            }
            sb.append(SEMICOLON);
            lineLength += 2;
        }
        return sb.toString();
    }

    /**
     * Convert the Object to a String, handles Object[], Collection and single isntances.
     *
     * @param o The Object to convert.
     * @return String representation of the Object.
     */
    private static String getObjectInfo(final Object o) {
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
                    s = o.getClass().getSimpleName() + "<" + o.toString().trim() + ">";
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
    private static String getMapInfo(final Map info) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Map[");
        final Iterator keys = info.keySet().iterator();
        while (keys.hasNext()) {
            final Object key = keys.next();
            sb.append(key);
            sb.append(EQUALS);
            final Object value = info.get(key);
            sb.append((value == null ? NULL : value.toString()));
            if (keys.hasNext()) {
                sb.append(SEMICOLON);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get a string for the contents of an object array.
     *
     * @param objects the array of objects.
     * @return the contents as a String.
     */
    private static String getObjectArrayInfo(final Object[] objects) {
        final StringBuilder innerString = new StringBuilder();
        innerString.append(objects.getClass().getSimpleName());
        if (objects.length == 0) {
            innerString.append(EMPTY);
        } else {
            innerString.append('[');
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
            innerString.append(']');
        }
        return innerString.toString();
    }

    /**
     * Get a string for the contents of a Collection
     *
     * @param collection the array of objects.
     * @return the contents as a String.
     */
    private static String getCollectionInfo(final Collection collection) {
        final StringBuilder sb = new StringBuilder("Collection[");
        final Iterator collectionIter = collection.iterator();
        while (collectionIter.hasNext()) {
            sb.append(collectionIter.next());
            if (collectionIter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getUriInfo(final UriInfo uriInfo) {
        final StringBuilder sb = new StringBuilder();
        sb.append("UriInfo[");
        sb.append(uriInfo.getRequestUri());
        sb.append("]");
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
    public static boolean isLevelActive(final Level level) {
        return SERVICES_LOGGER.isLoggable(level);
    }
}
