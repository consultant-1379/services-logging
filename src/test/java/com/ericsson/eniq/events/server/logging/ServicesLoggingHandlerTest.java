package com.ericsson.eniq.events.server.logging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ServicesLoggingHandlerTest {
    private static String homeDir;
    private static ServicesLoggingHandler instance;
    private static LogRecord lR;
    private static String dstamp;

    @BeforeClass
    public static void init() throws IOException {
        DateFormat form = new SimpleDateFormat("yyyy_MM_dd");
        Date dat = new Date(10000);
        dstamp = form.format(dat);
        homeDir = System.getProperty("user.workspace");
        System.setProperty("LOG_DIR", homeDir);
        lR = new LogRecord(Level.parse("1000"), "Message");
        lR.setLoggerName("file.Logger.Log");
        lR.setMillis(10000);
        instance = new ServicesLoggingHandler();
    }

    /**
     * Test that log files exists
     */

    @Test
    public void testPublish() {

        instance.publish(lR);
        instance.flush();
        instance.close();

        File logFile = new File(homeDir, File.separator + "services" + File.separator + "services" + "-" + dstamp + ".log");

        String expected = "01.01 01:00:10 10 SEVERE file.Logger.Log : Message";
        try {
            String logActual = readFileToString(logFile);
            assertEquals(expected, logActual);
        } catch (Exception e) {
            e.printStackTrace();
            fail("testPublish() failed");
        }
        assertEquals(true, logFile.isFile());
        logFile.delete();
    }

    @Test
    public void testRotate() {
        try {
            instance.rotate("timestamp");
            instance.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("testRotate() failed");
        }

        File logFile3 = new File(homeDir, File.separator + "services" + File.separator + "services" + "-" + "timestamp" + ".log");

        assertEquals(true, logFile3.isFile());

        logFile3.delete();
    }

    @AfterClass
    public static void clean() {
        File logDir = new File(homeDir, "services");
        logDir.delete();
    }

    private String readFileToString(File f) throws Exception {

        BufferedReader reader = null;
        String result = null;

        try {
            reader = new BufferedReader(new FileReader(f));
            String input;
            while ((input = reader.readLine()) != null) {
                result = input;
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    System.out.println("Error occurred during closing the file");
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
