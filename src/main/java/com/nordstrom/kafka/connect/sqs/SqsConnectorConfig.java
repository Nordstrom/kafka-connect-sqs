package com.nordstrom.kafka.connect.sqs;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class SqsConnectorConfig extends AbstractConfig {

    private final String queueUrl;
    private final String topics;
    private final String region;
    private final String endpointUrl;

    private final Boolean messageAttributesEnabled;

    private final List<String> messageAttributesList;

    private final String messageAttributePartitionKey;

    public SqsConnectorConfig(ConfigDef configDef, Map<?, ?> originals) {
        super(configDef, originals);
        queueUrl = getString(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue());
        topics = getString(SqsConnectorConfigKeys.TOPICS.getValue());
        region = getString(SqsConnectorConfigKeys.SQS_REGION.getValue());
        endpointUrl = getString(SqsConnectorConfigKeys.SQS_ENDPOINT_URL.getValue());
        messageAttributesEnabled = getBoolean(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue());

        List<String> csMessageAttributesList = getList(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue());
        messageAttributesList = messageAttributesEnabled ? csMessageAttributesList : new ArrayList<>();
        messageAttributePartitionKey = getString(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTE_PARTITION_KEY.getValue());
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

    public String getEndpointUrl()  {
        return endpointUrl;
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
