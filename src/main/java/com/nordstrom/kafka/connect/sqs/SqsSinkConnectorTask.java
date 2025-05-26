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

import java.text.MessageFormat ;
import java.util.Collection ;
import java.util.HashMap;
import java.util.List;
import java.util.Map ;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.sink.SinkRecord ;
import org.apache.kafka.connect.sink.SinkTask ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.nordstrom.kafka.connect.sqs.SqsSinkConnector ;

public class SqsSinkConnectorTask extends SinkTask {
  private final Logger log = LoggerFactory.getLogger( this.getClass() ) ;

  private SqsClient client ;
  private SqsSinkConnectorConfig config ;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.connector.Task#version()
   */
  @Override
  public String version() {
    return new SqsSinkConnector().version() ;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.sink.SinkTask#start(java.util.Map)
   */
  @Override
  public void start( Map<String, String> props ) {
    log.info( "task.start" ) ;
    Guard.verifyNotNull( props, "Task properties" ) ;

    config = new SqsSinkConnectorConfig( props ) ;
    client = new SqsClient(config) ;

    log.info( "task.start:OK, sqs.queue.url={}, topics={}", config.getQueueUrl(), config.getTopics() ) ;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.sink.SinkTask#put(java.util.Collection)
   */
  @Override
  public void put( Collection<SinkRecord> records ) {
    if ( records.isEmpty() ) {
      return ;
    }

    if ( !isValidState() ) {
      throw new IllegalStateException( "Task is not properly initialized" ) ;
    }

    log.debug( ".put:record_count={}", records.size() ) ;
    for ( final SinkRecord record : records ) {
      final String mid = MessageFormat.format( "{0}-{1}-{2}", record.topic(), record.kafkaPartition().longValue(),
          record.kafkaOffset() ) ;
      final String key = Facility.isNotNull( record.key() ) ? record.key().toString() : null ;
      final String gid = Facility.isNotNullNorEmpty( key ) ? key : record.topic() ;
      final String body = Facility.isNotNull( record.value() ) ? record.value().toString() : "" ;

      Map<String, MessageAttributeValue> messageAttributes = null;

      if (config.getMessageAttributesEnabled()) {
        final Headers headers = record.headers();
        messageAttributes = new HashMap<>();
        List<String> attributesList = config.getMessageAttributesList();
        boolean allNamesEnabled = attributesList.isEmpty();
        for(Header header: headers) {
          if(allNamesEnabled || attributesList.contains(header.key())) {
            if(header.schema().equals(Schema.STRING_SCHEMA)) {
              messageAttributes.put(header.key(), new MessageAttributeValue()
                .withDataType("String")
                .withStringValue((String)header.value()));
            }
          }
        }
      }

      if ( Facility.isNotNullNorEmpty( body ) ) {
        try {
          final String sid = client.send( config.getQueueUrl(), body, gid, mid, messageAttributes ) ;

          log.debug( ".put.OK:message-id={}, queue.url={}, sqs-group-id={}, sqs-message-id={}", gid, mid,
              config.getQueueUrl(), sid ) ;
        } catch ( final RuntimeException e ) {
          log.error( "An Exception occurred while sending message {} to target url {}:", mid, config.getQueueUrl(),
              e ) ;
        }
      } else {
        log.warn( "Skipping empty message: key={}", key ) ;
      }

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.kafka.connect.sink.SinkTask#stop()
   */
  @Override
  public void stop() {
    log.info( "Stopping SQS Sink Connector Task" ) ;
    if (this.client != null) {
        this.client.close();
        this.client = null;
    }
    log.info( "SQS Sink Connector Task stopped." ) ;
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
