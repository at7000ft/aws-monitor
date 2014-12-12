/**
 * <p>
 * Title: MetricAccumulator.java
 * </p>
 * <p>
 * Description: Aggregates multiple MetricData data sets for the same metric. Calculates average value and success
 * percentage when asked.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * The Class MetricAccumulator.
 */
public class MetricAccumulator {
    private MetricData data;
    private long totalCount = 0;
    private long successCount = 0;
    private double valueSum = 0.0;
    private ACCTYPE type;
    private static final String MULTI_LATENCY_SUFFIX = "-latency";
    private static final String MULTI_TCOUNT_SUFFIX = "-tcount";
    private static final String MULTI_SPERCENT_SUFFIX = "-spercent";
    private static final Logger logger = LoggerFactory.getLogger(MetricAccumulator.class);

    /**
     * The Enum ACCTYPE.
     */
    public static enum ACCTYPE {
        SINGLE, MULTI
    };

    /**
     * Instantiates a new metric accumulator.
     * 
     * @param data the data
     */
    public MetricAccumulator(MetricData data) {
        super();
        this.data = data;
        if (data.getStatus() == null) {
            this.type = ACCTYPE.SINGLE;
        } else {
            this.type = ACCTYPE.MULTI;
        }
    }

    /**
     * Copy constructor. Instantiates a new metric accumulator.
     * 
     * @param org the org
     */
    public MetricAccumulator(MetricAccumulator org) {
        this.data = new MetricData(org.getData());
        this.totalCount = org.getTotalCount();
        this.successCount = org.getSuccessCount();
        this.valueSum = org.getValueSum();
        this.type = org.getType();
    }

    /**
     * Insert metric data.
     * 
     * @param data the data
     */
    public void insert(MetricData data) {
        if (data.getStatus() == CWMonitorIF.STATUS.SUCCESS) {
            this.successCount++;
        }
        this.totalCount++;
        this.valueSum += data.getValue();
        if (this.successCount >= Long.MAX_VALUE || this.totalCount >= Long.MAX_VALUE
                    || this.successCount >= Double.MAX_VALUE) {
            logger.warn("Count exceeds MAX_VALUE, resetting all counts to zero for " + data.getMetricName());
            totalCount = 0;
            successCount = 0;
            valueSum = 0.0;
        }
    }

    /**
     * Compose and return a PutMetricDataRequest that can be sent to CloudWatch. For SINGLE ACCTYPE metrics only. Adds
     * average value into request.
     * 
     * @return the put metric data request
     */
    public PutMetricDataRequest getPutMetricDataRequest() {
        return getPutMetricDataRequest(getAverage(), data.getUnit(), data.getMetricName());
    }

    /**
     * Compose and return a PutMetricDataRequest that can be sent to CloudWatch. For SINGLE ACCTYPE metrics only. Adds
     * average value into request.
     *
     * @param value the value
     * @param unit the unit
     * @param metricName the metric name
     * @return the put metric data request
     */
    public PutMetricDataRequest getPutMetricDataRequest(double value, StandardUnit unit, String metricName) {
        Date timestamp = new Date();
        PutMetricDataRequest req = new PutMetricDataRequest();
        // Add a collection of MetricDatum
        ArrayList<MetricDatum> metrics = new ArrayList<MetricDatum>();
        MetricDatum datum = new MetricDatum().withMetricName(metricName).withTimestamp(timestamp).withUnit(unit)
                    .withValue(value);
        if (data.getDimensions() != null) {
            List<Dimension> dims = datum.getDimensions();
            for (Map.Entry<String, String> entry : data.getDimensions().entrySet()) {
                Dimension dim = new Dimension().withName(entry.getKey()).withValue(entry.getValue());
                dims.add(dim);
            }
            datum.setDimensions(dims);
        }
        // Add metric to collection
        metrics.add(datum);
        req.setMetricData(metrics);
        req.setNamespace(data.getNamespace());
        return req;
    }

