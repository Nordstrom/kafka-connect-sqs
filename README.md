# kafka-connect-sqs
The SQS connector plugin provides the ability to use AWS SQS queues as both a source (from an SQS queue into a Kafka topic) or sink (out of a Kafka topic into an SQS queue).

## Compatibility Matrix
|Connector version|Kafka Connect API|AWS SDK|
|:---|:---|:---|
|1.1.0|2.2.0|1.11.501|
|1.2.0|2.3.0|1.11.924|
|1.3.0|2.8.1|1.11.1034|
|1.4.0|3.1.1|1.12.241|
|1.5.0|3.3.2|1.12.409|

Due to a compatibility issue with [Apache httpcomponents](http://hc.apache.org/), connector versions 1.1.0 and earlier may not work with Kafka Connect versions greater than 2.2

# Building
You can build the connector with Maven using the standard lifecycle goals:
```
mvn clean
mvn package
```

## Source Connector

SQS source connector reads from an AWS SQS queue and publishes to a Kafka topic.

Required properties:
 * `topics`: Kafka topic to be written to.
 * `sqs.queue.url`: URL of the SQS queue to be read from.

Optional properties:
* `sqs.region`: AWS region of the SQS queue to be read from.
* `sqs.endpoint.url`: Override value for the AWS region specific endpoint.
* `sqs.max.messages`: Maximum number of messages to read from SQS queue for each poll interval. Range is 0 - 10 with default of 1.
* `sqs.wait.time.seconds`: Duration (in seconds) to wait for a message to arrive in the queue. Default is 1.
* `sqs.message.attributes.enabled`: If true, it gets the SQS MessageAttributes and inserts them as Kafka Headers (only string headers are currently supported). Default is false.
* `sqs.message.attributes.include.list`: The comma separated list of MessageAttribute names to be included, if empty it includes all the Message Attributes. Default is the empty string.

### Sample Configuration

```json
{
  "config": {
    "connector.class": "com.nordstrom.kafka.connect.sqs.SqsSourceConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "name": "sqs-source-chirps",
    "sqs.max.messages": "5",
    "sqs.queue.url": "https://sqs.<AWS_REGION>.amazonaws.com/<AWS_ACCOUNT>/chirps-q",
    "sqs.wait.time.seconds": "5",
    "topics": "chirps-t",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter"
  },
  "name": "sqs-source-chirps"
}
```

## Sink Connector

SQS sink connector reads from a Kafka topic and publishes to an AWS SQS queue.

Required properties:
 * `topics`: Kafka topic to be read from.
 * `sqs.queue.url`: URL of the SQS queue to be written to.

Optional properties:
* `sqs.region`: AWS region of the SQS queue to be written to.
* `sqs.endpoint.url`: Override value for the AWS region specific endpoint.
* `sqs.message.attributes.enabled`: If true, it gets the Kafka Headers and inserts them as SQS MessageAttributes (only string headers are currently supported). Default is false.
* `sqs.message.attributes.include.list`: The comma separated list of Header names to be included, if empty it includes all the Headers. Default is the empty string.

### Sample Configuration
```json
{
  "config": {
    "connector.class": "com.nordstrom.kafka.connect.sqs.SqsSinkConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "name": "sqs-sink-chirped",
    "sqs.queue.url": "https://sqs.<AWS_REGION>.amazonaws.com/<AWS_ACCOUNT>/chirps-q",
    "sqs.region": "<AWS_REGION>",
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

### AWS Assume Role Support

An SQS connector can assume a cross-account role to enable such features as Server Side Encryption of a queue:
* `sqs.credentials.provider.class=com.nordstrom.kafka.connect.auth.AWSAssumeRoleCredentialsProvider`: REQUIRED Class providing cross-account role assumption.
* `sqs.credentials.provider.role.arn`: REQUIRED AWS Role ARN providing the access.
* `sqs.credentials.provider.session.name`: REQUIRED Session name
* `sqs.credentials.provider.external.id`: OPTIONAL (but recommended) External identifier used by the `kafka-connect-sqs` when assuming the role.

Define the AWS IAM Role that `kafka-connect-sqs` will assume when writing to the queue (e.g., `kafka-connect-sqs-role`) with a Trust Relationship to a principal in a separate account:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::<AWS_ACCOUNT>:root"
      },
      "Action": "sts:AssumeRole",
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "my-queue-external-id"
        }
      }
    }
  ]
}
```

Define an SQS Queue Policy Document for the queue to allow `SendMessage`. An example policy is:

```json
{
  "Version": "2012-10-17",
  "Id": "arn:aws:sqs:us-west-2:<AWS_ACCOUNT>:my-queue/SQSDefaultPolicy",
  "Statement": [
    {
      "Sid": "kafka-connect-sqs-sendmessage",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::<AWS_ACCOUNT>:role/kafka-connect-sqs-role"
      },
      "Action": "sqs:SendMessage",
      "Resource": "arn:aws:sqs:us-west-2:<AWS_ACCOUNT>:my-queue"
    }
  ]
}
```

SQS sink connector configuration would then include the additional properties:

```
sqs.credentials.provider.class=com.nordstrom.kafka.connect.auth.AWSAssumeRoleCredentialsProvider
sqs.credentials.provider.role.arn=arn:aws:iam::<AWS_ACCOUNT>:role/kafka-connect-sqs-role
sqs.credentials.provider.session.name=my-queue-session
sqs.credentials.provider.external.id=my-queue-external-id
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

## Build the connector plugin

Build the connector jar file:

```shell
mvn clean package
```

## Run the connector using Docker Compose

Ensure you have `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables exported in your shell. Docker Compose will pass these values into the `connect` container.

Use the provided [Docker Compose](https://docs.docker.com/compose) file and run `docker-compose up`.

With the [Kafka Connect REST interface](https://docs.confluent.io/current/connect/references/restapi.html), verify the SQS sink and source connectors are installed and ready: `curl http://localhost:8083/connector-plugins`.

## AWS

The demo assumes you have an AWS account and valid credentials in ~/.aws/credentials as well as
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

## Create the connectors

The `source` connector configuration is defined in `demos/sqs-source-chirps.json]`, The `sink` connector configuration
is defined in `demos/sqs-sink-chirped.json`.  You will have to modify the `sqs.queue.url` parameter to reflect the
values noted when you created the queues.

Create the connectors using the Confluent CLI:

```shell
curl -XPOST -H 'Content-Type: application/json' http://localhost:8083/connectors -d @demos/sqs-source-chirps.json
curl -XPOST -H 'Content-Type: application/json' http://localhost:8083/connectors -d @demos/sqs-sink-chirped.json
```

## Send/receive messages

Using the AWS Console (or the AWS CLI), send a message to the `chirps-q`.

The source connector will read the message from the queue and write it to the `chirps-t` Kafka topic.

The `sink` connector will read the message from the topic and write it to the `chirped-q` queue.

Use the AWS Console (or the AWS CLI) to read your message from the `chirped-q`

