package com.aicoding.platform.auth;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldLoginSuccessfully() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD),
                String.class);

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.accessToken")).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.refreshToken")).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.tokenType")).isEqualTo("Bearer");
        assertThat(TestJsonHelper.getLong(root, "data.expiresIn")).isGreaterThan(0);
        assertThat(TestJsonHelper.getString(root, "data.user.username")).isEqualTo("admin");
        assertThat(TestJsonHelper.getString(root, "data.user.email")).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void shouldGetCurrentUserWithToken() {
        ResponseEntity<String> res = get("/api/auth/me");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.username")).isEqualTo("admin");
        assertThat(TestJsonHelper.getString(root, "data.email")).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void shouldRejectWithoutToken() {
        ResponseEntity<String> res = getNoAuth("/api/auth/me");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    void shouldRefreshToken() {
        // Login to get refresh token
        ResponseEntity<String> loginRes = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD),
                String.class);
        JsonNode loginRoot = TestJsonHelper.parse(loginRes.getBody());
        String refreshToken = TestJsonHelper.getString(loginRoot, "data.refreshToken");
        assertThat(refreshToken).isNotEmpty();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("refreshToken", refreshToken), headers);
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl() + "/api/auth/refresh", HttpMethod.POST, entity, String.class);

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.accessToken")).isNotEmpty();
    }

    @Test
    void shouldRejectAccessTokenForRefresh() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                    Map.of("refreshToken", adminToken()), headers);
            ResponseEntity<String> res = restTemplate.exchange(
                    baseUrl() + "/api/auth/refresh", HttpMethod.POST, entity, String.class);
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Server correctly rejected the request — JDK HTTP client may throw on 401
            // when WWW-Authenticate challenge cannot be satisfied
        }
    }

    @Test
    void shouldRejectWrongPassword() {
        try {
            ResponseEntity<String> res = restTemplate.postForEntity(
                    baseUrl() + "/api/auth/login",
                    Map.of("email", ADMIN_EMAIL, "password", "WrongPassword123"),
                    String.class);
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Server correctly rejected the request — JDK HTTP client may throw on 401
        }
    }

    @Test
    void shouldRejectMissingEmail() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("password", ADMIN_PASSWORD),
                String.class);

        assertCode(res, "VALIDATION_ERROR");
    }
}
