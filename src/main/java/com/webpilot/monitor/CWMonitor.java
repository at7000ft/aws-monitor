/**
 * <p>
 * Title: CWMonitor.java
 * </p>
 * <p>
 * Description: Main WP AWS Monitoring Library class. CWMonitor creates a concurrent BlockingQueue and
 * thread pool listening for AWS CloudWatch metric inserts which are pushed to CloudWatch asynchronously
 * by the Threads.
 *
 * 
 * </p>
 * <p>
 * 2014
 * </p>
 * 
 * @author Rick Holland
 * 
 */
package com.webpilot.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.webpilot.monitor.exceptions.CWMetricException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CWMonitor implements CWMonitorIF {
    private String defaultNamespace;
    private AmazonCloudWatchClient cwClient = null;
    // Used to allow testing on non-EC2 instance
    private static final String US_WEST2_REGION = "us-west-2";
    private static String ec2InstanceId = null;
    private static String regionStr = null;
    private static String stackName = null;
    private BlockingQueue<Runnable> queue = null;
    private ThreadPoolExecutor executor = null;
    private Map<String, MetricAccumulator> accMap = null;
    // Map to maintain all metrics that are being passed in by using application, data in these is not used, only metric
    // names/namespaces.
    private Map<String, MetricAccumulator> zeroDataAccMap = new HashMap<String, MetricAccumulator>();;
    private Thread mpThread = null;
    private boolean cwMetricsPush = true;
    private int coreThreadPoolSize = 10;
    private int maxThreadPoolSize = 40;
    private static final Logger logger = LoggerFactory.getLogger(CWMonitor.class);
    private static final String versionId = "1.0.1";
    private static final String INSTANCE_ID_KEY = "instanceId";

    /**
     * Instantiates a new CW monitor.
     * 
     * @param namespace the namespace
     * @throws CWMetricException the cW metric exception
     */
    public CWMonitor(String namespace) throws CWMetricException {
        logger.info("Const versionId - " + versionId);
        awsInit(namespace);
        threadInit();
    }

    /**
     * Thread init.
     * 
     * @throws CWMetricException the CW metric exception
     */
    private void threadInit() throws CWMetricException {
        try {
            queue = new ArrayBlockingQueue<Runnable>(maxThreadPoolSize);
            executor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, Long.MAX_VALUE, TimeUnit.SECONDS,
                        queue);
            accMap = new HashMap<String, MetricAccumulator>();
            // Start MetricPushThread to average and push all metric data collected in accMap to CloudWatch on time
            // interval
            mpThread = new Thread(new MetricPushThread(this));
            mpThread.setDaemon(true);
            mpThread.isDaemon();
            mpThread.start();
        } catch (Exception e) {
            logger.warn("Init error -  " + e.getMessage());
            throw new CWMetricException("Init error - " + e.getMessage());
        }
    }

    /**
     * Aws init.
     * 
     * @param namespace the default namespace
     * @throws CWMetricException the CW metric exception
     */
    private void awsInit(String namespace) throws CWMetricException {
        try {
            // Need to concantenate CF stack name to this
            this.defaultNamespace = namespace;
            ClientConfiguration clientConfig = new ClientConfiguration();
            // Default is HTTPS but just to convince the security people
            clientConfig.setProtocol(Protocol.HTTPS);
            try {
                // Search for AWS credentials first in Environment Variables: AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
                // then Java System Properties: aws.accessKeyId and aws.secretKey
                // and last through Instance Metadata Service, which provides the credentials associated with the IAM
                // role for the EC2 instance.
                cwClient = new AmazonCloudWatchClient(clientConfig);
            } catch (AmazonClientException e) {
                logger.warn("AWS credentials not found in environment or using Instance Meta Data Service (IMDS)");
                throw new CWMetricException(
                            "AWS credentials not found in environment or using Instance Meta Data Service (IMDS)");
            }
            cwClient.setRegion(getCurrentRegion());
            getStackName();
            getInstanceId();
        } catch (Exception e) {
            logger.warn("Init error -  " + e.getMessage());
            throw new CWMetricException("init error - " + e.getMessage());
        }
    }

    /**
     * Gets the current region.
     * 
     * @return the current region
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     * @throws CWMetricException the cW metric exception
     */
    public static Region getCurrentRegion() throws IOException, InterruptedException, CWMetricException {
        return RegionUtils.getRegion(getRegionString());
    }

    /**
     * Gets the namespace string.
     * 
     * @param namespace the namespace
     * @return the namespace string
     */
    private String getNamespaceString(String namespace) {
        String ns;
        if (namespace == null) {
            ns = defaultNamespace;
        } else {
            ns = namespace;
        }
        if (ns.endsWith("/")) {
            return ns + stackName;
        } else {
            return ns + "-" + stackName;
        }
    }

    /*
     * @see CWMonitorIF#putCWMetric(java.lang.String, java.lang.String,
     * com.amazonaws.services.cloudwatch.model.StandardUnit, long, java.util.Map)
     */
    public boolean putCWMetric(String metricName, String namespace, StandardUnit unit, long value,
                Map<String, String> dimensions) {
        try {
            Date timestamp = new Date();
            String ns = getNamespaceString(namespace);
            logger.debug("Sending metric " + ns + "." + metricName + " with value " + value + " at UTC "
                        + getUTCTime(timestamp));
            PutMetricDataRequest req = new PutMetricDataRequest();
            // Add a collection of MetricDatum
            ArrayList<MetricDatum> metrics = new ArrayList<MetricDatum>();
            MetricDatum datum = new MetricDatum().withMetricName(metricName).withTimestamp(timestamp).withUnit(unit)
                        .withValue((double) value);
            if (dimensions != null) {
                List<Dimension> dims = datum.getDimensions();
                for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                    Dimension dim = new Dimension().withName(entry.getKey()).withValue(entry.getValue());
                    dims.add(dim);
                }
                datum.setDimensions(dims);
            }
            // Add metric to collection
            metrics.add(datum);
            req.setMetricData(metrics);
            req.setNamespace(ns);
            // AmazonCloudWatchClient is not threadsafe
            synchronized (cwClient) {
                cwClient.putMetricData(req);
            }
        } catch (Exception e) {
            logger.warn("Error -  " + e.getMessage());
            return false;
        }
        return true;
    }

    /*
     * @see CWMonitorIF#putCWAsyncMetric(java.lang.String, java.lang.String,
     * com.amazonaws.services.cloudwatch.model.StandardUnit, long, java.util.Map, boolean)
     */
    public boolean putCWAsyncMetric(String metricName, String namespace, StandardUnit unit, long value,
                Map<String, String> dimensions, boolean instIdDimension) {
        Map<String, String> dmap = null;
        try {
            if (dimensions != null) {
                // Add dimensions to treemap to sort keys
                dmap = new TreeMap<String, String>(dimensions);
            }
            if (instIdDimension) {
                if (dmap == null) {
                    dmap = new TreeMap<String, String>();
                }
                addInstanceIdDimension(dmap);
            }
            String ns = (namespace == null ? defaultNamespace : namespace);
            logger.debug("Loading metric " + ns + "." + metricName + " with value " + value);
            MetricData data = new MetricData(metricName, getNamespaceString(ns), unit, value, dmap, instIdDimension,
                        null);
            // Calls queueMetricData from threadpool thread
            MetricQueueProcessor proc = new MetricQueueProcessor(data, this);
            executor.submit(proc);
        } catch (Exception e) {
            logger.warn("Error -  " + e.getMessage());
            return false;
        }
        return true;
    }

    /*
     * @see CWMonitorIF#putCWAsyncMultiMetric(java.lang.String, java.lang.String, long,
     * CWMonitorIF.STATUS, boolean)
     */
    public boolean putCWAsyncMultiMetric(String metricName, String namespace, long msLatency, STATUS status,
                boolean instIdDimension) {
        try {
            Map<String, String> dmap = null;
            if (instIdDimension) {
                dmap = new TreeMap<String, String>();
                addInstanceIdDimension(dmap);
            }
            String ns = (namespace == null ? defaultNamespace : namespace);
            logger.debug("Loading metric " + ns + "." + metricName + " with msLatency " + msLatency);
            MetricData data = new MetricData(metricName, getNamespaceString(ns), StandardUnit.Milliseconds, msLatency,
                        dmap, instIdDimension, status);
            // Calls queueMetricData from threadpool thread
            MetricQueueProcessor proc = new MetricQueueProcessor(data, this);
            executor.submit(proc);
        } catch (Exception e) {
            logger.warn("Error -  " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Adds the instance id dimension.
     * 
     * @param dimensions the dimensions
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void addInstanceIdDimension(Map<String, String> dimensions) throws IOException {
        dimensions.put(INSTANCE_ID_KEY, getInstanceId());
    }

    /**
     * Push all metrics to cw. Called by MetricPushThread. Only allow accMap to be accessed by a single thread at a
     * time.
     * 
     * @return the list
     */
    public synchronized List<MetricAccumulator> snapshotMetrics() {
        if (accMap == null || accMap.size() == 0) {
            return null;
        }
        List<MetricAccumulator> metrics = new ArrayList<MetricAccumulator>(accMap.size());
        try {
            for (MetricAccumulator acc : accMap.values()) {
                logger.debug("Processing - " + acc);
                // Deep copy each MetricAccumulator instance for processing
                metrics.add(new MetricAccumulator(acc));
            }
        } catch (Exception e) {
            logger.warn("Error - " + e);
            e.printStackTrace();
        }
        accMap = new HashMap<String, MetricAccumulator>();
        return metrics;
    }

    /**
     * Push all metrics to cw. Called by MetricPushThread with snapshot of MetricAccumulator list.
     * 
     * @param metrics the metrics
     */
    public void pushAllMetricsToCW(List<MetricAccumulator> metrics) {
        try {
            // AmazonCloudWatchClient is not threadsafe
            synchronized (cwClient) {
                for (MetricAccumulator acc : metrics) {
                    logger.debug("Processing " + acc.getData().getMetricName() + " ave=" + acc.getAverage()
                                + " success%=" + acc.getSuccessPercentage());
                    if (acc.getType() == MetricAccumulator.ACCTYPE.SINGLE) {
                        PutMetricDataRequest req = acc.getPutMetricDataRequest();
                        cwClient.putMetricData(req);
                    } else {
                        PutMetricDataRequest req = acc.getMultiCountPutMetricDataRequest();
                        cwClient.putMetricData(req);
                        req = acc.getMultiLatencyPutMetricDataRequest();
                        cwClient.putMetricData(req);
                        req = acc.getMultiSuccessCountPutMetricDataRequest();
                        cwClient.putMetricData(req);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error - " + e);
        }
    }

    /**
     * If no new data has been cached by the application send zero latency, success % of 100, and a count of zero if
     * acc.getType() is multiple.
     * 
     */
    public void pushNoNewDataMetricsToCW() {
        try {
            if (zeroDataAccMap == null || zeroDataAccMap.size() == 0) {
                logger.debug("No metric data in zeroDataAccMap yet to push zero data metrics");
                return;
            }
            logger.debug("Pushing zero data messages");
            for (MetricAccumulator acc : zeroDataAccMap.values()) {
                PutMetricDataRequest req = acc.getMultiCountZeroDataPutMetricDataRequest();
                cwClient.putMetricData(req);
                req = acc.getMultiLatencyZeroDataPutMetricDataRequest();
                cwClient.putMetricData(req);
                req = acc.getMultiSuccessCountZeroDataPutMetricDataRequest();
                cwClient.putMetricData(req);
            }
        } catch (Exception e) {
            logger.warn("Error - " + e);
        }
    }

    /**
     * Queue metric data. Called by MetricQueueProcessor Thread.
     * 
     * @param data the data
     */
    public synchronized void queueMetricData(MetricData data) {
        try {
            MetricAccumulator acc = accMap.get(data.getMapCompoundKey());
            if (acc == null) {
                acc = new MetricAccumulator(data);
                accMap.put(data.getMapCompoundKey(), acc);
            }
            // Add another data record to the MetricData
            acc.insert(data);
            // Also check zeroDataAccMap for a MetricAccumulator with this MetricData and insert is not present. This
            // map is never cleared.
            if (zeroDataAccMap.get(data.getMapCompoundKey()) == null) {
                acc = new MetricAccumulator(data);
                zeroDataAccMap.put(data.getMapCompoundKey(), acc);
            }
            logger.debug("New MetricData added to - " + acc.toString());
        } catch (Exception e) {
            logger.warn("Error - " + e);
        }
    }

    /**
     * Gets the instance id.
     * 
     * @return wget -q -O - http://169.254.169.254/latest/meta-data/instance-id
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getInstanceId() throws IOException {
        if (ec2InstanceId != null)
            return ec2InstanceId;
        String metaUrl = "http://169.254.169.254/latest/meta-data/instance-id";
        String EC2Id = "";
        String inputLine;
        try {
            URL EC2MetaData = new URL(metaUrl);
            URLConnection EC2MD = EC2MetaData.openConnection();
            EC2MD.setConnectTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(EC2MD.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                EC2Id = inputLine;
            }
            in.close();
        } catch (Exception e) {
            logger.warn("Error getting instanceId setting to i-unknown");
            ec2InstanceId = "i-unknown";
            return ec2InstanceId;
        }
        ec2InstanceId = EC2Id;
        return EC2Id;
    }

    /**
     * Gets the region string.
     * 
     * @return the region string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getRegionString() throws IOException {
        if (regionStr != null)
            return regionStr;
        String metaUrl = "http://169.254.169.254/latest/dynamic/instance-identity/document";
        String rStr = "";
        String inputLine;
        try {
            URL EC2MetaData = new URL(metaUrl);
            URLConnection EC2MD = EC2MetaData.openConnection();
            EC2MD.setConnectTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(EC2MD.getInputStream()));
            StringBuilder jsonBld = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                jsonBld.append(inputLine);
            }
            String jsonStr = jsonBld.toString();
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(jsonStr).getAsJsonObject();
            rStr = obj.get("region").toString().replace("\"", "");
            in.close();
        } catch (Exception e) {
            regionStr = US_WEST2_REGION;
            logger.warn("Error getting regionStr setting to " + regionStr);
            return regionStr;
        }
        regionStr = rStr;
        logger.debug("Region string set to - " + regionStr);
        return regionStr;
    }

    /**
     * Gets the stack name.
     * 
     * @return the stack name
     * @throws CWMetricException the cW metric exception
     */
    public static String getStackName() throws CWMetricException {
        if (stackName != null) {
            return stackName;
        }
        String stackKey = "aws:cloudformation:stack-name";
        String instId = null;
        try {
            instId = getInstanceId();
            List<Tag> tags = getTags(instId);
            for (Tag tag : tags) {
                if (tag.getKey().equals(stackKey)) {
                    stackName = tag.getValue();
                    return stackName;
                }
            }
        } catch (Exception e) {
            logger.warn("Error getting stack-name   - " + e.getMessage() + " Setting stackname to UnknownStack");
            stackName = "UnknownStack";
            return stackName;
        }
        throw new CWMetricException("Error getting stack-name, " + stackKey + " not found");
    }

    /**
     * Gets the tags.
     * 
     * @param instanceId the instance id
     * @return the tags
     * @throws IllegalArgumentException the illegal argument exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     * @throws CWMetricException the cW metric exception
     */
    private static List<Tag> getTags(String instanceId) throws IllegalArgumentException, IOException,
                InterruptedException, CWMetricException {
        logger.debug("Looking for instId - " + instanceId);
        // ClientConfiguration clientConfig = new ClientConfiguration();
        AmazonEC2 ec2 = new AmazonEC2Client();
        ec2.setRegion(getCurrentRegion());
        // DescribeTagsResult result = ec2.describeTags();
        // logger.debug("getTags: DescribeTagsResult - " + result);
        DescribeInstancesRequest instReq = new DescribeInstancesRequest();
        instReq.withInstanceIds(instanceId);
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(instReq);
        // logger.debug("getTags: describeInstancesResult - " + describeInstancesResult);
        List<Reservation> reservations = describeInstancesResult.getReservations();
        for (Reservation reservation : reservations) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getInstanceId().equals(instanceId))
                    return instance.getTags();
            }
        }
        logger.warn("InstanceId - " + instanceId + " not found");
        throw new CWMetricException("Error instanceId - " + instanceId + " not found");
    }

    /**
     * Gets the uTC time.
     * 
     * @param date the date
     * @return the uTC time
     */
    private static String getUTCTime(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(date);
    }

    /**
     * Close.
     */
    public void close() {
        logger.debug("Shutting down all threads");
        // Gracefully shutdown thread pool executor. All submitted tasks will
        // run to completion, no new tasks will be accepted.
        executor.shutdown();
        // Wait for all submitted tasks to complete. Handle InterruptedException
        // if thread pool executor is interrupted.
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        // Notify MetricPushThread it time to do a final push and go away
        cwMetricsPush = false;
        // Force mpThread out of its while loop and exit
        mpThread.interrupt();
        try {
            mpThread.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * Checks if is cw metrics push.
     * 
     * @return the cwMetricsPush
     */
    public boolean isCwMetricsPush() {
        return cwMetricsPush;
    }
}
