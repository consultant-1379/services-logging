package com.ericsson.eniq.events.server.logging.performance;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ServicesPerformanceTraceLogFormatterTest {
    private static ServicesPerformanceTraceLogFormatter objUnderTest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            objUnderTest = new ServicesPerformanceTraceLogFormatter();
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
        final String expected = "[#|1970-01-01 01:00:00.999|10|SEVERE|testGeneral|#]\n";
        assertEquals(expected, objUnderTest.format(record));
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
