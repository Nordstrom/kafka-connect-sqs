package com.nordstrom.kafka.connect.sqs;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

abstract class SqsConnectorConfig extends AbstractConfig {

    private final String queueUrl;
    private final String topics;
    private final String region;
    private final String endpoint;

    public SqsConnectorConfig(ConfigDef configDef, Map<?, ?> originals) {
        super(configDef, originals);
        queueUrl = getString(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue());
        topics = getString(SqsConnectorConfigKeys.TOPICS.getValue());
        region = getString(SqsConnectorConfigKeys.SQS_REGION.getValue());
        endpoint = getString(SqsConnectorConfigKeys.SQS_ENDPOINT.getValue());
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getTopics() {
        return topics;
    }

    public String getRegion()  {
        return region;
    }
    public String getEndpoint()  {
        return endpoint;
    }
}
