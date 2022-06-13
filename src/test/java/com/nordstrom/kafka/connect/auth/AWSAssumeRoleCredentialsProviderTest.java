package com.nordstrom.kafka.connect.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

public class AWSAssumeRoleCredentialsProviderTest {
    @Mock(name = "provider")
    STSAssumeRoleSessionCredentialsProvider providerMock;

    @InjectMocks
    AWSAssumeRoleCredentialsProvider provider;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testConfigure() {
        Map<String, String> configs = ImmutableMap.<String, String> builder()
                .put("external.id", "test-id")
                .put("role.arn", "aws:iam:test:arn")
                .put("session.name", "test-session")
                .build();
        provider.configure(configs);
    }

    @Test
    void testConfigureFailsWithMissingArn() {
        Map<String, String> configs = ImmutableMap.<String, String> builder()
                .put("external.id", "test-id")
                .put("session.name", "test-session")
                .build();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> provider.configure(configs));
        assertEquals("The field 'role.arn' should not be null", exception.getMessage());
    }

    @Test
    void testConfigureFailsWithMissingSessionName() {
        Map<String, String> configs = ImmutableMap.<String, String> builder()
                .put("external.id", "test-id")
                .put("role.arn", "aws:iam:test:arn")
                .build();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> provider.configure(configs));
        assertEquals("The field 'session.name' should not be null", exception.getMessage());
    }

    @Test
    void testGetCredentials() {
        provider.getCredentials();
        verify(providerMock, times(1)).getCredentials();
    }

    @Test
    void testRefreshCredentials() {
        provider.refresh();
        verify(providerMock, times(1)).refresh();
    }
}
