# kafka-connect-sqs

The SQS connector plugin provides the ability to use AWS SQS queues as both a source (from an SQS
queue into a Kafka topic) or sink (out of a Kafka topic into an SQS queue).

## Compatibility matrix

|Connector version|Kafka Connect API|AWS SDK|
|:---|:---|:---|
|1.4|3.1.1|1.12.241|
|1.5|3.3.2|1.12.409|
|1.6|3.4.1|1.12.669|

## Building the distributable

You can build the connector with Maven using the standard lifecycle goals:

```sh
mvn clean
mvn package
```

## Source connector

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
* `sqs.message.attributes.partition.key`: The name of a single AWS SQS MessageAttribute to use as the partition key. If this is not specified, default to the SQS message ID as the partition key.

### Sample IAM policy

When using this connector, ensure the authentication principal has privileges to read messages from
the SQS queue.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "kafka-connect-sqs-source",
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

## Sink connector

SQS sink connector reads from a Kafka topic and publishes to an AWS SQS queue.

Required properties:
* `topics`: Kafka topic to be read from.
* `sqs.queue.url`: URL of the SQS queue to be written to.

Optional properties:
* `sqs.region`: AWS region of the SQS queue to be written to.
* `sqs.endpoint.url`: Override value for the AWS region specific endpoint.
* `sqs.message.attributes.enabled`: If true, it gets the Kafka Headers and inserts them as SQS MessageAttributes (only string headers are currently supported). Default is false.
* `sqs.message.attributes.include.list`: The comma separated list of Header names to be included, if empty it includes all the Headers. Default is the empty string.

### Sample SQS queue policy

Define a corresponding SQS queue policy that allows the connector to send messages to the SQS queue:

```json
{
  "Version": "2012-10-17",
  "Id": "arn:aws:sqs:us-west-2:<AWS_ACCOUNT>:my-queue/SQSDefaultPolicy",
  "Statement": [
    {
      "Sid": "kafka-connect-sqs-sink",
      "Effect": "Allow",
      "Principal": {
        "AWS": "<Your principal ARN>"
      },
      "Action": "sqs:SendMessage",
      "Resource": "arn:aws:sqs:us-west-2:<AWS_ACCOUNT>:my-queue"
    }
  ]
}
```

### Sample IAM policy

When using this connector, ensure the authentication principal has privileges to read messages from
the SQS queue.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "kafka-connect-sqs-sink",
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage"
      ],
      "Resource": "arn:aws:sqs:*:*:*"
    }
  ]
}
```

## AWS authentication

By default, the connector uses the AWS SDK `DefaultAWSCredentialsProviderChain` to determine the
identity of the connector. This works well in simple scenarios when the connector gains privileges
granted to the Kafka Connect worker (i.e., environment variables, EC2 instance metadata, etc.)

When the identity of the connector must be separate from the worker, supply an implementation of
`sqs.credentials.provider.class` in the worker's classpath. There are two implementations directly
included within this library:

- `com.nordstrom.kafka.connect.auth.AWSUserCredentialsProvider`
- `com.nordstrom.kafka.connect.auth.AWSAssumeRoleCredentialsProvider`

### AWSUserCredentialsProvider

Use this credentials provider to cause the connector to authenticate as a specific IAM user.

Required properties:
* `sqs.credentials.provider.class`: Must be `com.nordstrom.kafka.connect.auth.AWSUserCredentialsProvider`
* `sqs.credentials.provider.accessKeyId`: AWS access key of the IAM user
* `sqs.credentials.provider.secretKey`: AWS secret key of the IAM user

### AWSAssumeRoleCredentialsProvider

Use this credentials provider to cause the connector to assume an IAM role.

Required properties:
* `sqs.credentials.provider.class`: Must be `com.nordstrom.kafka.connect.auth.AWSAssumeRoleCredentialsProvider`
* `sqs.credentials.provider.role.arn`: ARN of the IAM role to assume
* `sqs.credentials.provider.session.name`: A session name specific to this connector

Optional properties:
* `sqs.credentials.provider.external.id`: An [external identifier](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-user_externalid.html) used when assuming the role

The IAM role will have a corresponding trust policy. For example:

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
          "sts:ExternalId": "my-external-id"
        }
      }
    }
  ]
}
```

## Running the connector

This example demonstrates using the sink connector to send a message to an SQS queue from Kafka.

- Setup an SQS queue
- Setup Kafka. Use the cluster defined in `docker-compose.yaml` if you don't have one
- Customize the files in the config directory; for example, `config/sink-connector.properties.example`

Now, start the sink connector in standalone mode:

```sh
$KAFKA_HOME/bin/connect-standalone.sh \
  config/connect-worker.properties config/sink-connector.properties
```

Use a tool to produce messages to the Kafka topic.

```sh
bin/kafka-console-producer --bootstrap-server localhost:9092 \
    --topic hello-sqs-sink \
    --property parse.headers=true \
    --property 'headers.delimiter=\t'
>test:abc\t{"hello":"world"}
```
