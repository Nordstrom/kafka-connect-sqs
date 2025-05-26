package com.nordstrom.kafka.connect.auth;

import com.nordstrom.kafka.connect.sqs.SqsConnectorConfigKeys;
import com.nordstrom.kafka.connect.utils.StringUtils;
import org.apache.kafka.common.Configurable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.net.URI;
import java.util.Map;

public class AWSAssumeRoleCredentialsProvider implements AwsCredentialsProvider, Configurable {
  //NB: uncomment slf4j imports and field declaration to enable logging.
//  private static final Logger log = LoggerFactory.getLogger(AWSAssumeRoleCredentialsProvider.class);

  public static final String EXTERNAL_ID_CONFIG = "external.id";
  public static final String ROLE_ARN_CONFIG = "role.arn";
  public static final String SESSION_NAME_CONFIG = "session.name";

  private String externalId;
  private String roleArn;
  private String sessionName;
  private String region;
  private String endpointUrl;

  // TODO: The cached stsClient is not explicitly closed as AwsCredentialsProvider does not define a close() method.
  // Consider if this provider should implement java.io.Closeable if used in a context that can manage its lifecycle.
  private volatile software.amazon.awssdk.services.sts.StsClient stsClient;

  @Override
  public void configure(Map<String, ?> map) {
    externalId = getOptionalField(map, EXTERNAL_ID_CONFIG);
    roleArn = getRequiredField(map, ROLE_ARN_CONFIG);
    sessionName = getRequiredField(map, SESSION_NAME_CONFIG);
    region = getRequiredField(map, SqsConnectorConfigKeys.SQS_REGION.getValue());
    endpointUrl = getOptionalField(map, SqsConnectorConfigKeys.SQS_ENDPOINT_URL.getValue());
  }

  @Override
  public AwsCredentials resolveCredentials() {
    if (this.stsClient == null) {
      synchronized (this) {
        if (this.stsClient == null) {
          StsClientBuilder stsClientBuilder = software.amazon.awssdk.services.sts.StsClient.builder()
              .credentialsProvider(software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create()); // For the STS client itself

          if (StringUtils.isBlank(endpointUrl)) {
            stsClientBuilder.region(software.amazon.awssdk.regions.Region.of(region));
          } else {
            stsClientBuilder.region(software.amazon.awssdk.regions.Region.of(region)) // Region still needed with endpoint override for signing
                          .endpointOverride(java.net.URI.create(endpointUrl));
          }
          this.stsClient = stsClientBuilder.build();
        }
      }
    }

    AssumeRoleRequest assumeRoleRequest = software.amazon.awssdk.services.sts.model.AssumeRoleRequest.builder()
        .roleArn(roleArn)
        .roleSessionName(sessionName)
        .externalId(externalId) // externalId can be null, builder handles it
        .build();

    // Note: StsAssumeRoleCredentialsProvider itself is lightweight and can be created per call.
    // The main benefit is caching the StsClient used by it.
    software.amazon.awssdk.auth.credentials.StsAssumeRoleCredentialsProvider provider =
        software.amazon.awssdk.auth.credentials.StsAssumeRoleCredentialsProvider.builder()
            .stsClient(this.stsClient) // Use the cached client
            .refreshRequest(assumeRoleRequest)
            .build();

    return provider.resolveCredentials();
  }

  // refresh() method is removed as it's not part of AwsCredentialsProvider in SDK v2

  private String getOptionalField(final Map<String, ?> map, final String fieldName) {
    final Object field = map.get(fieldName);
    if (isNotNull(field)) {
      return field.toString();
    }
    return null;
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
