package com.ericsson.eniq.events.server.logging.audit;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServicesAuditLoggerTest {
    private static final LogRecord LOG_RECORD = new LogRecord(Level.parse("1000"), "Message");

    private static File LOG_DIR = null;

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy_MM_dd");

    private static String originalLogDir = null;

    private static ServicesAuditLogger servicesAuditLogger;

    @BeforeClass
    public static void beforeClass() {

        final String logDir = "LOG_DIR";
        final String homeDir = System.getProperty("user.workspace");
        if (System.getProperty(logDir) == null) {
            System.setProperty("LOG_DIR", homeDir);
        } else {
            // In the CI env there's no write access to /eniq/... so reset it to somewhere else
            originalLogDir = System.getProperty(logDir);
            System.setProperty("LOG_DIR", homeDir);
        }

        servicesAuditLogger = new ServicesAuditLogger();

        servicesAuditLogger.setFileLimit(10);
        servicesAuditLogger.resetHandlers();
        servicesAuditLogger.setLevel(Level.OFF);

        LOG_DIR = new File(servicesAuditLogger.getServicesLogDirectory());
    }

    @AfterClass
    public static void afterClass() {

        if (originalLogDir != null) {
            System.setProperty("LOG_DIR", originalLogDir);
        }

    }

    @Before
    public void beforeTest() {
        servicesAuditLogger.resetHandlers();
        cleanLogs();
    }

    @After
    public void afterTest() {
        servicesAuditLogger.setLevel(Level.OFF);
        cleanLogs();
    }

    //    @Test
    //    public void testResetHandlers() throws SecurityException, IOException {
    //        final Level origLevel = servicesAuditLogger.getLevel();
    //        setLevel(Level.FINE);
    //        try {
    //            final Logger rawLogger = servicesAuditLogger.getRawLogger();
    //            rawLogger.addHandler(new FileHandler());
    //            rawLogger.addHandler(new FileHandler());
    //            rawLogger.addHandler(new FileHandler());
    //
    //            servicesAuditLogger.resetHandlers();
    //            assertEquals("There should only be one Handler registered to logger", 1, rawLogger.getHandlers().length);
    //        } finally {
    //            setLevel(origLevel);
    //        }
    //
    //    }

    private void cleanLogs() {
        servicesAuditLogger.closeLogFiles();
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
        Logger.getLogger(ServicesAuditLogger.SERVICES_AUDIT_LOGGER_NAME).setLevel(level);
    }

    @Test
    public void testDetailedWithFine() throws Exception {
        final Level origLevel = servicesAuditLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicesAuditLogger.detailed(Level.FINE, "info321", "info4321");
            final String logged = getLogContents(".0");
            assertTrue(logged.contains("FINE"));
            assertTrue(logged.contains("info321"));
            assertTrue(logged.contains("info4321"));
        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testDetailedWithInfo() throws Exception {
        final Level origLevel = servicesAuditLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicesAuditLogger.detailed(Level.INFO, "info123", "info1234");
            final String logged = getLogContents(".0");
            assertTrue(logged.contains("INFO"));
            assertTrue(logged.contains("info123"));
            assertTrue(logged.contains("info1234"));
        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testLogRollover() throws Exception {
        final Level origLevel = servicesAuditLogger.getLevel();
        setLevel(Level.FINE);
        try {
            servicesAuditLogger.detailed(Level.INFO, "info345", "info3456");

            final String savedTimeStamp = servicesAuditLogger.getTimeStamp(); // save existing timestamp

            servicesAuditLogger.setTimeStamp(String.valueOf(System.currentTimeMillis() + (1000L * 60 * 60 * 24))); // ie +24 hours to simulate a midnight transition)
            servicesAuditLogger.detailed(Level.FINE, "info456", "info4567");

            servicesAuditLogger.setTimeStamp(savedTimeStamp); // restore existing timestamp

            String logged = getLogContents(".0");
            assertTrue(logged.contains("FINE"));
            assertTrue(logged.contains("info456"));
            assertTrue(logged.contains("info4567"));

            logged = getLogContents(".1");
            assertTrue(logged.contains("INFO"));
            assertTrue(logged.contains("info345"));
            assertTrue(logged.contains("info3456"));

        } finally {
            setLevel(origLevel);
        }
    }

    @Test
    public void testLogFileNotCreatedWhenLoggerIsOff() throws Exception {
        final Level origLevel = servicesAuditLogger.getLevel();
        setLevel(Level.OFF);
        try {
            servicesAuditLogger.detailed(Level.INFO, "info123", "info1234");

            final Date date = new Date(System.currentTimeMillis());
            final String dstamp = DATE_FORMATTER.format(date);
            final File f = new File(LOG_DIR, "servicesaudit-" + dstamp + ".log" + ".0");

            assertFalse("test failed - log file is created when log level is set to OFF", f.exists());

        } finally {
            setLevel(origLevel);
        }
    }

    private String getLogContents(final String fileExtension) throws IOException {
        final Date date = new Date(System.currentTimeMillis());
        final String dstamp = DATE_FORMATTER.format(date);
        final File f = new File(LOG_DIR, "servicesaudit-" + dstamp + ".log" + fileExtension);
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
