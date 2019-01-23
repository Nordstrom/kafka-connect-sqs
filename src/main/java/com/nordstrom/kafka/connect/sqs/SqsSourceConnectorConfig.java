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

package com.nordstrom.kafka.connect.sqs ;

import java.util.Map ;

import org.apache.kafka.common.config.AbstractConfig ;
import org.apache.kafka.common.config.ConfigDef ;
import org.apache.kafka.common.config.ConfigDef.Importance ;
import org.apache.kafka.common.config.ConfigDef.Type ;

/**
 *
 */
public class SqsSourceConnectorConfig extends AbstractConfig {

  private final Integer maxMessages ;
  private final String queueUrl ;
  private final String topics ;
  private final Integer waitTimeSeconds ;

  public static enum ConfigurationKeys {
    SQS_MAX_MESSAGES( "sqs.max.messages" ),
    SQS_QUEUE_URL( "sqs.queue.url" ),
    SQS_WAIT_TIME_SECONDS( "sqs.wait.time.seconds" ),
    TOPICS( "topics" ),
    
    // These are not part of the connector configuration, but just a convenient
    // place to define the constant
    SQS_MESSAGE_ID( "sqs.message.id" ),
    SQS_MESSAGE_RECEIPT_HANDLE( "sqs.message.receipt-handle" )
    ;

    private final String value ;

    private ConfigurationKeys( String value ) {
      this.value = value ;
    }

    public String getValue() {
      return value ;
    }
    
  }

  private static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define( ConfigurationKeys.SQS_MAX_MESSAGES.value, Type.INT, 1, Importance.LOW,
          "Maximum number of messages to read from SQS queue for each poll interval. Range is 0 - 10 with default of 1." )
      .define( ConfigurationKeys.SQS_QUEUE_URL.value, Type.STRING, Importance.HIGH, "The URL of the SQS queue to be read from." )
      .define( ConfigurationKeys.SQS_WAIT_TIME_SECONDS.value, Type.INT, 1, Importance.LOW,
          "Duration (in seconds) to wait for a message to arrive in the queue. Default is 1." )
      .define( ConfigurationKeys.TOPICS.value, Type.STRING, Importance.HIGH, "The Kafka topic to be written to." ) ;

  public static ConfigDef config() {
    return CONFIG_DEF ;
  }

  public SqsSourceConnectorConfig( Map<?, ?> originals ) {
    super( config(), originals ) ;
    maxMessages = getInt( ConfigurationKeys.SQS_MAX_MESSAGES.value ) ;
    queueUrl = getString( ConfigurationKeys.SQS_QUEUE_URL.value ) ;
    topics = getString( ConfigurationKeys.TOPICS.value ) ;
    waitTimeSeconds = getInt( ConfigurationKeys.SQS_WAIT_TIME_SECONDS.value ) ;
  }

  public Integer getMaxMessages() {
    return maxMessages ;
  }

  public String getQueueUrl() {
    return queueUrl ;
  }

  public String getTopics() {
    return topics ;
  }

  public Integer getWaitTimeSeconds() {
    return waitTimeSeconds ;
  }

}
