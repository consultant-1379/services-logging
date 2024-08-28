package com.ericsson.eniq.events.server.logging.performance;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServicePerformanceTraceLoggerTest {

    private static File LOG_DIR = null;

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy_MM_dd");

    private static String originalLogDir = null;

    private static ServicePerformanceTraceLogger servicePerformanceTraceLogger;

    private static ServicePeformanceContextInformation contxtInfo;

    //Delimiter used in performance logger
    private static final String DELIMITER = "|";

    @BeforeClass
    public static void beforeClass() {

        final String logDir = "LOG_DIR";
        final String homeDir = System.getProperty("user.workspace");
        System.out.println("homeDir = " + homeDir);
        if (System.getProperty(logDir) == null) {
            System.setProperty("LOG_DIR", homeDir);
        } else {
            // In the CI env there's no write access to /eniq/... so reset it to somewhere else
            originalLogDir = System.getProperty(logDir);
            System.setProperty("LOG_DIR", homeDir);
        }

        servicePerformanceTraceLogger = new ServicePerformanceTraceLogger();
        contxtInfo = new ServicePeformanceContextInformation();
        contxtInfo.setInterval(60);
        contxtInfo.setPoolName("reader");
        contxtInfo.setQueryExecutionEndTime(Calendar.getInstance().getTimeInMillis());
        contxtInfo.setQueryExecutionStartTime(Calendar.getInstance().getTimeInMillis());
        contxtInfo.setRequestEndTime(Calendar.getInstance().getTimeInMillis());
        contxtInfo.setRequestStartTime(Calendar.getInstance().getTimeInMillis());
        contxtInfo.setUriInfo("uriInfo");
        servicePerformanceTraceLogger.setFileLimit(10);
        servicePerformanceTraceLogger.resetHandlers();
        servicePerformanceTraceLogger.setLevel(Level.OFF);

        LOG_DIR = new File(servicePerformanceTraceLogger.getServicesLogDirectory());
    }

    @AfterClass
    public static void afterClass() {

        if (originalLogDir != null) {
            System.setProperty("LOG_DIR", originalLogDir);
        }

    }

    @Before
    public void beforeTest() {
        servicePerformanceTraceLogger.resetHandlers();
        cleanLogs();
    }

    @After
    public void afterTest() {
        servicePerformanceTraceLogger.setLevel(Level.OFF);
        cleanLogs();
    }

    //    @Test
    //    public void testResetHandlers() throws SecurityException, IOException {
    //        final Level origLevel = servicePerformanceTraceLogger.getLevel();
    //        setLevel(Level.FINE);
    //        try {
    //            final Logger rawLogger = servicePerformanceTraceLogger.getRawLogger();
    //            rawLogger.addHandler(new FileHandler());
    //            rawLogger.addHandler(new FileHandler());
    //            rawLogger.addHandler(new FileHandler());
    //
    //            servicePerformanceTraceLogger.resetHandlers();
    //            assertEquals("There should only be one Handler registered to logger", 1, rawLogger.getHandlers().length);
    //        } finally {
    //            setLevel(origLevel);
    //        }
    //
    //    }

    private void cleanLogs() {
        servicePerformanceTraceLogger.closeLogFiles();
        if (LOG_DIR == null) {
            return;
        }
        final File[] toDelete = LOG_DIR.listFiles();
        if (toDelete == null || toDelete.length == 0) {
            return;
        }
        for (final File f : toDelete) {
            if (!f.delete()) {
                System.err.println("Failed to delete " + f.getAbsolutePath());
            }
        }
        if (!LOG_DIR.delete()) {
            System.err.println("Failed to delete " + LOG_DIR.getAbsolutePath());
        }

    }

    private void setLevel(final Level level) {
        Logger.getLogger(ServicePerformanceTraceLogger.SERVICES_PERFORMANCE_TRACE_LOGGER_NAME).setLevel(level);
    }

    @Test
    public void testDetailedWithFine() throws Exception {
        final Level origLevel = servicePerformanceTraceLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicePerformanceTraceLogger.detailed(Level.FINE, contxtInfo);
            final String logged = getLogContents(".0");
            assertTrue(logged.contains("FINE"));
            assertTrue(logged.contains(contxtInfo.getContextInformation(DELIMITER)));
        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testDetailedWithInfo() throws Exception {
        final Level origLevel = servicePerformanceTraceLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicePerformanceTraceLogger.detailed(Level.INFO, contxtInfo);
            final String logged = getLogContents(".0");
            assertTrue(logged.contains("INFO"));
            assertTrue(logged.contains(contxtInfo.getContextInformation(DELIMITER)));
        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testLogRollover() throws Exception {
        final Level origLevel = servicePerformanceTraceLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicePerformanceTraceLogger.detailed(Level.INFO, contxtInfo);

            final String savedTimeStamp = servicePerformanceTraceLogger.getTimeStamp(); // save existing timestamp

            servicePerformanceTraceLogger.setTimeStamp(String.valueOf(System.currentTimeMillis()
                    + (1000L * 60 * 60 * 24))); // ie +24 hours to simulate a midnight transition)
            servicePerformanceTraceLogger.detailed(Level.FINE, contxtInfo);

            servicePerformanceTraceLogger.setTimeStamp(savedTimeStamp); // restore existing timestamp

            String logged = getLogContents(".0");
            assertTrue(logged.contains("FINE"));
            assertTrue(logged.contains(contxtInfo.getContextInformation(DELIMITER)));

            logged = getLogContents(".1");
            assertTrue(logged.contains("INFO"));
            assertTrue(logged.contains(contxtInfo.getContextInformation(DELIMITER)));

        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testLogFileNotCreatedWhenLoggerIsOff() {
        final Level origLevel = servicePerformanceTraceLogger.getLevel();
        setLevel(Level.OFF);
        try {
            servicePerformanceTraceLogger.detailed(Level.INFO, contxtInfo);

            final Date date = new Date(System.currentTimeMillis());
            final String dstamp = DATE_FORMATTER.format(date);
            final File f = new File(LOG_DIR, "servicesperformancetrace-" + dstamp + ".log" + ".0");

            assertFalse("test failed - log file is created when log level is set to OFF", f.exists());

        } finally {
            setLevel(origLevel);
        }
    }

    private String getLogContents(final String fileExtension) throws IOException {
        final Date date = new Date(System.currentTimeMillis());
        final String dstamp = DATE_FORMATTER.format(date);
        final File f = new File(LOG_DIR, "servicesperformancetrace-" + dstamp + ".log" + fileExtension);
        if (f.exists()) {
            final BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            final StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        }
        throw new FileNotFoundException(f.getAbsolutePath());
    }
}
