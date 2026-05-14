package com.aicoding.platform.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected final AtomicReference<String> adminToken = new AtomicReference<>();
    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "Admin@123456";
    protected static final String AGENT_ID = "300002";

    protected @NonNull String baseUrl() {
        return "http://localhost:" + port;
    }

    protected @NonNull String adminToken() {
        String token = adminToken.get();
        if (token == null) {
            token = loginAdmin();
            adminToken.set(token);
        }
        return Objects.requireNonNull(token);
    }

    protected @NonNull String loginAdmin() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD),
                String.class);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String code = TestJsonHelper.getString(root, "code");
        if (!"OK".equals(code)) {
            throw new RuntimeException("Admin login failed: " + res.getBody());
        }
        return Objects.requireNonNull(TestJsonHelper.getString(root, "data.accessToken"));
    }

    protected @NonNull String requiredAdminToken() {
        return Objects.requireNonNull(adminToken());
    }

    protected <T> ResponseEntity<String> post(@NonNull String path, Object body) {
        RequestEntity<Object> entity = RequestEntity.post(uri(path))
                .contentType(jsonMediaType())
                .headers(headers -> headers.setBearerAuth(requiredAdminToken()))
                .body(Objects.requireNonNull(body));
        return restTemplate.exchange(entity, String.class);
    }

    protected ResponseEntity<String> get(@NonNull String path) {
        RequestEntity<Void> entity = RequestEntity.get(uri(path))
                .headers(headers -> headers.setBearerAuth(requiredAdminToken()))
                .build();
        return restTemplate.exchange(entity, String.class);
    }

    protected ResponseEntity<String> put(@NonNull String path, Object body) {
        RequestEntity<Object> entity = RequestEntity.put(uri(path))
                .contentType(jsonMediaType())
                .headers(headers -> headers.setBearerAuth(requiredAdminToken()))
                .body(Objects.requireNonNull(body));
        return restTemplate.exchange(entity, String.class);
    }

    protected ResponseEntity<String> delete(@NonNull String path) {
        RequestEntity<Void> entity = RequestEntity.delete(uri(path))
                .headers(headers -> headers.setBearerAuth(requiredAdminToken()))
                .build();
        return restTemplate.exchange(entity, String.class);
    }

    protected @NonNull URI uri(@NonNull String path) {
        return Objects.requireNonNull(URI.create(baseUrl() + path));
    }

    protected @NonNull MediaType jsonMediaType() {
        return Objects.requireNonNull(MediaType.APPLICATION_JSON);
    }

    protected ResponseEntity<String> getNoAuth(@NonNull String path) {
        return restTemplate.getForEntity(baseUrl() + path, String.class);
    }

    protected void assertOk(ResponseEntity<String> res) {
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String code = TestJsonHelper.getString(root, "code");
        if (!"OK".equals(code)) {
            throw new AssertionError("Expected OK but got " + code + ": " + res.getBody());
        }
    }

    protected void assertCode(ResponseEntity<String> res, String expectedCode) {
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String code = TestJsonHelper.getString(root, "code");
        if (!expectedCode.equals(code)) {
            throw new AssertionError("Expected " + expectedCode + " but got " + code + ": " + res.getBody());
        }
    }
}
