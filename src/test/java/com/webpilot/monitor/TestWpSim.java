/**
 * <p>
 * Title: TestWpSim.java
 * </p>
 * <p>
 * Description: A CWMonitor simple example test application. AWS_ACCESS_KEY and AWS_SECRET_KEY must be in runtime env to
 * run in eclipse, or in environment to run in command line (if not running on EC2 with IAM Role).
 * 
 * BUILD: mvn clean compile assembly:single
 * 
 * SCP: scp -i ~/.ssh/devKey.pem target/TestWpSim-0.0.1-jar-with-dependencies.jar ubuntu@(IP):~
 * 
 * RUN: ssh ubuntu@(IP) -i /Users/rholl00/.ssh/devKey.pem (ssh to dev kernel EC2) su bhnuser (pw bhnuser) java
 * -jar TestWpSim-0.0.1-jar-with-dependencies.jar
 * 
 * View IAM metadata on an EC2 instance using role name curl
 * http://169.254.169.254/latest/meta-data/iam/security-credentials
 * /WpCWRoleStackDEV-KSCWMailWriteAccessRole-15Y7018OR8X7L
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

import java.util.Random;
import java.util.Scanner;


public class TestWpSim {
    // CloudWatch metric namespace 'BHN/KS/<app name>', all metrics added to CW using this CWMonitor instance will be
    // categorized under this.
    private static final String KERNEL_NAMESPACE = "BHN/KS/Kernel";
    private static final String KEYSTONE_NAMESPACE = "BHN/KS";
    private static final String KEYSTONE_RECON_NAMESPACE = "BHN/KS/Recon";
    // Define a constant for each operation (metric name) to be monitored
    private static final String METRIC_NAME_ATALLA = "atalla";
    private static final String METRIC_NAME_KERNEL_UMS = "ums";
    private static final String METRIC_NAME_KERNEL_VISA = "visa";
    private static final String METRIC_NAME_KERNEL_CMF = "cmf";
    // Batch metric names
    private static final String METRIC_NAME_BATCH_JOB_START = "job-start"; // Once at job start
    private static final String METRIC_NAME_BATCH_RUN_STATUS = "run-status"; // Once at job end 0 or 1
    private static final String METRIC_NAME_BATCH_RUN_LATENCY = "run-latency"; // Once at job end
    private static final String METRIC_NAME_BATCH_ITEM = "item"; // Multi for each item processed
    private static final String SES_EMAIL_ADDR = "Rick.Holland@bhnetwork.com";
    // CWMonitor to push metrics to CW
    private CWMonitorIF monitor = null;
    private static int iterationCount = 1;
    private int asyncSleepInterval = 10000;
    private static final Logger logger = LoggerFactory.getLogger(TestWpSim.class);

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws CWMetricException
     */
    public static void main(String[] args) throws CWMetricException {
        String commandString = "";
        logger.info("main: Starting run");
        TestWpSim test = new TestWpSim();
        try {
            test.monitor = new CWMonitor(KERNEL_NAMESPACE);
        } catch (CWMetricException e) {
            logger.error("main: error - " + e.getMessage());
            System.exit(1);
        }
        Scanner scanner = new Scanner(System.in);
        while (!commandString.equals("e")) {
            test.showMenu();
            commandString = scanner.nextLine().trim();
            // Test for quit command
            if (commandString.equals("e")) {
                continue;
            }
            int intEntry = 0;
            try {
                intEntry = new Integer(commandString);
            } catch (NumberFormatException e) {
                System.out.println("Bad entry moron - " + commandString);
                continue;
            }
            System.out.println("Enter iteration count (default 1):");
            commandString = scanner.nextLine().trim();
            if (commandString.length() == 0)
                iterationCount = 1;
            else
                iterationCount = new Integer(commandString);
            switch (intEntry) {
            case 0:
                test.sendKernelAndWpMetrics();
                break;
            case 1:
                test.sendMultiMetrics(METRIC_NAME_KERNEL_UMS, null, iterationCount, 340);
                break;
            case 2:
                test.sendMultiMetrics(METRIC_NAME_KERNEL_VISA, null, iterationCount, 330);
                break;
            case 3:
                test.sendMultiMetrics(METRIC_NAME_KERNEL_CMF, null, iterationCount, 320);
                break;
            case 4:
                test.sendMultiMetrics(METRIC_NAME_ATALLA, KEYSTONE_NAMESPACE, iterationCount, 350);
                break;
            case 5:
                test.sendAllAlarmValue();
                break;
            case 6:
                test.sendBatchMetrics();
                break;
            case 7:
                test.sendSesEmail();
                break;
            case 8:
                test.sendIntermittentMultiMetrics(METRIC_NAME_KERNEL_UMS, null, iterationCount, 200);
                break;
            default:
                System.out.println("Bad entry moron");
            }
        }
        //
        test.monitor.close();
        logger.info("main: Done");
        System.exit(0);
    }

    private void showMenu() {
        System.out.println("Select metric to run:");
        System.out.println("\t0) Send Kernel and Wp metrics: ");
        System.out.println("\t1) Send Kernel UMS alarm metric values: ");
        System.out.println("\t2) Send Kernel VISA alarm metric values: ");
        System.out.println("\t3) Send Kernel CMF alarm metric values: ");
        System.out.println("\t4) Send Wp Atalla latency alarm metric values: ");
        System.out.println("\t5) Send All alarm metric values: ");
        System.out.println("\t6) Send Recon batch metrics: ");
        System.out.println("\t7) Send email: ");
        System.out.println("\t8) Send intermittent Kernel UMS alarm metric values: ");
        System.out.println("\te) exit: \n");
    }

    public void sendKernelAndWpMetrics() {
        for (int i = 0; i < iterationCount; i++) {
            sendMultiMetrics(METRIC_NAME_KERNEL_UMS, null, 1, 0);
            sendMultiMetrics(METRIC_NAME_KERNEL_VISA, null, 1, 0);
            sendMultiMetrics(METRIC_NAME_KERNEL_CMF, null, 1, 0);
            // sendMultiMetrics(METRIC_NAME_ATALLA, KEYSTONE_NAMESPACE, 1, 0);
        }
    }

    public void sendAllAlarmValue() {
        for (int i = 0; i < iterationCount; i++) {
            sendMultiMetrics(METRIC_NAME_KERNEL_UMS, null, 1, 340);
            sendMultiMetrics(METRIC_NAME_KERNEL_VISA, null, 1, 330);
            sendMultiMetrics(METRIC_NAME_KERNEL_CMF, null, 1, 320);
            // sendMultiMetrics(METRIC_NAME_ATALLA, KEYSTONE_NAMESPACE, 1, 350);
        }
    }

    public void sendMultiMetrics(String name, String namespace, int itCount, long latency) {
        boolean status = false;
        long lat = 0;
        for (int i = 0; i < itCount; i++) {
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
            if (latency > 0) {
                lat = latency;
            } else {
                lat = endTime - startTime;
            }
            // Push multi async metrics to processing queue
            monitor.putCWAsyncMultiMetric(name, namespace, lat, stat, false);
            if (iterationCount > 1) {
                try {
                    Thread.sleep(asyncSleepInterval);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void sendIntermittentMultiMetrics(String name, String namespace, int itCount, long latency) {
        boolean status = false;
        long lat = 0;
        for (int i = 0; i < itCount; i++) {
            long startTime = System.currentTimeMillis();
            try {
                // Invoke the operation to be monitored
                status = doSomeTransaction(60000, -200, 1000);
            } catch (Exception e) {
                logger.error("callDoSomeTransaction: error - " + e);
                return;
            }
            long endTime = System.currentTimeMillis();
            STATUS stat = status ? STATUS.SUCCESS : STATUS.FAILURE;
            if (latency > 0) {
                lat = latency;
            } else {
                lat = endTime - startTime;
            }
            // Push multi async metrics to processing queue
            monitor.putCWAsyncMultiMetric(name, namespace, lat, stat, false);
            if (iterationCount > 1) {
                try {
                    Thread.sleep(asyncSleepInterval);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void sendBatchMetrics() {
        // boolean status = false;
        // long runStartTime = System.currentTimeMillis();
        // Send job-start
        // monitor.putCWMetric(METRIC_NAME_BATCH_JOB_START, KEYSTONE_RECON_NAMESPACE, StandardUnit.Count, 1, null);
        // Send multiple item latency for multi metric
        // for (int i = 0; i < 10; i++) {
        // try {
        // Thread.sleep(asyncSleepInterval);
        // } catch (InterruptedException e) {
        // }
        // long startTime = System.currentTimeMillis();
        // try {
        // // Invoke the operation to be monitored
        // status = doSomeTransaction();
        // } catch (Exception e) {
        // logger.error("callDoSomeTransaction: error - " + e);
        // return;
        // }
        // long endTime = System.currentTimeMillis();
        // STATUS stat = status ? STATUS.SUCCESS : STATUS.FAILURE;
        //
        // // Push multi async metrics to processing queue
        // monitor.putCWAsyncMultiMetric(METRIC_NAME_BATCH_ITEM, KEYSTONE_RECON_NAMESPACE, endTime - startTime, stat,
        // false);
        //
        // }
        //
        // long runEndTime = System.currentTimeMillis();
        // Send run-status 0=success 1=failure
        monitor.putCWMetric(METRIC_NAME_BATCH_RUN_STATUS, KEYSTONE_RECON_NAMESPACE, StandardUnit.Count, 0, null);
        // Send run-latency
        // monitor.putCWMetric(METRIC_NAME_BATCH_RUN_LATENCY, KEYSTONE_RECON_NAMESPACE,
        // StandardUnit.Microseconds,runEndTime-runStartTime, null);
    }

    /*
     * You use AWS access keys when you send an email using the Amazon SES API, and SMTP credentials when you send an
     * email using the Amazon SES SMTP interface. IAM User Name Smtp Username Smtp Password ses-smtp-user.rholl00
     * AKIAJNZUU36NC4NISWRQ AlSruTXoEcMEhPLvt5/lL3T7c9ZbQpTSn+HjqanCKTlG
     */
    private void sendSesEmail() {
        String FROM = SES_EMAIL_ADDR; // This address must be verified.
        String TO = SES_EMAIL_ADDR;
        String instId = null;
        String SUBJECT = "Wp Email From Amazon SES test ";
        logger.debug("sendSesEmail: Sending email to " + TO);
        try {
            instId = CWMonitor.getInstanceId();
            String BODY = "This email was sent from an EC2 instance in the AWS CLOUD through SES from the TestWpSim app using AWS SDK for Java from EC2 instance id -  "
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
            long sleepDuration = baseLatencyValue + getRandomBetweenPlusMinus(-100, 99);
            Thread.sleep((long) sleepDuration);
        } catch (InterruptedException e) {
        }
        // Return true most of the time
        return Math.random() < 0.99;
    }

    private boolean doSomeTransaction(long baseLatencyValue, int min, int max) {
        try {
            long sleepDuration = baseLatencyValue + getRandomBetweenPlusMinus(min, max);
            logger.debug("doSomeTransaction: sleepDuration-" + sleepDuration);
            Thread.sleep((long) sleepDuration);
        } catch (InterruptedException e) {
        }
        // Return true most of the time
        return Math.random() < 0.99;
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
