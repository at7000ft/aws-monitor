/**
 * <p> Title: MetricData.java </p>
 * <p> Description:   
 *
 * </p>
 * <p> 2008</p>
 * @author Rick Holland
 *
 *
 */ 

package com.webpilot.monitor.model;

import java.util.Map;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class MetricData {
   // boolean putCWAsyncMetric(String metricName, String namespace, StandardUnit unit, long value, Map<String, String> dimensions,
     //           boolean instIdDimension);
    private String metricName;
    private String namespace;
    private StandardUnit unit;
    private long value;
    private Map<String, String> dimensions;
    private boolean instIdDimension;
    /**
     * @param metricName
     * @param namespace
     * @param unit
     * @param value
     * @param dimensions
     * @param instIdDimension
     */
    public MetricData(String metricName, String namespace, StandardUnit unit, long value,
                Map<String, String> dimensions, boolean instIdDimension) {
        super();
        this.metricName = metricName;
        this.namespace = namespace;
        this.unit = unit;
        this.value = value;
        this.dimensions = dimensions;
        this.instIdDimension = instIdDimension;
    }
    /**
     * @return the metricName
     */
    public String getMetricName() {
        return metricName;
    }
    /**
     * @param Set metricName  
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }
    /**
     * @param Set namespace  
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    /**
     * @return the unit
     */
    public StandardUnit getUnit() {
        return unit;
    }
    /**
     * @param Set unit  
     */
    public void setUnit(StandardUnit unit) {
        this.unit = unit;
    }
    /**
     * @return the value
     */
    public long getValue() {
        return value;
    }
    /**
     * @param Set value  
     */
    public void setValue(long value) {
        this.value = value;
    }
    /**
     * @return the dimensions
     */
    public Map<String, String> getDimensions() {
        return dimensions;
    }
    /**
     * @param Set dimensions  
     */
    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }
    /**
     * @return the instIdDimension
     */
    public boolean isInstIdDimension() {
        return instIdDimension;
    }
    /**
     * @param Set instIdDimension  
     */
    public void setInstIdDimension(boolean instIdDimension) {
        this.instIdDimension = instIdDimension;
    }
    
    
}
