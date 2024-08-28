package com.ericsson.eniq.events.server.logging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServicesLogFormatterTest {
    private static ServicesLogFormatter objUnderTest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            objUnderTest = new ServicesLogFormatter();
        } catch (Exception e) {
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
        final String expected = "01.01 01:00:00 10 SEVERE logrecordname : testGeneral\n";
        assertEquals(expected, objUnderTest.format(record));
    }
    @Test
    public void testFormatThrowable() throws Exception {
        final LogRecord record = new LogRecord(Level.FINE, "testThrowable");
        record.setLoggerName("logrecordname");
        record.setThreadID(15);
        record.setMillis(999);
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) final Throwable thrown = new Throwable("throwableMessage");
        record.setThrown(thrown);
        final String expected = "01.01 01:00:00 15 FINE logrecordname : testThrowable\n" +
            "        java.lang.Throwable: throwableMessage";
        final String result = objUnderTest.format(record);
        assertTrue(result.startsWith(expected));
    }

    @Test
    public void testFormatWithNullLogRecord() throws Exception {
        final LogRecord record = null;
        try {
            objUnderTest.format(record);
            fail("Test failed - NullPointerException expected as the LogRecord is null");
        } catch (NullPointerException npe) {
            // expected this
        } catch (Exception e) {
            fail("Unexpected error occurred - NullPointerException was expected" + e);
        }
    }
}
