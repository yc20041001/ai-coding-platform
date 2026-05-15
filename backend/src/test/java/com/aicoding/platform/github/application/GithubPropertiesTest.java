package com.aicoding.platform.github.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GithubPropertiesTest {

    @Test
    void shouldReportNotConfiguredWhenClientIdIsBlank() {
        GithubProperties props = new GithubProperties();
        props.setClientId("");
        props.setClientSecret("secret");
        assertFalse(props.isConfigured());
    }

    @Test
    void shouldReportNotConfiguredWhenClientSecretIsBlank() {
        GithubProperties props = new GithubProperties();
        props.setClientId("client-id");
        props.setClientSecret("");
        assertFalse(props.isConfigured());
    }

    @Test
    void shouldReportNotConfiguredWhenBothAreNull() {
        GithubProperties props = new GithubProperties();
        assertFalse(props.isConfigured());
    }

    @Test
    void shouldReportConfiguredWhenBothAreSet() {
        GithubProperties props = new GithubProperties();
        props.setClientId("client-id");
        props.setClientSecret("client-secret");
        assertTrue(props.isConfigured());
    }
}
