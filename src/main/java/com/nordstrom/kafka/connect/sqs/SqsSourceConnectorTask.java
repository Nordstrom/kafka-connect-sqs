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

package com.nordstrom.kafka.connect.sqs ;

import java.util.*;
import java.util.stream.Collectors ;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.nordstrom.kafka.connect.utils.StringUtils;
import org.apache.kafka.connect.data.Schema ;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.source.SourceRecord ;
import org.apache.kafka.connect.source.SourceTask ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.amazonaws.services.sqs.model.Message ;
import com.nordstrom.kafka.connect.About ;

public class SqsSourceConnectorTask extends SourceTask {
  private final Logger log = LoggerFactory.getLogger( this.getClass() ) ;

  private SqsClient client ;
  private SqsSourceConnectorConfig config ;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.connector.Task#version()
   */
  @Override
  public String version() {
    return About.CURRENT_VERSION ;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.source.SourceTask#start(java.util.Map)
   */
  @Override
  public void start( Map<String, String> props ) {
    log.info( "task.start" ) ;
    Guard.verifyNotNull( props, "Task properties" ) ;

    config = new SqsSourceConnectorConfig( props ) ;
    client = new SqsClient(config) ;

    log.info( "task.start.OK, sqs.queue.url={}, topics={}", config.getQueueUrl(), config.getTopics() ) ;
  }

  private String getPartitionKey(Message message) {
    String messageId = message.getMessageId();
    if (!config.getMessageAttributesEnabled()) {
      return messageId;
    }
    String messageAttributePartitionKey = config.getMessageAttributePartitionKey();
    if (StringUtils.isBlank(messageAttributePartitionKey)) {
      return messageId;
    }

    // search for the String message attribute with the same name as the configured partition key
    Map<String, MessageAttributeValue> attributes = message.getMessageAttributes();
    for(String attributeKey: attributes.keySet()) {
      if (!Objects.equals(attributeKey, messageAttributePartitionKey)) {
        continue;
      }
      MessageAttributeValue attrValue = attributes.get(attributeKey);
      if (!attrValue.getDataType().equals("String")) {
        continue;
      }
      return attrValue.getStringValue();
    }
    return messageId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.source.SourceTask#poll()
   */
  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    log.debug( ".poll:valid-state={}", isValidState() ) ;

    if ( !isValidState() ) {
      throw new IllegalStateException( "Task is not properly initialized" ) ;
    }

    // Read messages from the queue.
    List<Message> messages = client.receive(
        config.getQueueUrl(),
        config.getMaxMessages(),
        config.getWaitTimeSeconds(),
        config.getMessageAttributesEnabled(),
        config.getMessageAttributesList());
    log.debug( ".poll:url={}, max={}, wait={}, size={}", config.getQueueUrl(), config.getMaxMessages(),
        config.getWaitTimeSeconds(), messages.size() ) ;

    // Create a SourceRecord for each message in the queue.
    return messages.stream().map( message -> {

      Map<String, String> sourcePartition = Collections.singletonMap( SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(),
          config.getQueueUrl() ) ;
      Map<String, String> sourceOffset = new HashMap<>() ;
      // Save the message id and receipt-handle. receipt-handle is needed to delete
      // the message once the record is committed.
      sourceOffset.put( SqsConnectorConfigKeys.SQS_MESSAGE_ID.getValue(), message.getMessageId() ) ;
      sourceOffset.put( SqsConnectorConfigKeys.SQS_MESSAGE_RECEIPT_HANDLE.getValue(), message.getReceiptHandle() ) ;
      log.trace( ".poll:source-partition={}", sourcePartition ) ;
      log.trace( ".poll:source-offset={}", sourceOffset ) ;

      final String body = message.getBody();
      final String key = getPartitionKey(message);
      final String topic = config.getTopics() ;

      final ConnectHeaders headers = new ConnectHeaders();
      if (config.getMessageAttributesEnabled()) {
        Map<String, MessageAttributeValue> attributes = message.getMessageAttributes();
        // sqs api should return only the fields specified in the list
        for(String attributeKey: attributes.keySet()) {
          MessageAttributeValue attrValue = attributes.get(attributeKey);
          if (attrValue.getDataType().equals("String")) {
            SchemaAndValue schemaAndValue = new SchemaAndValue(Schema.STRING_SCHEMA, attrValue.getStringValue());
            headers.add(attributeKey, schemaAndValue);
          }
        }
      }

      return new SourceRecord(sourcePartition, sourceOffset, topic, null, Schema.STRING_SCHEMA, key, Schema.STRING_SCHEMA,
          body, null, headers) ;
    } ).collect( Collectors.toList() ) ;
  }

  /* (non-Javadoc)
   * @see org.apache.kafka.connect.source.SourceTask#commitRecord(org.apache.kafka.connect.source.SourceRecord)
   */
  @Override
  public void commitRecord( SourceRecord record ) throws InterruptedException {
    Guard.verifyNotNull( record, "record" ) ;
    final String receipt = record.sourceOffset().get( SqsConnectorConfigKeys.SQS_MESSAGE_RECEIPT_HANDLE.getValue() )
        .toString() ;
    log.debug( ".commit-record:url={}, receipt-handle={}", config.getQueueUrl(), receipt ) ;
    client.delete( config.getQueueUrl(), receipt ) ;
    super.commitRecord( record ) ;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.source.SourceTask#stop()
   */
  @Override
  public void stop() {
    log.info( "Stopping SQS Source Connector Task" ) ;
    if (this.client != null) {
        this.client.close();
        this.client = null;
    }
    log.info( "SQS Source Connector Task stopped." ) ;
  }

  /**
   * Test that we have both the task configuration and SQS client properly
   * initialized.
   * 
   * @return true if task is in a valid state.
   */
  private boolean isValidState() {
    return null != config && null != client ;
  }

}
