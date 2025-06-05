package com.nordstrom.kafka.connect.sqs;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class SqsSinkConnectorTaskTest {

    private static final String VALID_QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/123456789012/test-queue";
    private static final String VALID_REGION = "us-west-2";

    private SqsSinkConnectorTask task;
    private Map<String, String> baseConfig;

    @Before
    public void setUp() {
        task = new SqsSinkConnectorTask();
        baseConfig = new HashMap<>();
        baseConfig.put(SqsConnectorConfigKeys.SQS_QUEUE_URL.getValue(), VALID_QUEUE_URL);
        baseConfig.put(SqsConnectorConfigKeys.SQS_REGION.getValue(), VALID_REGION);
    }

    @Test
    public void testVersion() {
        String version = task.version();
        assertThat(version).isNotNull().isNotEmpty();
    }

    @Test
    public void testStartWithTopicsConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");

        // Should not throw exception
        assertThatCode(() -> task.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStartWithTopicsRegexConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.company\\.orders\\..*");

        // Should not throw exception
        assertThatCode(() -> task.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testPutWithEmptyRecords() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");
        task.start(props);

        List<SinkRecord> emptyRecords = Collections.emptyList();

        // Should not throw exception with empty records
        assertThatCode(() -> task.put(emptyRecords)).doesNotThrowAnyException();
    }

    @Test
    public void testStartupWithRegexConfiguration() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "com\\.ecommerce\\..*");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");

        // Should start successfully with regex configuration
        assertThatCode(() -> task.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStartupWithComplexRegexPattern() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), "(user|order)-service\\.events\\.(created|updated)");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");

        // Should start successfully with complex regex
        assertThatCode(() -> task.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStartupWithMessageAttributes() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS_REGEX.getValue(), ".*\\.events\\..*");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_ENABLED.getValue(), "true");
        props.put(SqsConnectorConfigKeys.SQS_MESSAGE_ATTRIBUTES_INCLUDE_LIST.getValue(), "event-type,correlation-id");

        // Should start successfully with message attributes configuration
        assertThatCode(() -> task.start(props)).doesNotThrowAnyException();
    }

    @Test
    public void testStop() {
        Map<String, String> props = new HashMap<>(baseConfig);
        props.put(SqsConnectorConfigKeys.TOPICS.getValue(), "test-topic");
        task.start(props);

        // Should not throw exception
        assertThatCode(() -> task.stop()).doesNotThrowAnyException();
    }

    @Test
    public void testPutBeforeStartThrowsException() {
        SinkRecord record = createMockSinkRecord("test-topic", "test-key", "test-value");
        List<SinkRecord> records = Collections.singletonList(record);

        // Should throw IllegalStateException because task not started
        assertThatThrownBy(() -> task.put(records))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Task is not properly initialized");
    }

    // Helper methods for creating mock SinkRecord objects

    private SinkRecord createMockSinkRecord(String topic, String key, String value) {
        return new SinkRecord(
                topic,
                0,  // partition
                Schema.STRING_SCHEMA,
                key,
                Schema.STRING_SCHEMA,
                value,
                100L  // offset
        );
    }

    private SinkRecord createMockSinkRecordWithHeaders(String topic, String key, String value, Map<String, String> headers) {
        ConnectHeaders connectHeaders = new ConnectHeaders();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connectHeaders.add(header.getKey(), new SchemaAndValue(Schema.STRING_SCHEMA, header.getValue()));
        }

        return new SinkRecord(
                topic,
                0,  // partition
                Schema.STRING_SCHEMA,
                key,
                Schema.STRING_SCHEMA,
                value,
                100L,  // offset
                null,  // timestamp
                null,  // timestampType
                connectHeaders
        );
    }
} 