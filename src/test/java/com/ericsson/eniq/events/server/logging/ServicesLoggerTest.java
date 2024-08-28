package com.ericsson.eniq.events.server.logging;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.eniq.events.server.logging.stubs.DummyUriInfoImplForLogger;

public class ServicesLoggerTest {
    private static final LogRecord LOG_RECORD = new LogRecord(Level.parse("1000"), "Message");

    private static File LOG_DIR = null;

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy_MM_dd");

    private static String originalLogDir = null;

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
        LOG_DIR = new File(ServicesLogger.getServicesLogDirectory());
        LOG_RECORD.setLoggerName("file.Logger.Log");
        LOG_RECORD.setMillis(10000);
        ServicesLogger.setLevel(Level.INFO);
    }

    @AfterClass
    public static void afterClass() {
        if (originalLogDir != null) {
            System.setProperty("LOG_DIR", originalLogDir);
        }
        ServicesLogger.setLevel(Level.FINEST);
    }

    @Before
    public void setUp() {
        cleanLogs();
    }

    @After
    public void tearDown() {
        cleanLogs();
    }

    @Test
    public void testResetHandlers() {
        final Logger rawLogger = ServicesLogger.getRawLogger();
        rawLogger.addHandler(new ServicesLoggingHandler());
        rawLogger.addHandler(new ServicesLoggingHandler());
        rawLogger.addHandler(new ServicesLoggingHandler());

        ServicesLogger.resetHandlers();
        assertEquals("There should only be one Handler registered to logger", 1, rawLogger.getHandlers().length);

    }

    private void cleanLogs() {
        ServicesLogger.closeLogFiles();
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

    @Test
    public void testLogMap() throws Exception {
        final Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("keya", "valuea");
        testMap.put("keyb", "valueb");
        ServicesLogger.info("CLASSNAME", "METHODNAME", testMap);
        final String logged = getLogContents();
        assertTrue(logged.contains("Map[keya=valuea; keyb=valueb]"));
    }

    @Test
    public void testLogEmptyMap() throws Exception {
        final Map<String, String> testMap = new HashMap<String, String>();
        ServicesLogger.info("CLASSNAME", "METHODNAME", testMap);
        final String logged = getLogContents();
        assertTrue(logged.contains("Map[]"));
    }

    @Test
    public void testLogNullMap() throws Exception {
        final Map<String, String> testMap = null;
        ServicesLogger.info("CLASSNAME", "METHODNAME", testMap);
        final String logged = getLogContents();
        assertTrue(logged.contains("<Null>"));
    }

    @Test
    public void testLogCollection() throws Exception {
        final List<String> testCollection = Arrays.asList("index-0", "index-n");
        ServicesLogger.info("CLASSNAME", "METHODNAME", testCollection);
        final String logged = getLogContents();
        assertTrue(logged.contains("Collection[index-0, index-n]"));
    }

    @Test
    public void testLogEmptyCollection() throws Exception {
        final List<String> testCollection = new ArrayList<String>();
        ServicesLogger.info("CLASSNAME", "METHODNAME", testCollection);
        final String logged = getLogContents();
        assertTrue(logged.contains("Collection[]"));
    }

    @Test
    public void testLogNullCollection() throws Exception {
        final List<String> testCollection = null;
        ServicesLogger.info("CLASSNAME", "METHODNAME", testCollection);
        final String logged = getLogContents();
        assertTrue(logged.contains("<Null>"));
    }

    @Test
    public void testLogArray() throws Exception {
        final String[] testArray = { "arr0", "arrn", null };
        ServicesLogger.info("CLASSNAME", "METHODNAME", ((Object) testArray));
        final String logged = getLogContents();
        assertTrue(logged.contains("String[][arr0,arrn,<Null>]"));
    }

    @Test
    public void testLogEmptyArray() throws Exception {
        final String[] testArray = {};
        ServicesLogger.info("CLASSNAME", "METHODNAME", ((Object) testArray));
        final String logged = getLogContents();
        assertTrue(logged.contains("String[]<EMPTY>"));
    }

    @Test
    public void testLogNullArray() throws Exception {
        final String[] testArray = null;
        ServicesLogger.info("CLASSNAME", "METHODNAME", ((Object) testArray));
        final String logged = getLogContents();
        assertTrue(logged.contains("<Null>"));
    }

    @Test
    public void testLogUriInfo() throws Exception {
        final UriInfo testUri = new DummyUriInfoImplForLogger(null, "baseURI", "somePath");
        ServicesLogger.info("CLASSNAME", "METHODNAME", testUri);
        final String logged = getLogContents();
        assertTrue(logged.contains("UriInfo[baseURI]"));
    }

    @Test
    public void testDetailed() throws Exception {
        final Level origLevel = ServicesLogger.getLevel();
        setLevel(Level.FINEST);
        try {
            ServicesLogger.detailed("CLASSNAME", "METHODNAME", "info1", "info33");
            final String logged = getLogContents();
            assertTrue(logged.contains("FINEST "));
            assertTrue(logged.contains("CLASSNAME"));
            assertTrue(logged.contains("METHODNAME"));
            assertTrue(logged.contains("<info1>"));
            assertTrue(logged.contains("<info33>"));
        } finally {
            setLevel(origLevel);
        }
    }

    private void setLevel(final Level level) {
        Logger.getLogger(ServicesLogger.SERVICES_LOGGER_NAME).setLevel(level);
    }

    @Test
    public void testDetailedWithLevel() throws Exception {
        ServicesLogger.detailed(Level.INFO, "CLASSNAME", "METHODNAME", "info1", "info33");
        final String logged = getLogContents();
        assertTrue(logged.contains("INFO "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("<info1>"));
        assertTrue(logged.contains("<info33>"));
    }

    @Test
    public void testEnter() throws Exception {
        ServicesLogger.enter(Level.INFO, "CLASSNAME", "METHODNAME", "param1", 2);
        final String logged = getLogContents();
        assertTrue(logged.contains("INFO "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("<param1>"));
        assertTrue(logged.contains("<2>"));
    }

    @Test
    public void testExitReturnResult() throws Exception {
        ServicesLogger.exit(Level.INFO, "CLASSNAME", "METHODNAME", "param1");
        final String logged = getLogContents();
        assertTrue(logged.contains("INFO "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("<param1>"));
    }

    @Test
    public void testExitVoid() throws Exception {
        ServicesLogger.exit(Level.INFO, "CLASSNAME", "METHODNAME", null);
        final String logged = getLogContents();
        assertTrue(logged.contains("INFO "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
    }

    @Test
    public void testException() throws Exception {
        //noinspection ThrowableInstanceNeverThrown
        ServicesLogger.exception("CLASSNAME", "METHODNAME", "something", new Exception("testException"));
        final String logged = getLogContents();
        assertTrue(logged.contains("SEVERE "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("testException"));
    }

    @Test
    public void testWarning() throws Exception {
        ServicesLogger.warn("CLASSNAME", "METHODNAME", "param1", "tyty");
        final String logged = getLogContents();
        assertTrue(logged.contains("WARNING "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("<param1>"));
        assertTrue(logged.contains("<tyty>"));
    }

    @Test
    public void testError() throws Exception {
        ServicesLogger.error("CLASSNAME", "METHODNAME", "someERROr");
        final String logged = getLogContents();
        assertTrue(logged.contains("SEVERE "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("someERROr"));
    }

    @Test
    public void testInfo() throws Exception {
        ServicesLogger.info("CLASSNAME", "METHODNAME", "someINF0");
        final String logged = getLogContents();
        assertTrue(logged.contains("INFO "));
        assertTrue(logged.contains("CLASSNAME"));
        assertTrue(logged.contains("METHODNAME"));
        assertTrue(logged.contains("someINF0"));
    }

    private String getLogContents() throws IOException {
        final Date date = new Date(System.currentTimeMillis());
        final String dstamp = DATE_FORMATTER.format(date);
        final File f = new File(LOG_DIR, "services-" + dstamp + ".log");
        if (f.exists()) {
            final BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            final StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            System.out.println(sb.toString());
            return sb.toString();
        }
        throw new FileNotFoundException(f.getAbsolutePath());
    }
}
