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
public class SqsSinkConnectorConfig extends AbstractConfig {
  private final String queueUrl ;
  private final String topics ;

  public static enum ConfigurationKeys {
    SQS_QUEUE_URL( "sqs.queue.url" ),
    TOPICS( "topics" );

    private final String value ;

    private ConfigurationKeys( String value ) {
      this.value = value ;
    }
  }

  private static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define( ConfigurationKeys.SQS_QUEUE_URL.value, Type.STRING, Importance.HIGH, "URL of the SQS queue to be written to." )
      .define( ConfigurationKeys.TOPICS.value, Type.STRING, Importance.HIGH, "Kafka topic to be read from." ) ;

  public static ConfigDef config() {
    return CONFIG_DEF ;
  }

  public SqsSinkConnectorConfig( Map<?, ?> originals ) {
    super( config(), originals ) ;
    queueUrl = getString( ConfigurationKeys.SQS_QUEUE_URL.value ) ;
    topics = getString( ConfigurationKeys.TOPICS.value ) ;
  }

  public String getQueueUrl() {
    return queueUrl ;
  }

  public String getTopics() {
    return topics ;
  }

}
