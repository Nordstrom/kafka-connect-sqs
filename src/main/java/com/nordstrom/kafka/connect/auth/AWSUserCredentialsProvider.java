package com.nordstrom.kafka.connect.auth;

import org.apache.kafka.common.Configurable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Map;

public class AWSUserCredentialsProvider implements AwsCredentialsProvider, Configurable {
    private static final String AWS_ACCESS_KEY_ID = "accessKeyId";
    private static final String AWS_SECRET_ACCESS_KEY = "secretKey";

    private String awsAccessKeyId;
    private String awsSecretKey;

    @Override
    public AwsCredentials resolveCredentials() {
        return AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey);
    }

    // refresh() method is removed as it's not part of AwsCredentialsProvider in SDK v2

    @Override
    public void configure(Map<String, ?> map) {
        awsAccessKeyId = getRequiredField(map, AWS_ACCESS_KEY_ID);
        awsSecretKey = getRequiredField(map, AWS_SECRET_ACCESS_KEY);
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