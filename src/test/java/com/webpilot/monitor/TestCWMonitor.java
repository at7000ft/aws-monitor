/**
 * <p>
 * Title: TestCWMonitor.java
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

import java.util.Random;

/**
 * The Class TestCWMonitor.
 */
public class TestCWMonitor {
	//CloudWatch metric namespace 'WP.Project1.<app name>', all metrics added to CW using this CWMonitor instance will be categorized under this.
	private static final String namespace = "WP/Project1/TestCWMonitor";
	//Define a constant for each operation (metric name) to be monitored
	private static final String METRIC_NAME_TRANS_Z3 = "transz3";
	//CWMonitor to push metrics to CW
	private CWMonitorIF monitor = null;
	private static final Logger logger = LoggerFactory.getLogger(TestCWMonitor.class);

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		logger.debug("main: Starting");
		TestCWMonitor test = new TestCWMonitor();
		try {
			test.monitor = new CWMonitor(namespace);
		} catch (CWMetricException e) {
			logger.error("main: error - " + e.getMessage());
			System.exit(1);
		}
		for (int i = 0; i < 130; i++) {
			test.callDoSomeTransaction();
			try {
				Thread.sleep(5000);
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
		// Get the system time before calling operation
		long startTime = System.currentTimeMillis();
		try {
			// Invoke the operation to be monitored
			status = doSomeTransaction();
		} catch (Exception e) {
			logger.error("callDoSomeTransaction: error - " + e);
			return;
		}
		// Get system time after operation complete
		long endTime = System.currentTimeMillis();
		// Push timing metric to CW with operation duration
		monitor.putCWMetric(METRIC_NAME_TRANS_Z3, null,StandardUnit.Milliseconds, endTime - startTime,  null);
	}

	/**
	 * Do some transaction (could involve network comm, just processing, or both).
	 * 
	 * @return true, if successful
	 */
	private boolean doSomeTransaction() {
		long baseLatencyValue = 200;
		try {
			long sleepDuration = baseLatencyValue + getRandomBetweenPlusMinus(-100, 120);
			Thread.sleep((long) sleepDuration);
		} catch (InterruptedException e) {
		}
		//Return true most of the time
		return Math.random() < 0.92;
	}

	/**
	 * Gets the random between plus minus.
	 *
	 * @param min the min
	 * @param max the max
	 * @return the random between plus minus
	 */
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
