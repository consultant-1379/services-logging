/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.logging.performance;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class holds the context information related to logging performance info
 * 
 * @author echchik
 *
 */
public class ServicePeformanceContextInformation{

    private static final String EMPTY_STRING = "";

    /**
     * Default date format
     */
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private long interval ;

    private String poolName ;

    private String reqStartTime ;

    private String reqEndTime ;

    private String queryExecStartTime ;

    private String queryExecEndTime ;

    private String uriInfo ;

    /**
     * Constructor
     */
    public ServicePeformanceContextInformation(){
        //Set the String fields to empty string as it may be null and string builder will
        //throw null pointer exception
        poolName = EMPTY_STRING ;
        reqStartTime = EMPTY_STRING;
        reqEndTime = EMPTY_STRING;
        queryExecEndTime = EMPTY_STRING;
        queryExecStartTime = EMPTY_STRING ;
        uriInfo = EMPTY_STRING ;
    }

    public void setRequestStartTime(final long reqStartTime) {
        final String timeStr = simpleDateFormat.format(new Date(reqStartTime));
        this.reqStartTime = timeStr;
    }

    public void setRequestEndTime(final long reqEndTime) {
        final String timeStr = simpleDateFormat.format(new Date(reqEndTime));
        this.reqEndTime = timeStr;
    }

    public void setQueryExecutionStartTime(final long queryExecStartTime) {
        final String timeStr = simpleDateFormat.format(new Date(queryExecStartTime));
        this.queryExecStartTime = timeStr;
    }

    public void setQueryExecutionEndTime(final long queryExecEndTime) {
        final String timeStr = simpleDateFormat.format(new Date(queryExecEndTime));
        this.queryExecEndTime = timeStr;
    }

    public void setInterval(final long interval) {
        this.interval = interval;
    }

    public void setUriInfo(final String uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setPoolName(final String poolName) {
        this.poolName = poolName;
    }

    public String getContextInformation(final String delimeter) {
        final StringBuilder contextInfo = new StringBuilder(poolName);
        contextInfo.append(delimeter);
        contextInfo.append(reqStartTime);
        contextInfo.append(delimeter);
        contextInfo.append(reqEndTime);
        contextInfo.append(delimeter);
        contextInfo.append(queryExecStartTime);
        contextInfo.append(delimeter);
        contextInfo.append(queryExecEndTime);
        contextInfo.append(delimeter);
        contextInfo.append(interval);
        contextInfo.append(delimeter);
        contextInfo.append(uriInfo);
        return contextInfo.toString();
    }

}
