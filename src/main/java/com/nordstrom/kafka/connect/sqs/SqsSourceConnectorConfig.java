/*
 * Copyright 2019 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.kafka.connect.sqs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class SqsSourceConnectorConfig extends SqsConnectorConfig {
  private final Integer maxMessages;
  private final Integer waitTimeSeconds;
  private final Boolean messageAttributesEnabled;
  private final List<String> messageAttributesList;
  private final String messageAttributePartitionKey;

  private static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), Type.STRING, Importance.HIGH,
          "The URL of the SQS queue to be read from.")
      .define(SqsConnectorConfigKeys.TOPICS.getValue(), Type.STRING, Importance.HIGH,
          "The Kafka topic to be written to.")
      .define(SqsConnectorConfigKeys.SQS_REGION.getValue(), Type.STRING, System.getenv("AWS_REGION"), Importance.HIGH,
          "SQS queue AWS region.")
      .define(SqsConnectorConfigKeys.SQS_ENDPOINT_URL.getValue(), Type.STRING, "", Importance.LOW,
          "If specified, the connector will override the AWS region specific endpoint URL with this value. Note that this is not the queue URL.")
      .define(SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_CONFIG.getValue(), Type.CLASS,
          SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_DEFAULT.getValue(),
          new CredentialsProviderValidator(),
          Importance.LOW,
          "Credentials provider or provider chain to use for authentication to AWS. By default the connector uses 'DefaultAWSCredentialsProviderChain'.",
          "SQS",
          0,
          ConfigDef.Width.LONG,
          "AWS Credentials Provider Class")
      .define(SqsConnectorConfigKeys.SQS_MAX_MESSAGES.getValue(), Type.INT, 1, Importance.LOW,
          "Maximum number of messages to read from SQS queue for each poll interval. Range is 0 - 10 with default of 1.")
      .define(SqsConnectorConfigKeys.SQS_WAIT_TIME_SECONDS.getValue(), Type.INT, 1, Importance.LOW,
          "Duration (in seconds) to wait for a message to arrive in the queue. Default is 1.")
      .define(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), Type.BOOLEAN, false, Importance.LOW,
          "If true, it gets the SQS MessageAttributes and inserts them as Kafka Headers (only string headers are currently supported). Default is false.")
      .define(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue(), Type.LIST, "", Importance.LOW,
          "The comma separated list of MessageAttribute names to be included, if empty it includes all the Message Attributes. Default is the empty string.")
      .define(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTE_PARTITION_KEY.getValue(), Type.STRING, "", Importance.LOW,
          "The name of a single AWS SQS MessageAttribute to use as the partition key");

  public static ConfigDef config() {
    return CONFIG_DEF;
  }

  public SqsSourceConnectorConfig(Map<?, ?> originals) {
    super(config(), originals);
    maxMessages = getInt(SqsConnectorConfigKeys.SQS_MAX_MESSAGES.getValue());
    waitTimeSeconds = getInt(SqsConnectorConfigKeys.SQS_WAIT_TIME_SECONDS.getValue());

    messageAttributesEnabled = getBoolean(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue());
    if (messageAttributesEnabled) {
      messageAttributesList = getList(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue());
    } else {
      messageAttributesList = Collections.emptyList();
    }
    messageAttributePartitionKey = getString(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTE_PARTITION_KEY.getValue());
  }

  public Integer getMaxMessages() {
    return maxMessages;
  }

  public Integer getWaitTimeSeconds() {
    return waitTimeSeconds;
  }

  public Boolean getMessageAttributesEnabled() {
    return messageAttributesEnabled;
  }

  public List<String> getMessageAttributesList() {
    return messageAttributesList;
  }

  public String getMessageAttributePartitionKey() {
    return messageAttributePartitionKey;
  }
}
