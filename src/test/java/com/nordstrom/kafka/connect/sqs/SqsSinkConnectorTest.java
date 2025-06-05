package com.nordstrom.kafka.connect.sqs;

import org.apache.kafka.common.config.ConfigException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class SqsSinkConnectorTest {

    private static final String VALID_QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/123456789012/test-queue";
    private static final String VALID_REGION = "us-west-2";

    private SqsSinkConnector connector;
    private Map<String, String> baseConfig;

    @Before
    public void setUp() {
        connector = new SqsSinkConnector();
        baseConfig = new HashMap<>();
        baseConfig.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        baseConfig.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
    }

    @Test
    public void testVersion() {
        String version = connector.version();
        assertThat(version).isNotNull().isNotEmpty();
    }

    @Test
    public void testTaskClass() {
        Class<?> taskClass = connector.taskClass();
        assertThat(taskClass).isEqualTo(SqsSinkConnectorTask.class);
    }

    @Test
    public void testStartWithValidTopicsConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");

        // Should not throw exception
        assertThatCode(() -> connector.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStartWithValidTopicsRegexConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.company\\.orders\\..*");

        // Should not throw exception
        assertThatCode(() -> connector.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStartWithInvalidConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.company\\..*\\.events");

        assertThatThrownBy(() -> connector.start(props))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("Cannot specify both 'topics' and 'topics.regex'");
    }

    @Test
    public void testStartWithMissingTopicsConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        // No topics or topics.regex specified

        assertThatThrownBy(() -> connector.start(props))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("Either 'topics' or 'topics.regex' must be specified");
    }

    @Test
    public void testTaskConfigsWithTopics() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");

        connector.start(props);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(2);

        assertThat(taskConfigs).hasSize(2);
        
        // Each task should have the same configuration
        for (Map<String, String> taskConfig : taskConfigs) {
            assertThat(taskConfig.get(SqsConnectorConfigKeys.TOPICS.getValue())).isEqualTo("test-topic");
            assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue())).isEqualTo(VALID_QUEUE_URL);
            assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_REGION.getValue())).isEqualTo(VALID_REGION);
        }
    }

    @Test
    public void testTaskConfigsWithTopicsRegex() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "payment-service\\.events\\..*");

        connector.start(props);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(3);

        assertThat(taskConfigs).hasSize(3);
        
        // Each task should have the same configuration
        for (Map<String, String> taskConfig : taskConfigs) {
            assertThat(taskConfig.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue())).isEqualTo("payment-service\\.events\\..*");
            assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue())).isEqualTo(VALID_QUEUE_URL);
            assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_REGION.getValue())).isEqualTo(VALID_REGION);
        }
    }

    @Test
    public void testTaskConfigsWithZeroTasks() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");

        connector.start(props);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(0);

        assertThat(taskConfigs).isEmpty();
    }

    @Test
    public void testTaskConfigsWithComplexConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.ecommerce\\.(orders|payments)\\..*");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue(), "event-type,source");

        connector.start(props);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);

        assertThat(taskConfigs).hasSize(1);
        
        Map<String, String> taskConfig = taskConfigs.get(0);
        assertThat(taskConfig.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue())).isEqualTo("com\\.ecommerce\\.(orders|payments)\\..*");
        assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue())).isEqualTo("true");
        assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue())).isEqualTo("event-type,source");
    }

    @Test
    public void testStop() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");

        connector.start(props);
        
        // Should not throw exception
        assertThatCode(() -> connector.stop()).doesNotThrowAnyException();
    }

    @Test
    public void testConfig() {
        assertThat(connector.config()).isNotNull();
        assertThat(connector.config()).isEqualTo(SqsSinkConnectorConfig.config());
    }

    @Test
    public void testTaskConfigsPassesThroughAllConfigurationProperties() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "user-service\\.events\\..*");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue(), "event-id,correlation-id");
        props.put(SqsConnectorConfigKeys.SQS_ENDPOINT_URL.getValue(), "http://localhost:4566");

        connector.start(props);

        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);
        Map<String, String> taskConfig = taskConfigs.get(0);

        // Verify all properties are passed through
        assertThat(taskConfig).containsAllEntriesOf(props);
    }
} 