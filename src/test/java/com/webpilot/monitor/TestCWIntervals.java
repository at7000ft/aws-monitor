/**
 * <p>
 * Title: TestCWIntervals.java
 * </p>
 * <p>
 * Description:
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

import com.webpilot.monitor.exceptions.CWMetricException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TestCWIntervals {
	private static final String namespace = "BHN/KS/TestCWIntervals";
	private static final String METRIC_NAME_TRANS_X1 = "transx1";
	private CWMonitorIF monitor = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("main: Starting at " + getUTCTime());
		TestCWIntervals test = new TestCWIntervals();
		try {
			test.monitor = new CWMonitor(namespace);
		} catch (CWMetricException e) {
			System.err.println("main: error - " + e.getMessage());
			System.exit(1);
		}
		test.testInt();
		System.out.println("main: Done at " + getUTCTime());
	}

	private void testInt() {
		int metricValue = 0;
		for (int i = 0; i < 30; i++) {
			//monitor.putCWTimingMetric(METRIC_NAME_TRANS_X1, metricValue, "success");
			metricValue += 50;
			if (metricValue > 100) {
				metricValue = 0;
			}
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
			}
		}
	}

	private static void showUTCTime() {
		SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		f.setTimeZone(TimeZone.getTimeZone("UTC"));
		System.out.println(f.format(new Date()));
	}

	private static String getUTCTime() {
		SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		f.setTimeZone(TimeZone.getTimeZone("UTC"));
		return f.format(new Date());
	}
}
