/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2010 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.logging.performance;

/**
 * This class is used to hold ServicePeformanceContextInformation
 * @author echchik
 *
 */
public class ServicesPerformanceThreadLocal extends ThreadLocal<ServicePeformanceContextInformation> {

    /**
     * This method is overridden to set the new ServicePeformanceContextInformation object
     * for each request.
     * 
     * By default the ThreadLocal object will have NULL as default initial value.
     */
    @Override
    protected ServicePeformanceContextInformation initialValue(){
        return new ServicePeformanceContextInformation();
    }
}
