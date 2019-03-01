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

import java.util.Map;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqsSinkConnectorConfig extends AbstractConfig {
//  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private static final Logger log = LoggerFactory.getLogger(SqsSinkConnectorConfig.class);

  private final String queueUrl;
  private final String topics;

  private static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), Type.STRING, Importance.HIGH, "URL of the SQS queue to be written to.")
      .define(SqsConnectorConfigKeys.TOPICS.getValue(), Type.STRING, Importance.HIGH, "Kafka topic to be read from.")
      .define(SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_CONFIG.getValue(), Type.CLASS,
          SqsConnectorConfigKeys.CREDENTIALS_PROVIDER_CLASS_DEFAULT.getValue(),
          new CredentialsProviderValidator(),
          Importance.LOW,
          "Credentials provider or provider chain to use for authentication to AWS. By default the connector uses 'DefaultAWSCredentialsProviderChain'.",
          "SQS",
          0,
          ConfigDef.Width.LONG,
          "AWS Credentials Provider Class"
      )
      ;

  public static ConfigDef config() {
    return CONFIG_DEF;
  }

  public SqsSinkConnectorConfig(Map<?, ?> originals) {
    super(config(), originals);
    queueUrl = getString(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue());
    topics = getString(SqsConnectorConfigKeys.TOPICS.getValue());
  }

  public String getQueueUrl() {
    return queueUrl;
  }

  public String getTopics() {
    return topics;
  }



  private static class CredentialsProviderValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(String name, Object provider) {
      log.warn(".validator:name={}, provider={}", name, provider);
      if (provider != null && provider instanceof Class
          && AWSCredentialsProvider.class.isAssignableFrom((Class<?>) provider)) {
        return;
      }
      throw new ConfigException(
          name,
          provider,
          "Class must extend: " + AWSCredentialsProvider.class
      );
    }

    @Override
    public String toString() {
      return "Any class implementing: " + AWSCredentialsProvider.class;
    }
  }
}
