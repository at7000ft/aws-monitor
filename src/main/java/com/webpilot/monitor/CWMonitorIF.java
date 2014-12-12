/**
 * <p>
 * Title: CWMonitorIF.java
 * </p>
 * <p>
 * Description: CWMonitor interfaces.
 * 
 * </p>
 * <p>
 * 2013
 * </p>
 *
 * @author Rick Holland
 * 
 * 
 */
package com.webpilot.monitor;

import java.util.Map;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * The Interface CWMonitorIF.
 */
public interface CWMonitorIF {
    
    /**
     * The Enum STATUS.
     */
    enum STATUS {
        SUCCESS, FAILURE
    };
    
    /**
     * Send a metric value to CloudWatch synchonously.
     *
     * @param metricName the metric name (name of the operation)
     * @param namespace - overrides the default namespace if not null (optional). CF stackname is appended by monitor lib.
     * @param unit - StandardUnit e.g. Count, Milliseconds, Nanoseconds, Percent, etc.
     * @param value the long value, counts, message latency, percentage, etc.
     * @param dimensions - a map of string key value pairs further defining the metric e.g. instanceId, peerId, etc.
     * @return true, if successful else false
     */
    boolean putCWMetric(String metricName, String namespace, StandardUnit unit, long value, Map<String,String> dimensions);

    /**
     * Asynchronously on <timing interval> intervals send average latency, success percentage, and total count type
     * metric values to CloudWatch. Internally maintain total counts and success counts for each unique metric.
     * 
     * @param metricName the metric name (name of the operation). Monitor lib appends a different string for each of the three metric type sent to CW:
     *      "-latency"  - for the Milliseconds type latency value.
     *      "-tcount"   - for the Count type total count.
     *      "-spercent"   - for the Percent type succuss percentage.
     * @param namespace - overrides the default namespace if not null (optional). CF stackname is appended by monitor lib.
     * @param msLatency the long value, message latency
     * @param status the status, SUCCESS or FAILURE
     * @param instIdDimension if true the EC2 instance id is added to the metrics as a dimension in the form
     *        'instanceId=xxxx'
     * @return true, if successful else false
     */
    boolean putCWAsyncMultiMetric(String metricName, String namespace, long msLatency, STATUS status, boolean instIdDimension);

    /**
     * Asynchronously on <timing interval> intervals send averaged value metric to CloudWatch.
     *
     * @param metricName the metric name (name of the operation)
     * @param namespace - overrides the default namespace if not null (optional). CF stackname is appended by monitor lib.
     * @param unit the unit
     * @param value the long value, message latency
     * @param dimensions - a map of string key value pairs further defining the metric e.g. responseId, peerId, etc.
     * @param instIdDimension if true the EC2 instance id is added to the metrics as a dimension in the form
     * 'instanceId=xxxx'
     * @return true, if successful else false
     */
    boolean putCWAsyncMetric(String metricName, String namespace, StandardUnit unit, long value, Map<String, String> dimensions,
                boolean instIdDimension);
    
    /**
     * Close all threads, called at application exit.
     */
    void close();
    
}
