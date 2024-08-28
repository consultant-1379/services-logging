/*
 * ---------------------------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * ---------------------------------------------------------------------------------------
 */

package com.ericsson.eniq.events.server.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Services Logger File Handler
 * Logs to file in /eniq/log/sw_log/services by default
 * 
 */
public class ServicesLoggingHandler extends Handler {
    /**
     * Timestamp formatter for the output log file
     */
    private static final DateFormat LOGFILE_TSTAMP_FORMATTER = new SimpleDateFormat("yyyy_MM_dd");
    /**
     * Default nbame for services in the DEFAULT_LOG_DIR dir 
     */
    private static final String SERVICES = "services";
    /**
     * Default base log directory
     */
    private static final String DEFAULT_LOG_DIR = File.separator+"eniq"+File.separator+"log"+File.separator+"sw_log";
    /**
     * File Writer
     */
    private BufferedWriter logWriter = null;
    /**
     * Current timestamp.
     */
    private String timeStamp;

    // Backup logger is something in here fails.
    private static final Logger BACKUP_LOGGER = Logger.getLogger("");

    /**
     * Constructor
     */
    public ServicesLoggingHandler() {
        setFormatter(new ServicesLogFormatter());
    }

    /**
     * Get the output log directory.
     * Done like this so the CI env test run can change the location from the default.
     * @return The dir to write the logs too
     */
    String getLogDirectory() {
        final String baseLogDir = System.getProperty("LOG_DIR", DEFAULT_LOG_DIR);
        return baseLogDir + File.separator + SERVICES;
    }
    /**
     * Flush any buffered output.
     */
    @Override
    public synchronized void flush() {
        if(logWriter != null){
            try {
                logWriter.flush();
            } catch (IOException e) {
                BACKUP_LOGGER.log(Level.WARNING, "flush failed", e);
            }
        }
    }

    /**
     * Close the <tt>Handler</tt> and free all associated resources.
     * <p/>
     * The close method will perform a <tt>flush</tt> and then close the
     * <tt>Handler</tt>.   After close has been called this <tt>Handler</tt>
     * should no longer be used.  Method calls may either be silently
     * ignored or may throw runtime exceptions.
     *
     * @throws SecurityException if a security manager exists and if
     *                           the caller does not have <tt>LoggingPermission("control")</tt>.
     */
    @Override
    public synchronized void close() {
        if(logWriter != null){
            try {
                logWriter.close();
            } catch (IOException e) {
                BACKUP_LOGGER.log(Level.WARNING, "close failed", e);
            }
            logWriter = null;
        }
    }

    /**
     * Publish a <tt>LogRecord</tt>.
     * <p/>
     * The logging request was made initially to a <tt>Logger</tt> object,
     * which initialized the <tt>LogRecord</tt> and forwarded it here.
     * <p/>
     * The <tt>Handler</tt>  is responsible for formatting the message, when and
     * if necessary.  The formatting should include localization.
     *
     * @param record description of the log event. A null record is
     *               silently ignored and is not published
     */
    @Override
    public synchronized void publish(final LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        try {
            final Date date = new Date(record.getMillis());
            final String dstamp = LOGFILE_TSTAMP_FORMATTER.format(date);
            if (logWriter == null || !dstamp.equals(timeStamp)) {
                logWriter = rotate(dstamp);
            }
            if(logWriter == null){
                BACKUP_LOGGER.log(record.getLevel(), record.getMessage());
            } else {
                logWriter.write(getFormatter().format(record));
                logWriter.flush();
            }
        } catch (Exception ex) {
            BACKUP_LOGGER.log(Level.WARNING, "public failed", ex);
        }
    }

    /**
     * Closed the current log file and start a new one
     * @param timestamp Date for the file name
     * @return A Write for the log file
     */
    protected BufferedWriter rotate(final String timestamp) {
        try {
            if(logWriter != null){
                logWriter.close();
                logWriter = null;
            }
            final File dir = new File(getLogDirectory());
            if (!dir.exists()) {
                if(!dir.mkdirs()){
                    BACKUP_LOGGER.log(Level.WARNING, "Failed to create the directory tree " + getLogDirectory());
                }
            }
            final File f = new File(dir, "services-" + timestamp + ".log");
            logWriter = new BufferedWriter(new FileWriter(f, true));
            timeStamp = timestamp;
            logWriter.write(getFormatter().getHead(this));
            logWriter.flush();
        } catch (Exception e) {
            BACKUP_LOGGER.log(Level.WARNING, "rotate failed", e);
            return null;
        }
        return logWriter;
    }
}
