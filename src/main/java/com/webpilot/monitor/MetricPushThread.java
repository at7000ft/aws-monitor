/**
 * <p>
 * Title: MetricPushThread.java
 * </p>
 * <p>
 * Description: Thread started by CWMonitor. Loops in PUSH_TIME_INTERVAL sleep calling push metrics until cwMetricsPush
 * flag in CWMonitor instance is set false by a close() call by client.
 * 
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricPushThread implements Runnable {
    private CWMonitor monitor = null;
    private static final int PUSH_TIME_INTERVAL = 60000; // In milliseconds
    private static final Logger logger = LoggerFactory.getLogger(MetricPushThread.class);

    /**
     * @param monitor
     */
    public MetricPushThread(CWMonitor monitor) {
        super();
        this.monitor = monitor;
    }

    /*
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (true) {
            try {
                Thread.sleep(PUSH_TIME_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Interrupted");
            }
            // logger.debug("run: pushing metrics");
            List<MetricAccumulator> metrics = monitor.snapshotMetrics();
            if (metrics != null) {
                monitor.pushAllMetricsToCW(metrics);
            } else {
                logger.debug("No new metrics found, push zero data messages");
                // If no new data has been cached by the application send zero latency, success % of 100, and a count of
                // zero
                monitor.pushNoNewDataMetricsToCW();
            }
            // Only exit thread if close has been called on CWMonitor
            if (!monitor.isCwMetricsPush()) {
                logger.debug("Performed final metrics push, and exiting");
                break;
            }
            metrics = null;
        }
        logger.debug("Exiting");
    }
}
