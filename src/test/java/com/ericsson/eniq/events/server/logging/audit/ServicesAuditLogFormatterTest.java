package com.ericsson.eniq.events.server.logging.audit;

import static org.junit.Assert.*;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServicesAuditLogFormatterTest {
    private static ServicesAuditLogFormatter objUnderTest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            objUnderTest = new ServicesAuditLogFormatter();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        objUnderTest = null;
    }

    @Test
    public void testFormatGeneral() throws Exception {
        final LogRecord record = new LogRecord(Level.SEVERE, "testGeneral");
        record.setLoggerName("logrecordname");
        record.setThreadID(10);
        record.setMillis(999);
        final String expected = "[#|1970-01-01 01:00:00.999|10|SEVERE|testGeneral|#]\n";
        assertEquals(expected, objUnderTest.format(record));
    }

    @Test
    public void testFormatThrowable() throws Exception {
        final LogRecord record = new LogRecord(Level.FINE, "testThrowable");
        record.setLoggerName("logrecordname");
        record.setThreadID(15);
        record.setMillis(999);
        @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
        final Throwable thrown = new Throwable("throwableMessage");
        record.setThrown(thrown);
        final String expected = "[#|1970-01-01 01:00:00.999|15|FINE|testThrowable|#]\n"
                + "        java.lang.Throwable: throwableMessage";
        final String result = objUnderTest.format(record);
        assertTrue(result.startsWith(expected));
    }

    @Test
    public void testFormatWithNullLogRecord() throws Exception {
        final LogRecord record = null;
        try {
            objUnderTest.format(record);
            fail("Test failed - NullPointerException expected as the LogRecord is null");
        } catch (final NullPointerException npe) {
            // expected this
        } catch (final Exception e) {
            fail("Unexpected error occurred - NullPointerException was expected" + e);
        }
    }
}