    /**
     * Compose and return a PutMetricDataRequest that can be sent to CloudWatch. For MULTI ACCTYPE total count metric
     * only.
     * 
     * @return the put metric data request
     */
    public PutMetricDataRequest getMultiCountPutMetricDataRequest() {
        return getPutMetricDataRequest(totalCount, StandardUnit.Count, data.getMetricName() + MULTI_TCOUNT_SUFFIX);
    }
    
    /**
     * Gets the multi count zero data put metric data request.
     *
     * @return the multi count zero data put metric data request
     */
    public PutMetricDataRequest getMultiCountZeroDataPutMetricDataRequest() {
        return getPutMetricDataRequest(0, StandardUnit.Count, data.getMetricName() + MULTI_TCOUNT_SUFFIX);
    }

    /**
     * Compose and return a PutMetricDataRequest that can be sent to CloudWatch. For MULTI ACCTYPE success count metric
     * only.
     * 
     * @return the put metric data request
     */
    public PutMetricDataRequest getMultiSuccessCountPutMetricDataRequest() {
        return getPutMetricDataRequest(getSuccessPercentage(), StandardUnit.Percent, data.getMetricName()
                    + MULTI_SPERCENT_SUFFIX);
    }
    
    /**
     * Gets the multi success count zero data put metric data request.
     *
     * @return the multi success count zero data put metric data request
     */
    public PutMetricDataRequest getMultiSuccessCountZeroDataPutMetricDataRequest() {
        return getPutMetricDataRequest(100.0d, StandardUnit.Percent, data.getMetricName()
                    + MULTI_SPERCENT_SUFFIX);
    }

    /**
     * Compose and return a PutMetricDataRequest that can be sent to CloudWatch. For MULTI ACCTYPE latency metric only.
     * 
     * @return the put metric data request
     */
    public PutMetricDataRequest getMultiLatencyPutMetricDataRequest() {
        return getPutMetricDataRequest(getAverage(), StandardUnit.Milliseconds, data.getMetricName()
                    + MULTI_LATENCY_SUFFIX);
    }
    
    /**
     * Gets the multi latency zero data put metric data request.
     *
     * @return the multi latency zero data put metric data request
     */
    public PutMetricDataRequest getMultiLatencyZeroDataPutMetricDataRequest() {
        return getPutMetricDataRequest(0.0d, StandardUnit.Milliseconds, data.getMetricName()
                    + MULTI_LATENCY_SUFFIX);
    }

    /**
     * Gets the average.
     * 
     * @return the average
     */
    public double getAverage() {
        return (valueSum / totalCount);
    }

    /**
     * Gets the success percentage.
     * 
     * @return the success percentage
     */
    public double getSuccessPercentage() {
        return ((double) successCount) / ((double) totalCount) * 100.0;
    }

    /**
     * Gets the data.
     * 
     * @return the data
     */
    public MetricData getData() {
        return data;
    }

    /**
     * Sets the data.
     * 
     * @param data the new data
     */
    public void setData(MetricData data) {
        this.data = data;
    }

    /**
     * Gets the total count.
     * 
     * @return the totalCount
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Sets the total count.
     * 
     * @param totalCount the new total count
     */
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Gets the success count.
     * 
     * @return the successCount
     */
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Sets the success count.
     * 
     * @param successCount the new success count
     */
    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    /**
     * Gets the value sum.
     * 
     * @return the valueSum
     */
    public double getValueSum() {
        return valueSum;
    }

    /**
     * Sets the value sum.
     * 
     * @param valueSum the new value sum
     */
    public void setValueSum(double valueSum) {
        this.valueSum = valueSum;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public ACCTYPE getType() {
        return type;
    }

    /**
     * Sets the type.
     * 
     * @param type the new type
     */
    public void setType(ACCTYPE type) {
        this.type = type;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "MetricAccumulator [metric=" + data.getMapCompoundKey() + " totalCount=" + totalCount + ", successCount=" + successCount + ", valueSum="
                    + valueSum + ", type=" + type + "]";
    }
}
