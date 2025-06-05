package com.nordstrom.kafka.connect.sqs;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test demonstrating the end-to-end workflow of the SQS sink connector
 * with regex topic patterns. This test focuses on configuration validation and
 * basic functionality without requiring actual AWS resources.
 */
public class SqsSinkConnectorIntegrationTest {

    private static final String VALID_QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/123456789012/test-queue";
    private static final String VALID_REGION = "us-west-2";

    private SqsSinkConnector connector;
    private SqsSinkConnectorTask task;

    @Before
    public void setUp() {
        connector = new SqsSinkConnector();
        task = new SqsSinkConnectorTask();
    }

    @Test
    public void testCompleteWorkflowWithRegexTopicPattern() {
        // Test scenario: Event-driven architecture with multiple microservices
        // Regex pattern matches all order-related topics from ecommerce domain
        
        Map<String, String> connectorProps = new HashMap<>();
        connectorProps.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        connectorProps.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
        connectorProps.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.ecommerce\\.orders\\..*");
        connectorProps.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");
        connectorProps.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue(), "event-type,service-name");

        // Step 1: Start the connector
        assertThatCode(() -> connector.start(connectorProps)).doesNotThrowAnyException();

        // Step 2: Verify connector is properly configured
        assertThat(connector.taskClass()).isEqualTo(SqsSinkConnectorTask.class);

        // Step 3: Get task configurations
        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);
        assertThat(taskConfigs).hasSize(1);
        
        Map<String, String> taskProps = taskConfigs.get(0);
        assertThat(taskProps.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue())).isEqualTo("com\\.ecommerce\\.orders\\..*");

        // Step 4: Start the task
        assertThatCode(() -> task.start(taskProps)).doesNotThrowAnyException();

        // Step 5: Verify task can handle empty records
        List<SinkRecord> emptyRecords = Collections.emptyList();
        assertThatCode(() -> task.put(emptyRecords)).doesNotThrowAnyException();

        // Step 6: Stop the task and connector
        assertThatCode(() -> task.stop()).doesNotThrowAnyException();
        assertThatCode(() -> connector.stop()).doesNotThrowAnyException();
    }

    @Test
    public void testWorkflowWithSpecificTopicPattern() {
        // Test scenario: User service specific events
        Map<String, String> connectorProps = new HashMap<>();
        connectorProps.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        connectorProps.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
        connectorProps.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "user-service\\.events\\..*");
        connectorProps.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");

        // Full workflow test
        connector.start(connectorProps);
        
        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);
        Map<String, String> taskProps = taskConfigs.get(0);
        
        task.start(taskProps);

        // Verify configuration was passed correctly
        assertThat(taskProps.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue())).isEqualTo("user-service\\.events\\..*");

        task.stop();
        connector.stop();
    }

    @Test
    public void testWorkflowWithEnvironmentBasedPattern() {
        // Test scenario: Production environment events only
        Map<String, String> connectorProps = new HashMap<>();
        connectorProps.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        connectorProps.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
        connectorProps.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "prod\\..*\\.events");
        connectorProps.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "false");

        connector.start(connectorProps);
        
        List<Map<String, String>> taskConfigs = connector.taskConfigs(2);
        assertThat(taskConfigs).hasSize(2);

        // Both tasks should have the same configuration
        for (Map<String, String> taskConfig : taskConfigs) {
            assertThat(taskConfig.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue())).isEqualTo("prod\\..*\\.events");
            
            task.start(taskConfig);
            
            // Verify task starts successfully with the configuration
            assertThat(taskConfig).containsKey(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue());
            assertThat(taskConfig).containsKey(SqsConnectorConfigKeys.SQS_REGION.getValue());

            task.stop();
        }

        connector.stop();
    }

    @Test
    public void testConfigurationValidationInWorkflow() {
        // Test invalid configuration scenarios
        Map<String, String> invalidProps = new HashMap<>();
        invalidProps.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        invalidProps.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
        invalidProps.put(SqsConnectorConfigKeys.TOPICS.getValue(), "specific-topic");
        invalidProps.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), ".*_event");

        // Should fail during connector start due to configuration validation
        assertThatThrownBy(() -> connector.start(invalidProps))
                .hasMessageContaining("Cannot specify both 'topics' and 'topics.regex'");
    }

    @Test
    public void testMultipleTaskConfiguration() {
        // Test that multiple tasks get the same regex configuration
        Map<String, String> connectorProps = new HashMap<>();
        connectorProps.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        connectorProps.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
        connectorProps.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.company\\.(orders|payments)\\..*");

        connector.start(connectorProps);
        
        List<Map<String, String>> taskConfigs = connector.taskConfigs(3);
        assertThat(taskConfigs).hasSize(3);

        // All tasks should have identical configuration
        for (Map<String, String> taskConfig : taskConfigs) {
            assertThat(taskConfig.get(SqsConnectorConfigKeys.TOPICS_REGEX.getValue()))
                    .isEqualTo("com\\.company\\.(orders|payments)\\..*");
            assertThat(taskConfig.get(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue()))
                    .isEqualTo(VALID_QUEUE_URL);
        }

        connector.stop();
    }

    // Helper methods for creating mock SinkRecord objects (kept for potential future use)

    private SinkRecord createRecord(String topic, String key, String value) {
        return new SinkRecord(
                topic,
                0,
                Schema.STRING_SCHEMA,
                key,
                Schema.STRING_SCHEMA,
                value,
                100L
        );
    }

    private SinkRecord createRecordWithHeaders(String topic, String key, String value, Map<String, String> headers) {
        ConnectHeaders connectHeaders = new ConnectHeaders();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connectHeaders.add(header.getKey(), new SchemaAndValue(Schema.STRING_SCHEMA, header.getValue()));
        }

        return new SinkRecord(
                topic,
                0,
                Schema.STRING_SCHEMA,
                key,
                Schema.STRING_SCHEMA,
                value,
                100L,
                null,
                null,
                connectHeaders
        );
    }
} 