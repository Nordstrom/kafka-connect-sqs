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

/*
 * Contains all connector configuration keys and constants.
 */
public enum SqsConnectorConfigKeys {
  SQS_MAX_MESSAGES("sqs.max.messages"),
  SQS_QUEUE_URL("sqs.queue.url"),
  SQS_WAIT_TIME_SECONDS("sqs.wait.time.seconds"),
  TOPICS("topics"),
  SQS_REGION("sqs.region"),
  SQS_ENDPOINT_URL("sqs.endpoint.url"),
  SQS_MESSAGE_ATTRIBUTES_ENABLED("sqs.message.attributes.enabled"),
  SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST("sqs.message.attributes.include.list"),

  // These are not part of the connector configuration proper, but just a convenient
  // place to define the constants.
  CREDENTIALS_PROVIDER_CLASS_CONFIG("sqs.credentials.provider.class"),
  CREDENTIALS_PROVIDER_CLASS_DEFAULT("com.amazonaws.auth.DefaultAWSCredentialsProviderChain"),
  CREDENTIALS_PROVIDER_CONFIG_PREFIX("sqs.credentials.provider."),  //NB: trailing '.'
  SQS_MESSAGE_ID("sqs.message.id"),
  SQS_MESSAGE_RECEIPT_HANDLE("sqs.message.receipt-handle");

  private final String value;

  SqsConnectorConfigKeys(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
