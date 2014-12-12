/**
 * <p>
 * Title: MetricData.java
 * </p>
 * <p>
 * Description: Dumb container for metric data as passed in from client applications.
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

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.webpilot.monitor.CWMonitorIF.STATUS;

/**
 * The Class MetricData.
 */
public class MetricData {
    private String metricName;
    private String namespace;
    private StandardUnit unit;
    private long value;
    private Map<String, String> dimensions = new HashMap<String, String>();
    private boolean instIdDimension;
    private STATUS status = null;

    /**
     * Instantiates a new metric data.
     * 
     * @param metricName the metric name
     * @param namespace the namespace
     * @param unit the unit
     * @param value the value
     * @param dimensions the dimensions
     * @param instIdDimension the inst id dimension
     * @param status the status
     */
    public MetricData(String metricName, String namespace, StandardUnit unit, long value,
                Map<String, String> dimensions, boolean instIdDimension, STATUS status) {
        super();
        this.metricName = metricName;
        this.namespace = namespace;
        this.unit = unit;
        this.value = value;
        this.dimensions = dimensions;
        this.instIdDimension = instIdDimension;
        this.status = status;
    }

    /**
     * Copy constructor. Instantiates a new metric data.
     * 
     * @param orgData the org data
     */
    public MetricData(MetricData orgData) {
        this.metricName = new String(orgData.getMetricName());
        this.namespace = new String(orgData.getNamespace());
        this.unit = orgData.getUnit();
        this.value = orgData.getValue();
        if (orgData.getDimensions() != null) {
            this.dimensions.putAll(orgData.getDimensions());
        }
        this.instIdDimension = orgData.isInstIdDimension();
        this.status = orgData.getStatus();
    }

    /**
     * Return a key to be used for referencing MetricAccumulator instances from a Map
     * 
     * @return
     */
    public String getMapCompoundKey() {
        return namespace + ":" + metricName;
    }

    /**
     * Gets the status.
     * 
     * @return the status
     */
    public STATUS getStatus() {
        return status;
    }

    /**
     * Sets the status.
     * 
     * @param status the new status
     */
    public void setStatus(STATUS status) {
        this.status = status;
    }

    /**
     * Gets the metric name.
     * 
     * @return the metricName
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Sets the metric name.
     * 
     * @param metricName the new metric name
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Gets the namespace.
     * 
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace.
     * 
     * @param namespace the new namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Gets the unit.
     * 
     * @return the unit
     */
    public StandardUnit getUnit() {
        return unit;
    }

    /**
     * Sets the unit.
     * 
     * @param unit the new unit
     */
    public void setUnit(StandardUnit unit) {
        this.unit = unit;
    }

    /**
     * Gets the value.
     * 
     * @return the value
     */
    public long getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value the new value
     */
    public void setValue(long value) {
        this.value = value;
    }

    /**
     * Gets the dimensions.
     * 
     * @return the dimensions
     */
    public Map<String, String> getDimensions() {
        return dimensions;
    }

    /**
     * Sets the dimensions.
     * 
     * @param dimensions the dimensions
     */
    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * Checks if is inst id dimension.
     * 
     * @return the instIdDimension
     */
    public boolean isInstIdDimension() {
        return instIdDimension;
    }

    /**
     * Sets the inst id dimension.
     * 
     * @param instIdDimension the new inst id dimension
     */
    public void setInstIdDimension(boolean instIdDimension) {
        this.instIdDimension = instIdDimension;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "MetricData [metricName=" + metricName + ", namespace=" + namespace + ", unit=" + unit + ", value="
                    + value + ", status=" + status + ", dimensions=" + dimensions + ", instIdDimension="
                    + instIdDimension + "]";
    }
}
