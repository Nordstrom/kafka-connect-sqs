/*
 * Copyright 2019 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nordstrom.kafka.connect.sqs;

import java.net.URI;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // New import
import java.io.Closeable; // New import
import java.io.IOException; // New import, though not strictly needed for SqsClient.close()

// AWS SDK v2 imports
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes; // New import
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import org.apache.kafka.common.Configurable;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.kafka.connect.utils.StringUtils;

public class SqsClient implements Closeable { // Implements Closeable
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final String AWS_FIFO_SUFFIX = ".fifo";

  private final software.amazon.awssdk.services.sqs.SqsClient client;

  public SqsClient(SqsConnectorConfig config) {
    Map<String, Object> credentialProviderConfigs = config.originalsWithPrefix(
        SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CONFIG_PREFIX.getValue());
    // SQS_REGION is not directly part of credential provider configs in the same way,
    // but custom providers might expect it. Keeping it for compatibility if custom providers use it.
    credentialProviderConfigs.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), config.getRegion());

    AwsCredentialsProvider provider;
    try {
      provider = getCredentialsProvider(credentialProviderConfigs);
    } catch (Exception e) {
      log.error("Problem initializing credentials provider, falling back to DefaultCredentialsProvider.", e);
      provider = DefaultCredentialsProvider.create();
    }

    SqsClientBuilder builder = software.amazon.awssdk.services.sqs.SqsClient.builder();
    if (StringUtils.isBlank(config.getEndpointUrl())) {
      builder.region(Region.of(config.getRegion()));
    } else {
      builder.endpointOverride(URI.create(config.getEndpointUrl()));
      // Region may still be needed for signing even with endpoint override
      builder.region(Region.of(config.getRegion()));
    }

    builder.credentialsProvider(provider);
    client = builder.build();
  }

  /**
   * Delete a message from the SQS queue.
   *
   * @param url           SQS queue url.
   * @param receiptHandle Message receipt handle of message to delete.
   */
  public void delete(final String url, final String receiptHandle) {
    Guard.verifyValidUrl(url);
    Guard.verifyNotNullOrEmpty(receiptHandle, "receiptHandle");

    final DeleteMessageRequest request = DeleteMessageRequest.builder()
        .queueUrl(url)
        .receiptHandle(receiptHandle)
        .build();
    final DeleteMessageResponse result = client.deleteMessage(request);

    log.debug(".delete:receipt-handle={}, rc={}", receiptHandle, result.sdkHttpResponse().statusCode());
  }

  /**
   * Receive messages from the SQS queue.
   *
   * @param url                      SQS queue url.
   * @param maxMessages              Maximum number of messages to receive for this call.
   * @param waitTimeSeconds          Time to wait, in seconds, for messages to arrive.
   * @param messageAttributesEnabled Whether to collect message attributes.
   * @param messageAttributesList    Which message attributes to collect; if empty, all attributes are collected.
   * @return Collection of messages received.
   */
  public List<Message> receive(final String url, final int maxMessages, final int waitTimeSeconds, final Boolean messageAttributesEnabled, final List<String> messageAttributesList) {
    log.debug(".receive:queue={}, max={}, wait={}", url, maxMessages, waitTimeSeconds);

    Guard.verifyValidUrl(url);
    Guard.verifyNonNegative(waitTimeSeconds, "sqs.wait.time.seconds");
    Guard.verifyInRange(maxMessages, 0, 10, "sqs.max.messages"); // SQS max is 10
    if (!isValidState()) {
      throw new IllegalStateException("AmazonSQS client is not initialized");
    }

    ReceiveMessageRequest.Builder requestBuilder = ReceiveMessageRequest.builder()
        .queueUrl(url)
        .maxNumberOfMessages(maxMessages)
        .waitTimeSeconds(waitTimeSeconds)
        .attributeNames(""); // Start with empty, then add if needed

    if (messageAttributesEnabled) {
      if (messageAttributesList.isEmpty()) {
        requestBuilder.messageAttributeNames("All");
      } else {
        requestBuilder.messageAttributeNames(messageAttributesList);
      }
    }

    final ReceiveMessageRequest receiveMessageRequest = requestBuilder.build();
    final ReceiveMessageResponse result = client.receiveMessage(receiveMessageRequest);
    final List<Message> messages = result.messages();

    log.debug(".receive:{} messages, url={}, rc={}", messages.size(), url,
        result.sdkHttpResponse().statusCode());

    return messages;
  }

  /**
   * Send a message to an SQS queue.
   *
   * @param url               SQS queue url.
   * @param body              The message to send.
   * @param groupId           Optional group identifier (fifo queues only).
   * @param messageId         Optional message identifier (fifo queues only).
   * @param messageAttributes The message attributes to send (v1 type, will be converted).
   * @return Sequence number when FIFO; otherwise, the message identifier
   */
  public String send(final String url, final String body, final String groupId, final String messageId, final Map<String, com.amazonaws.services.sqs.model.MessageAttributeValue> messageAttributes) {
    log.debug(".send: queue={}, gid={}, mid={}", url, groupId, messageId);

    Guard.verifyValidUrl(url);
    if (!isValidState()) {
      throw new IllegalStateException("AmazonSQS client is not initialized");
    }
    final boolean fifo = isFifo(url);

    SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder().queueUrl(url).messageBody(body);

    if (messageAttributes != null) {
      Map<String, MessageAttributeValue> v2Attributes = new HashMap<>();
      for (Map.Entry<String, com.amazonaws.services.sqs.model.MessageAttributeValue> entry : messageAttributes.entrySet()) {
        com.amazonaws.services.sqs.model.MessageAttributeValue v1Value = entry.getValue();
        com.amazonaws.services.sqs.model.MessageAttributeValue v1Value = entry.getValue();
        software.amazon.awssdk.services.sqs.model.MessageAttributeValue.Builder v2Builder = software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder();
        
        v2Builder.dataType(v1Value.getDataType()); // Always set dataType

        if (v1Value.getStringValue() != null) {
            v2Builder.stringValue(v1Value.getStringValue());
        }
        if (v1Value.getBinaryValue() != null) {
            v2Builder.binaryValue(software.amazon.awssdk.core.SdkBytes.fromByteBuffer(v1Value.getBinaryValue()));
        }
        if (v1Value.getStringListValues() != null && !v1Value.getStringListValues().isEmpty()) {
            v2Builder.stringListValues(v1Value.getStringListValues());
        }
        if (v1Value.getBinaryListValues() != null && !v1Value.getBinaryListValues().isEmpty()) {
            v2Builder.binaryListValues(
                v1Value.getBinaryListValues().stream()
                    .map(software.amazon.awssdk.core.SdkBytes::fromByteBuffer)
                    .collect(java.util.stream.Collectors.toList())
            );
        }
        // IMPORTANT: SDK v1 MessageAttributeValue also had DataType specific list values 
        // like BinaryListValues, StringListValues, NumberListValues etc.
        // The above covers String and Binary lists based on v1Value.getStringListValues() and v1Value.getBinaryListValues().
        // If the original code supported other types (e.g. NumberListValues, or if dataType indicated a list type for stringValue/binaryValue),
        // that logic needs to be replicated. However, the v1 `MessageAttributeValue` class itself primarily features
        // `StringValue`, `BinaryValue`, `StringListValues`, `BinaryListValues`, and `DataType`.
        // So, covering these should be comprehensive for direct field mapping.

        v2Attributes.put(entry.getKey(), v2Builder.build());
      }
      requestBuilder.messageAttributes(v2Attributes);
    }

    if (fifo) {
      Guard.verifyNotNullOrEmpty(groupId, "groupId");
      Guard.verifyNotNullOrEmpty(messageId, "messageId");
      requestBuilder.messageGroupId(groupId);
      requestBuilder.messageDeduplicationId(messageId);
    }

    final SendMessageRequest request = requestBuilder.build();
    final SendMessageResponse result = client.sendMessage(request);

    log.debug(".send-message.OK: queue={}, result={}", url, result);

    return fifo ? result.sequenceNumber() : result.messageId();
  }

  private boolean isFifo(final String url) {
    return url.endsWith(AWS_FIFO_SUFFIX);
  }

  /**
   * Test that we have properly initialized the AWS SQS client.
   *
   * @return true if client is in a valid state.
   */
  private boolean isValidState() {
    return Facility.isNotNull(client);
  }

  @SuppressWarnings("unchecked")
  public AwsCredentialsProvider getCredentialsProvider(Map<String, ?> configs) {
    try {
      Object providerField = configs.get("class");
      String providerClass = SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_DEFAULT.getValue(); // Default if not specified
      if (null != providerField) {
        providerClass = providerField.toString();
      }

      // If the default is still the v1 DefaultAWSCredentialsProviderChain,
      // we should explicitly use the v2 DefaultCredentialsProvider.
      if ("com.amazonaws.auth.DefaultAWSCredentialsProviderChain".equals(providerClass)) {
        log.info("Using AWS SDK v2 DefaultCredentialsProvider as the effective default.");
        return DefaultCredentialsProvider.create();
      }
      
      log.info("Attempting to load custom credentials provider: {}", providerClass);
      AwsCredentialsProvider provider = ((Class<? extends AwsCredentialsProvider>)
          getClass(providerClass)).getDeclaredConstructor().newInstance();

      if (provider instanceof Configurable) {
        // Pass only the prefixed configs, excluding "class" itself if it was part of the original map.
        // The SqsConnectorConfig.originalsWithPrefix already handles this filtering appropriately.
        ((Configurable) provider).configure(configs);
      }

      return provider;
    } catch (ReflectiveOperationException e) { // Catches ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException
      throw new ConnectException(
          "Invalid class or configuration for: " + SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_CONFIG.getValue() + "; specified class: " + configs.get("class"),
          e
      );
    }
  }

  public Class<?> getClass(String className) {
    log.warn(".get-class:class={}", className);
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      log.error("Provider class not found: {}", e);
    }
    return null;
  }

  @Override
  public void close() { // No throws IOException needed as SqsClient.close() does not declare it
    if (this.client != null) {
      this.client.close(); // software.amazon.awssdk.services.sqs.SqsClient.close()
      log.info("SQS client closed.");
    }
  }
}
