/**
 * <p>
 * Title: TestCWMonitorWithEmail.java
 * </p>
 * <p>
 * Description: A CWMonitor simple example test application.
 * AWS_ACCESS_KEY and AWS_SECRET_KEY must be in runtime env to run in eclipse, or in environment to run in command line (if not running on EC2 with IAM Role).
 * 
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
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import com.webpilot.monitor.CWMonitorIF.STATUS;
import com.webpilot.monitor.exceptions.CWMetricException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class TestCWMonitorWithEmail {
    // CloudWatch metric namespace 'BHN/KS/<app name>', all metrics added to CW using this CWMonitor instance will be
    // categorized under this.
    private static final String namespace = "BHN/KS/TestCWMonitorWithEmail";
    private static final String keystoneNamespace = "BHN/KS";
    // Define a constant for each operation (metric name) to be monitored
    private static final String METRIC_NAME_SYNC_TRANS_Z5 = "synctransz5";
    private static final String METRIC_NAME_TRANS_Z6 = "transz6";
    private static final String METRIC_NAME_ASYNC_Z6 = "asyncz6";
    private static final String METRIC_NAME_NN_ASYNC_Z9 = "nnasyncz69";
    private static final String METRIC_NAME_MULTASYNC_Z6 = "multasyncz8";
    // CWMonitor to push metrics to CW
    private CWMonitorIF monitor = null;
    private static final Logger logger = LoggerFactory.getLogger(TestCWMonitorWithEmail.class);

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws CWMetricException
     */
    public static void main(String[] args) throws CWMetricException {
        logger.info("main: Starting");
        TestCWMonitorWithEmail test = new TestCWMonitorWithEmail();
        try {
            test.monitor = new CWMonitor(namespace);
        } catch (CWMetricException e) {
            logger.error("main: error - " + e.getMessage());
            System.exit(1);
        }
        test.sendSesEmail();
        test.doSyncMetric();
        test.doSyncMetricWithDimension();
        test.doAsyncSingleMetric();
        test.doAsyncMultiMetric();
        test.doAsyncSingleMetricWithNonDefaultNamespace();
        test.doMetricCallTiming();
        test.doAsyncSingleMetricInstId();
        test.doAsyncMultiMetricInstId();
        //
        test.monitor.close();
        logger.info("main: Done");
    }

    public void doSyncMetric() {
        // Get the system time before calling operation
        long startTime = System.currentTimeMillis();
        try {
            // Invoke the operation to be monitored
            doSomeTransaction();
        } catch (Exception e) {
            logger.error("callDoSomeTransaction: error - " + e);
            return;
        }
        // Get system time after operation complete
        long endTime = System.currentTimeMillis();
        // Push sync timing metric to CW with operation duration
        monitor.putCWMetric(METRIC_NAME_SYNC_TRANS_Z5, null, StandardUnit.Milliseconds, endTime - startTime, null);
    }

    public void doSyncMetricWithDimension() {
        // Get the system time before calling operation
        long startTime = System.currentTimeMillis();
        try {
            // Invoke the operation to be monitored
            doSomeTransaction();
        } catch (Exception e) {
            logger.error("callDoSomeTransaction: error - " + e);
            return;
        }
        // Get system time after operation complete
        long endTime = System.currentTimeMillis();
        // Push sync timing metric to CW with operation duration and dimension
        Map<String, String> dimensionsAtalla = new HashMap<String, String>();
        dimensionsAtalla.put("conn_peer", "atalla");
        monitor.putCWMetric(METRIC_NAME_SYNC_TRANS_Z5, null, StandardUnit.Milliseconds, endTime - startTime,
                    dimensionsAtalla);
    }

    public void doAsyncSingleMetric() {
        // Push async metrics to processing queue
        for (int i = 0; i < 70; i++) {
            long startTime = System.currentTimeMillis();
            try {
                // Invoke the operation to be monitored
                doSomeTransaction();
            } catch (Exception e) {
                logger.error("callDoSomeTransaction: error - " + e);
                return;
            }
            long endTime = System.currentTimeMillis();
            monitor.putCWAsyncMetric(METRIC_NAME_ASYNC_Z6, null, StandardUnit.Milliseconds, endTime - startTime, null,
                        false);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }
    
    public void doAsyncSingleMetricInstId() {
        // Push async metrics to processing queue
     
            long startTime = System.currentTimeMillis();
            try {
                // Invoke the operation to be monitored
                doSomeTransaction();
            } catch (Exception e) {
                logger.error("callDoSomeTransaction: error - " + e);
                return;
            }
            long endTime = System.currentTimeMillis();
            monitor.putCWAsyncMetric(METRIC_NAME_ASYNC_Z6, null, StandardUnit.Milliseconds, endTime - startTime, null,
                        true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        
    }

    
    public void doAsyncMultiMetric() {
        boolean status = false;
        for (int i = 0; i < 70; i++) {
            long startTime = System.currentTimeMillis();
            try {
                // Invoke the operation to be monitored
                status = doSomeTransaction();
            } catch (Exception e) {
                logger.error("callDoSomeTransaction: error - " + e);
                return;
            }
            long endTime = System.currentTimeMillis();
            STATUS stat = status ? STATUS.SUCCESS : STATUS.FAILURE;
            // Push multi async metrics to processing queue
            monitor.putCWAsyncMultiMetric(METRIC_NAME_MULTASYNC_Z6, null, endTime - startTime, stat, false);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void doAsyncMultiMetricInstId() {
        boolean status = false;
        
            long startTime = System.currentTimeMillis();
            try {
                // Invoke the operation to be monitored
                status = doSomeTransaction();
            } catch (Exception e) {
                logger.error("callDoSomeTransaction: error - " + e);
                return;
            }
            long endTime = System.currentTimeMillis();
            STATUS stat = status ? STATUS.SUCCESS : STATUS.FAILURE;
            // Push multi async metrics to processing queue
            monitor.putCWAsyncMultiMetric(METRIC_NAME_MULTASYNC_Z6, null, endTime - startTime, stat, true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        
    }
    public void doAsyncSingleMetricWithNonDefaultNamespace() {
        // Push async metric with non-default namespace
        monitor.putCWAsyncMetric(METRIC_NAME_NN_ASYNC_Z9, keystoneNamespace, StandardUnit.Milliseconds, 300, null,
                    false);
    }

    public void doMetricCallTiming() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            monitor.putCWMetric(METRIC_NAME_SYNC_TRANS_Z5, null, StandardUnit.Milliseconds, 222, null);
        }
        long endTime = System.currentTimeMillis();
        logger.debug("\ndoMetricCallTiming: putCWMetric ave time - " + (endTime - startTime) / 10 + " ms");
        //
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            monitor.putCWAsyncMetric(METRIC_NAME_ASYNC_Z6, null, StandardUnit.Milliseconds, 222, null, false);
        }
        endTime = System.currentTimeMillis();
        logger.debug("\ndoMetricCallTiming: putCWAsyncMetric ave time - " + (endTime - startTime) / 10 + " ms");
    }

    /*
     * You use AWS access keys when you send an email using the Amazon SES API, and SMTP credentials when you send an
     * email using the Amazon SES SMTP interface. IAM User Name Smtp Username Smtp Password ses-smtp-user.rholl00
     * AKIAJNZUU36NC4NISWRQ AlSruTXoEcMEhPLvt5/lL3T7c9ZbQpTSn+HjqanCKTlG
     */
    private void sendSesEmail() {
        String FROM = "Rick.Holland@bhnetwork.com"; // This address must be verified.
        String TO = "Rick.Holland@bhnetwork.com";
        String instId = null;
        String SUBJECT = "Amazon SES test (AWS SDK for Java)";
        logger.debug("sendSesEmail: Sending email to " + TO);
        try {
            instId = CWMonitor.getInstanceId();
            String BODY = "This email was sent through Amazon SES by using the AWS SDK for Java from EC2 instance "
                        + instId + " and stackname " + CWMonitor.getStackName();
            // Construct an object to contain the recipient address.
            Destination destination = new Destination().withToAddresses(new String[] { TO });
            // Create the subject and body of the message.
            Content subject = new Content().withData(SUBJECT);
            Content textBody = new Content().withData(BODY);
            Body body = new Body().withText(textBody);
            // Create a message with the specified subject and body.
            Message message = new Message().withSubject(subject).withBody(body);
            // Assemble the email.
            SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination)
                        .withMessage(message);
            logger.debug("Attempting to send an email through Amazon SES by using the AWS SDK for Java...");
            // Instantiate an Amazon SES client, which will make the service call with the env or IAM AWS credentials,
            // no SMTP credentials needed.
            AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient();
            // Send the email.
            client.sendEmail(request);
            logger.debug("Email sent!");
        } catch (Exception ex) {
            logger.error("The email was not sent.");
            logger.error("Error message: " + ex.getMessage());
        }
        logger.debug("sendSesEmail: Done");
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
        // Return true most of the time
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
