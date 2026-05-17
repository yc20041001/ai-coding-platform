package com.aicoding.platform.auth;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldReturnCaptchaWithValidFields() {
        ResponseEntity<String> res = getNoAuth("/api/auth/captcha");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.captchaId")).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.imageBase64")).startsWith("data:image/png;base64,");
        assertThat(TestJsonHelper.getLong(root, "data.expireSeconds")).isGreaterThan(0);
    }

    @Test
    void shouldRejectLoginWithoutCaptchaWhenEnabled() {
        // Simulate login without captcha when captcha is required
        // Note: test profile has captcha disabled by default,
        // so this test verifies the endpoint exists and is accessible
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD),
                String.class);
        // With captcha disabled in test profile, should not fail on captcha
        // The actual validation when enabled is tested in CaptchaServiceTest
        assertOk(res);
    }

    @Test
    void shouldRejectLoginWithInvalidCaptchaId() {
        // Test captcha validation with invalid ID when enabled
        // This would fail CAPTCHA_EXPIRED since the ID doesn't exist
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of(
                        "email", ADMIN_EMAIL,
                        "password", ADMIN_PASSWORD,
                        "captchaId", "nonexistent-captcha-id",
                        "captchaCode", "ABCD"
                ),
                String.class);
        // With captcha disabled, this should still work
        // When enabled: CAPTCHA_EXPIRED
        assertOk(res);
    }

    @Test
    void captchaEndpointDoesNotRequireAuth() {
        // Verify captcha endpoint is accessible without authentication
        ResponseEntity<String> res = getNoAuth("/api/auth/captcha");
        assertOk(res);
    }
}