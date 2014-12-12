aws-monitor
===========

A Java AWS CloudWatch custom metric library.

Setup
============

To setup, run the following to create a jar and add to maven repo:
```
$ git clone https://github.com/at7000ft/aws-monitor.git
$ cd aws-monitor
$ mvn clean install
```

Add the following dependency to your maven pom:
```
<dependency>
        <groupId>com.webpilot.monitor</groupId>
        <artifactId>aws-monitor</artifactId>
        <version>0.0.2-SNAPSHOT</version>
</dependency>
```
Usage
============

In your application define a namespace (a name that your CloudWatch metrics will be filed under,
along with CF stack name concatenated by the monitor library), a name for each metric
that you want to monitor, and create a CWMonitor instance:
```
// CloudWatch metric namespace '/MyCompany/MyProject/<app name>', all metrics added to CW using this CWMonitor instance will be categorized under this.
private static final String namespace = "/MyCompany/MyProject/TestCWMonitor";

// Define a constant for each operation (metric name) to be monitored
private static final String METRIC_NAME_TRANS_Z3 = "transz3";

//Create a monitor instance with namespace
CWMonitor monitor = new CWMonitor(namespace);
```

For each operation in your app that you want to time (measure latency metric) capture system time before and after:
```
// Get system time before operation starts (needed only for time latency measurements)
long startTime = System.currentTimeMillis();
// Invoke the operation to be monitored
status = doSomeTransaction();

// Get system time after operation complete (needed only for time latency measurements)
long endTime = System.currentTimeMillis();
```

Call putCWAsyncMetric to push the operation elapsed time to CloudWatch, with metric name, units, time, status and optional dimension map:
```
import com.amazonaws.services.cloudwatch.model.StandardUnit;

// Push millisecond timing metric to CW sync with operation duration (no exceptions will be thrown, returns true on success else false)
monitor.putCWMetric(METRIC_NAME_TRANS_Z3, null, StandardUnit.Milliseconds, endTime - startTime, null);
```

To allow CWMonitor logging output add a new appender to your application logback.xml:
```
<logger name="com.webpilot.monitor" level="<level>"  />
```

Sending a non-timing metric such as counts or percent will require only calling putCWMetric with the appropriate StandardUnit and data value.
* StandardUnits include (in part):
* Seconds
* Bytes
* Bits
* Percent
* Count
* Bytes/Second (bytes per second)
* Bits/Second (bits per second)
* Count/Second (counts per second)
* None (default when no unit is specified)

A dimension is a name/value pair that helps you to uniquely identify a metric. To define a different metrics you can add a Dimensions Map to the putCWMetric call (optional):
```
Map<String,String> dimensionsAtalla = new HashMap<String,String>();
dimensionsSunno.put("conn_peer", "sunno");
monitor.putCWMetric(METRIC_NAME_TRANS_Z3, null, StandardUnit.Milliseconds, endTime - startTime, dimensionsSunno);
```

Always call close() on CWMonitor before exiting an application to push all any accumulated data and kill thread:
```
monitor.close()
```

Go to the AWS Console under CloudWatch and view your metrics (find your namespace under "Custom Metrics").

Logging
=========
The monitor library logs using slf4j and will log based on your applications log configuration.

CloudWatch - IAM Role
=========
The AWS EC2 instances where your aws-monitor library runs will require an IAM role with a policy like
the following (ec2 access is required to access instanceId):

```
{
  "Statement": [
    {
      "Resource": "*",
      "Action": [
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:ListMetrics",
        "cloudwatch:PutMetricData"
      ],
      "Effect": "Allow"
    },
    {
      "Resource": "*",
      "Action": ["ec2:*"],
      "Effect": "Allow"
    }
  ]
}
```

