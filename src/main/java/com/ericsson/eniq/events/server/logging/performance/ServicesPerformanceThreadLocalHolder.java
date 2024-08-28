/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.logging.performance;

/**
 * This wrapper class provide static utilities to hold the performance context information
 * @author echchik
 *
 */
public class ServicesPerformanceThreadLocalHolder {

    private static final ServicesPerformanceThreadLocal PERFORMANCE_CONTEXT = 
        new ServicesPerformanceThreadLocal();

    private ServicesPerformanceThreadLocalHolder(){
        PERFORMANCE_CONTEXT.initialValue();
    }

    public static void setInterval(final long interval) {
        PERFORMANCE_CONTEXT.get().setInterval(interval);
    }

    public static void setRequestStartTime(final long reqStartTime) {
        PERFORMANCE_CONTEXT.get().setRequestStartTime(reqStartTime);
    }

    public static void setRequestEndTime(final long reqEndTime) {
        PERFORMANCE_CONTEXT.get().setRequestEndTime(reqEndTime);
    }

    public static void setQueryExecutionStartTime(final long queryExecStartTime) {
        PERFORMANCE_CONTEXT.get().setQueryExecutionStartTime(queryExecStartTime);
    }

    public static void setQueryExecutionEndTime(final long queryExecEndTime) {
        PERFORMANCE_CONTEXT.get().setQueryExecutionEndTime(queryExecEndTime);
    }

    public static void setUriInfo(final String uriInfo) {
        PERFORMANCE_CONTEXT.get().setUriInfo(uriInfo);
    }

    public static void setPoolName(final String poolName){
        PERFORMANCE_CONTEXT.get().setPoolName(poolName);
    }

    public static ServicePeformanceContextInformation getContextInfo(){
        return PERFORMANCE_CONTEXT.get() ;
    }

    public static void releaseAllResources() {
        PERFORMANCE_CONTEXT.remove();
    }
}
