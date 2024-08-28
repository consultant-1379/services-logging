/**
 * ---------------------------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * ---------------------------------------------------------------------------------------
 */

package com.ericsson.eniq.events.server.logging.performance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Eniq Services Performance Trace Log file formatter
 */
public class ServicesPerformanceTraceLogFormatter extends Formatter {

    /**
     * String used in building log messages.
     */
    private static final String DELIMITER = "|";

    /**
     * Default date format
     * Taken from Eniq Engine
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Formats one log entry.
     *
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(final LogRecord logRecord) {

        final StringBuilder res = new StringBuilder();

        res.append("[#");

        res.append(DELIMITER);
        res.append(SDF.format(new Date(logRecord.getMillis())));
        res.append(DELIMITER);
        res.append(logRecord.getThreadID());
        res.append(DELIMITER);
        res.append(logRecord.getLevel().getName());
        res.append(DELIMITER);
        res.append(logRecord.getMessage());
        res.append(DELIMITER);
        res.append("#]");
        res.append("\n");

        return res.toString();
    }

}
