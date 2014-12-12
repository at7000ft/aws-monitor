/**
 * <p>
 * Title: TestCWMonitorWCounts.java
 * </p>
 * <p>
 * Description: A CWMonitor simple example test application.
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

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.webpilot.monitor.exceptions.CWMetricException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class TestCWMonitorWCounts {
	// CloudWatch metric namespace 'blackhawk.keystone.<app name>', all metrics added to CW using this CWMonitor instance
	// will be categorized under this.
	private static final String namespace = "BHN/KS/TestCWMonitorWCounts";
	// Event name constant to be sent to SNS via CWMonitor to generate admin email when something real bad happens
	private static final String CANT_TALK_TO_SC_EVENT = "cantTalkToSCEvent";
	// Define a constant for each operation to be monitored
	private static final String METRIC_NAME_TRANS_Z4 = "transz4";
	// Maintain a success and failure count for each operation
	private long transz3SuccessCount = 0;
	private long transz3FailCount = 0;
	// Create a CWMonitor to push metrics to CW or events to SNS
	private CWMonitorIF monitor = null;
	private long baseLatencyValue = 200;
	private static final Logger logger = LoggerFactory.getLogger(TestCWMonitorWCounts.class);

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		logger.debug("main: Starting");
		TestCWMonitorWCounts test = new TestCWMonitorWCounts();
		try {
			test.monitor = new CWMonitor(namespace);
		} catch (CWMetricException e) {
			logger.error("main: error - " + e.getMessage());
			System.exit(1);
		}
		for (int i = 0; i < 130; i++) {
			test.callDoSomeTransaction();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		logger.debug("main: Done");
	}

	/**
	 * Call doSomeTransaction capturing duration and push metrics to CW.
	 */
	private void callDoSomeTransaction() {
		boolean status = true;
		Map<String,String> dimensionsAtalla = new HashMap<String,String>();
		dimensionsAtalla.put("conn_peer", "atalla");
		Map<String,String> dimensionSuccess = new HashMap<String,String>();
		dimensionSuccess.put("status", "success");
		Map<String,String> dimensionFailure = new HashMap<String,String>();
		dimensionFailure.put("status", "failure");
		// Get the system time before calling operation
		long startTime = System.currentTimeMillis();
		try {
			// Invoke the operation to be monitored
			status = doSomeTransaction();
		} catch (Exception e) {
			// Something bad happened, increment fail count and push fail count metric
			transz3FailCount++;
			// Push failure count metric to CW
			monitor.putCWMetric(METRIC_NAME_TRANS_Z4, null,StandardUnit.Count, transz3FailCount, dimensionFailure);
			return;
		}
		// Get system time after operation complete
		long endTime = System.currentTimeMillis();
		// Push timing metric to CW with operation duration
		monitor.putCWMetric(METRIC_NAME_TRANS_Z4, null,StandardUnit.Milliseconds, endTime - startTime, status ? dimensionSuccess: dimensionFailure);
 
		// Push count metic to CW, either fail count or success count
		if (status) {
			transz3SuccessCount++;
			// Push success count metric to CW
			monitor.putCWMetric(METRIC_NAME_TRANS_Z4, null,StandardUnit.Count, transz3SuccessCount,  dimensionSuccess);
		} else {
			transz3FailCount++;
			// Push failure count metric to CW
			monitor.putCWMetric(METRIC_NAME_TRANS_Z4, null,StandardUnit.Count, transz3FailCount,  dimensionFailure);
		}
	}

	/**
	 * Do some transaction (could involve network comm, just processing, or both).
	 * 
	 * @return true, if successful
	 */
	private boolean doSomeTransaction() {
		try {
			long sleepDuration = baseLatencyValue + getRandomBetweenPlusMinus(-100, 120);
			Thread.sleep((long)sleepDuration);
		} catch (InterruptedException e) {
		}
		return Math.random() < 0.85;
	}

 

	private int getRandomBetweenPlusMinus(int min, int max) {
		if (max < min) {
			return 0;
		}
		int newMax = max - min;
		Random r = new Random();
		int val = r.nextInt(newMax);
		return val + min;
	}
}
