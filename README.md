# kafka-connect-sqs
The SQS connector plugin provides the ability to use AWS SQS queues as both a source (from an SQS queue into a Kafka topic) or sink (out of a Kafka topic into an SQS queue).

## Supported Kafka and AWS versions
The `kafka-connect-sqs` connector has been tested with `connect-api:2.1.0` and `aws-java-sdk-sqs:1.11.452`

# Building
You can build the connector with Maven using the standard lifecycle goals:
```
mvn clean
mvn package
```

## Source Connector

A source connector reads from an AWS SQS queue and publishes to a Kafka topic.

A source connector configuration has two required fields:
 * `sqs.queue.url`: The URL of the SQS queue to be read from.
 * `topics`: The Kafka topic to be written to.

There are optional fields:
* `sqs.max.messages`: Maximum number of messages to read from SQS queue for each poll interval. Range is 0 - 10 with default of 1.
* `sqs.wait.time.seconds`: Duration (in seconds) to wait for a message to arrive in the queue. Default is 1.

### Sample Configuration

```json
{
  "config": {
    "connector.class": "com.nordstrom.kafka.connect.sqs.SqsSourceConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "name": "sqs-source-chirps",
    "sqs.max.messages": "5",
    "sqs.queue.url": "https://sqs.<SOME_REGION>.amazonaws.com/<SOME_ACCOUNT>/chirps-q",
    "sqs.wait.time.seconds": "5",
    "topics": "chirps-t",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
  },
  "name": "sqs-source-chirps"
}
```

## Sink Connector

A sink connector reads from a Kafka topic and publishes to an AWS SQS queue.

A sink connector configuration has two required fields:
 * `sqs.queue.url`: The URL of the SQS queue to be written to.
 * `topics`: The Kafka topic to be read from.

### Sample Configuration
```json
{
  "config": {
    "connector.class": "com.nordstrom.kafka.connect.sqs.SqsSinkConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "name": "sqs-sink-chirped",
    "sqs.queue.url": "https://sqs.<SOME_REGION>.amazonaws.com/<SOME_ACCOUNT>/chirps-q",
    "topics": "chirps-t",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
  },
  "name": "sqs-sink-chirped"
}
```


## AWS IAM Policies

The IAM Role that Kafka Connect is running under must have policies set for SQS resources in order
to read from or write to the target queues.

For a `sink` connector, the minimum actions required are:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "kafka-connect-sqs-sink-policy",
    "Effect": "Allow",
    "Action": [
      "sqs:SendMessage"
    ],
    "Resource": "arn:aws:sqs:*:*:*"
  }]
}
```

For a `source` connector, the minimum actions required are:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "kafka-connect-sqs-source-policy",
    "Effect": "Allow",
    "Action": [
      "sqs:DeleteMessage",
      "sqs:GetQueueUrl",
      "sqs:ListQueues",
      "sqs:ReceiveMessage"
    ],
    "Resource": "arn:aws:sqs:*:*:*"
  }]
}
```

# Running the Demo

The demo uses the Confluent Platform which can be downloaded here: https://www.confluent.io/download/

You can use either the Enterprise or Community version.

The rest of the tutorial assumes the Confluent Platform is installed at $CP and $CP/bin is on your PATH.

## AWS

The demo assumes you have an AWS account and have valid credentials in ~/.aws/credentials as well as
setting the `AWS_PROFILE` and `AWS_REGION` to appropriate values.

These are required so that Kafka Connect will have access to the SQS queues.

## The Flow

We will use the AWS Console to put a message into an SQS queue.  A source connector will read messages
from the queue and write the messages to a Kafka topic.  A sink connector will read messages from the
topic and write to a _different_ SQS queue.

```
 __               |  s  |       |  k  |       |  s  |
( o>  chirp  ---> |  q  |  ---> |  a  |  ---> |  q  |
///\              |  s  |   |   |  f  |   |   |  s  |
\V_/_             |_____|   |   |_____|   |   |_____|
                 chirps-q   |  chirps-t   |  chirped-q
                            |             |
                            |             |
                         source-        sink-
                        connector      connector
```

## Create AWS SQS queues

Create `chirps-q` and `chirped-q` SQS queues using the AWS Console.  Take note of the `URL:` values for each
as you will need them to configure the connectors later.

## Build the connector plugin

Build the connector jar file and copy to the the classpath of Kafka Connect:

```shell
mvn clean package
mkdir $CP/share/java/kafka-connect-sqs
cp target/kafka-connect-sqs-0.0.1.jar $CP/share/java/kafka-connect-sqs/
```

## Start Confluent Platform using the Confluent CLI

See https://docs.confluent.io/current/quickstart/ce-quickstart.html#ce-quickstart for more details.

```shell
$CP/bin/confluent start
```

## Create the connectors

The `source` connector configuration is defined in `demos/sqs-source-chirps.json]`, The `sink` connector configuration
is defined in `demos/sqs-sink-chirped.json`.  You will have to modify the `sqs.queue.url` parameter to reflect the
values noted when you created the queues.

Create the connectors using the Confluent CLI:

```shell
confluent load kafka-connect-sqs -d ./demos/sqs-source-chirps.json
confluent load kafka-connect-sqs -d ./demos/sqs-sink-chirped.json
```

## Send/receive messages

Using the AWS Console (or the AWS CLI), send a message to the `chirps-q`.

The source connector will read the message from the queue and write it to the `chirps-t` Kafka topic.

The `sink` connector will read the message from the topic and write it to the `chirped-q` queue.

Use the AWS Console (or the AWS CLI) to read your message from the `chirped-q`

## Cleaning up

Clean up by destroying the Confluent Platform to remove all messages and data stored on disk:

```shell
confluent destroy
```
