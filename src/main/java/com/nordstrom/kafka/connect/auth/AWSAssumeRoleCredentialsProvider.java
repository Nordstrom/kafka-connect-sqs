package com.nordstrom.kafka.connect.auth;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.apache.kafka.common.Configurable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.Map;

public class AWSAssumeRoleCredentialsProvider implements AWSCredentialsProvider, Configurable {
  //NB: uncomment slf4j imports and field declaration to enable logging.
//  private static final Logger log = LoggerFactory.getLogger(AWSAssumeRoleCredentialsProvider.class);

  public static final String EXTERNAL_ID_CONFIG = "external.id";
  public static final String ROLE_ARN_CONFIG = "role.arn";
  public static final String SESSION_NAME_CONFIG = "session.name";

  private String externalId;
  private String roleArn;
  private String sessionName;

  @Override
  public void configure(Map<String, ?> map) {
    externalId = getOptionalField(map, EXTERNAL_ID_CONFIG);
    roleArn = getRequiredField(map, ROLE_ARN_CONFIG);
    sessionName = getRequiredField(map, SESSION_NAME_CONFIG);
  }

  @Override
  public AWSCredentials getCredentials() {
    AWSSecurityTokenServiceClientBuilder clientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    AWSCredentialsProvider provider = new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, sessionName)
        .withStsClient(clientBuilder.defaultClient())
        .withExternalId(externalId)
        .build();

    return provider.getCredentials();
  }

  @Override
  public void refresh() {
    //Nothing to do really, since we are assuming a role.
  }

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
