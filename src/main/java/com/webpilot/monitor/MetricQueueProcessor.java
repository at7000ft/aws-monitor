/**
 * <p>
 * Title: MetricQueueProcessor.java
 * </p>
 * <p>
 * Description: Thread used to load metric data to a queue.
 * 
 * </p>
 * <p>
 * 2008
 * </p>
 *
 * @author Rick Holland
 * 
 * 
 */
package com.webpilot.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricQueueProcessor implements Runnable {
    private MetricData data = null;
    private CWMonitor monitor = null;
    private static final Logger logger = LoggerFactory.getLogger(MetricQueueProcessor.class);

    public MetricQueueProcessor(MetricData data, CWMonitor monitor) {
        this.data = data;
        this.monitor = monitor;
    }

    /* 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            monitor.queueMetricData(data);
            logger.debug("Loading MetricData - " + data);
        } catch (Exception e) {
            logger.warn("Error - " + e);
        }
    }
}
