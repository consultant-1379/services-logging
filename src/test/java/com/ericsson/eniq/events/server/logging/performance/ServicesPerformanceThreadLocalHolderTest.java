/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.logging.performance;

import static org.junit.Assert.assertEquals;
import static  com.ericsson.eniq.events.server.logging.performance.ServicesPerformanceThreadLocalHolder.*;

import java.io.IOException;
import java.util.Calendar;

import org.junit.BeforeClass;
import org.junit.Test;

public class ServicesPerformanceThreadLocalHolderTest {

    private static ServicePeformanceContextInformation contxtInfo;

    private static final long interval = 60;
    
    private static final String poolName = "reader";
    
    private static final long reqStrtTime = Calendar.getInstance().getTimeInMillis();

    private static final long queryStrtTime = Calendar.getInstance().getTimeInMillis();
    
    private static final long queryEndTime = Calendar.getInstance().getTimeInMillis();

    private static final long reqEndTime = Calendar.getInstance().getTimeInMillis();
    
    private static final String url = "uriInfo";

    //Delimiter used in performance logger
    private static final String DELIMITER = "|";
    
    @BeforeClass
    public static void beforeClass() {

        contxtInfo = new ServicePeformanceContextInformation();
        contxtInfo.setInterval(interval);
        contxtInfo.setPoolName(poolName);
        contxtInfo.setQueryExecutionEndTime(queryEndTime);
        contxtInfo.setQueryExecutionStartTime(queryStrtTime);
        contxtInfo.setRequestEndTime(reqEndTime);
        contxtInfo.setRequestStartTime(reqStrtTime);
        contxtInfo.setUriInfo(url);
    }

    @Test
    public void testSetValue() throws SecurityException, IOException {
        setInterval(interval);
        setPoolName(poolName);
        setQueryExecutionEndTime(queryEndTime);
        setQueryExecutionStartTime(queryStrtTime);
        setRequestEndTime(reqEndTime);
        setRequestStartTime(reqStrtTime);
        setUriInfo(url);
        assertEquals(contxtInfo.getContextInformation(DELIMITER),getContextInfo().getContextInformation(DELIMITER));
        releaseAllResources();
    }
}
