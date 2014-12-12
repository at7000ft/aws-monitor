/**
 * <p>
 * Title: TestCWCounts.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TestCWCounts {
	private static final String METRIC_NAME_TRANS_Y2 = "transy2";
	private static final String namespace = "WP/Project1/TestCWCounts";
	private CWMonitorIF monitor = null;
	private static final Logger logger = LoggerFactory.getLogger(TestCWCounts.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("main: Starting at " + getUTCTime());
		TestCWCounts test = new TestCWCounts();
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
		long count = 0;
		for (int i = 0; i < 30; i++) {
			//monitor.putCWCountMetric(METRIC_NAME_TRANS_Y2, count, "success");
			count += 50;
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
