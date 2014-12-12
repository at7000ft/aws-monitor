/**
 * <p> Title: CWMetricException.java </p>
 * <p> Description:   
 *
 * </p>
 * <p> 2008</p>
 * @author Rick Holland
 *
 *
 */ 

package com.webpilot.monitor.exceptions;

public class CWMetricException extends Exception {
	/**
	 * 
	 */
	public CWMetricException() {

	}

	/**
	 * @param arg0
	 */
	public CWMetricException(String arg0) {
		super(arg0);

	}

	/**
	 * @param arg0
	 */
	public CWMetricException(Throwable arg0) {
		super(arg0);

	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public CWMetricException(String arg0, Throwable arg1) {
		super(arg0, arg1);

	}
}
