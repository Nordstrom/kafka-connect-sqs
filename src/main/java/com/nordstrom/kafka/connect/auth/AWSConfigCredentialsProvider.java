package com.nordstrom.kafka.connect.auth;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.kafka.common.Configurable;

import java.util.Map;

public class AWSConfigCredentialsProvider implements AWSCredentialsProvider, Configurable {
    private static final String AWS_ACCESS_KEY_ID = "accessKeyId";

    private static final String AWS_SECRET_ACCESS_KEY = "secretAccessKey";

    private String awsAccessKeyId;

    private String awsSecretAccessKey;

    @Override
    public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    }

    @Override
    public void refresh() { }

    @Override
    public void configure(Map<String, ?> map) {
        awsAccessKeyId = getRequiredField(map, AWS_ACCESS_KEY_ID);
        awsSecretAccessKey = getRequiredField(map, AWS_SECRET_ACCESS_KEY);
    }

    private String getRequiredField(final Map<String, ?> map, final String fieldName) {
        final Object field = map.get(fieldName);
        verifyNotNull(field, fieldName);
        final String fieldValue = field.toString();
        verifyNotNullOrEmpty(fieldValue, fieldName);

        return fieldValue;
    }

    private boolean isNotNull(final Object field) {
        return null != field;
    }

    private boolean isNotNullOrEmpty(final String field) {
        return null != field && !field.isEmpty();
    }

    private void verifyNotNull(final Object field, final String fieldName) {
        if (!isNotNull(field)) {
            throw new IllegalArgumentException(String.format("The field '%1s' should not be null", fieldName));
        }
    }

    private void verifyNotNullOrEmpty(final String field, final String fieldName) {
        if (!isNotNullOrEmpty(field)) {
            throw new IllegalArgumentException(String.format("The field '%1s' should not be null or empty", fieldName));
        }
    }
}
