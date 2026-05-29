package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class BetaEnvironmentReadinessIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String sessionIdValue;
    private String checkIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;
        String suffix = String.valueOf(System.currentTimeMillis());

        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-BetaReadiness-" + suffix,
                "description", "Beta readiness integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Create session
        ResponseEntity<String> sessionRes = post("/api/beta-sessions", Map.of(
                "projectId", projectIdValue,
                "title", "Readiness Test Session " + suffix
        ));
        assertOk(sessionRes);
        sessionIdValue = TestJsonHelper.getString(TestJsonHelper.parse(sessionRes.getBody()), "data.id");

        // Create readiness check
        ResponseEntity<String> checkRes = post("/api/projects/" + projectIdValue + "/environment-readiness", Map.of(
                "sessionId", sessionIdValue,
                "targetName", "Docker Service",
                "targetType", "CONTAINER",
                "checkStatus", "PASS",
                "summary", "All containers running"
        ));
        assertOk(checkRes);
        checkIdValue = TestJsonHelper.getString(TestJsonHelper.parse(checkRes.getBody()), "data.id");
    }

    private String projectId() {
        ensureTestData();
        return Objects.requireNonNull(projectIdValue);
    }

    private String sessionId() {
        ensureTestData();
        return Objects.requireNonNull(sessionIdValue);
    }

    private String checkId() {
        ensureTestData();
        return Objects.requireNonNull(checkIdValue);
    }

    // ========================
    // 1. Happy path
    // ========================

    @Test
    void shouldCreateCheckSuccessfully() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/environment-readiness", Map.of(
                "sessionId", sessionId(),
                "targetName", "API Health " + suffix,
                "targetType", "ENDPOINT",
                "checkStatus", "PASS",
                "summary", "API is healthy",
                "detailJson", "{\"latency\":\"45ms\"}"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "checkStatus")).isEqualTo("PASS");
        assertThat(TestJsonHelper.getString(data, "targetName")).contains("API Health");
    }

    @Test
    void shouldCreateCheckWithWarnStatus() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/environment-readiness", Map.of(
                "sessionId", sessionId(),
                "targetName", "Database Connection",
                "targetType", "SERVICE",
                "checkStatus", "WARN",
                "summary", "High connection pool usage"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.checkStatus"))
                .isEqualTo("WARN");
    }

    @Test
    void shouldCreateCheckWithFailStatus() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/environment-readiness", Map.of(
                "sessionId", sessionId(),
                "targetName", "Redis",
                "targetType", "SERVICE",
                "checkStatus", "FAIL",
                "summary", "Connection refused"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.checkStatus"))
                .isEqualTo("FAIL");
    }

    @Test
    void shouldGetCheckById() {
        ResponseEntity<String> res = get("/api/environment-readiness/" + checkId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(checkId());
        assertThat(TestJsonHelper.getString(data, "targetName")).isEqualTo("Docker Service");
    }

    @Test
    void shouldListChecksByProject() {
        ResponseEntity<String> res = get("/api/environment-readiness?projectId=" + projectId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.elements().hasNext()).isTrue();
    }

    @Test
    void shouldListChecksBySession() {
        ResponseEntity<String> res = get("/api/environment-readiness?sessionId=" + sessionId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
    }

    @Test
    void shouldGetDashboardWithAggregatedData() {
        // Create multiple sessions and feedback to populate dashboard
        for (int i = 0; i < 3; i++) {
            String s = String.valueOf(System.currentTimeMillis()) + "_" + i;
            ResponseEntity<String> sRes = post("/api/beta-sessions", Map.of(
                    "projectId", projectId(),
                    "title", "Dashboard Session " + s
            ));
            assertOk(sRes);
            String sid = TestJsonHelper.getString(TestJsonHelper.parse(sRes.getBody()), "data.id");
            put("/api/beta-sessions/" + sid, Map.of("sessionStatus", "IN_PROGRESS"));
            put("/api/beta-sessions/" + sid, Map.of(
                    "sessionStatus", "COMPLETED",
                    "satisfactionScore", 7 + i,
                    "continueIntent", "YES"
            ));

            // Add feedback
            post("/api/beta-sessions/" + sid + "/feedback", Map.of(
                    "severity", i == 0 ? "P0" : "P1",
                    "title", "Feedback " + s,
                    "releaseBlocking", i == 0
            ));
        }

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta-dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "totalSessions")).isGreaterThanOrEqualTo(4);
        assertThat(TestJsonHelper.getLong(data, "completedSessions")).isGreaterThanOrEqualTo(3);
        assertThat(TestJsonHelper.getLong(data, "averageSatisfactionScore")).isPositive();
        assertThat(TestJsonHelper.getLong(data, "p0Count")).isPositive();
        assertThat(TestJsonHelper.getLong(data, "readinessPassCount")).isPositive();
    }

    // ========================
    // 2. Error paths
    // ========================

    @Test
    void shouldReturn404ForNonExistentCheck() {
        ResponseEntity<String> res = get("/api/environment-readiness/99999999");
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldReturn400WhenNoProjectOrSessionProvided() {
        ResponseEntity<String> res = get("/api/environment-readiness");
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void shouldReturn400ForInvalidProjectIdOnCreate() {
        ResponseEntity<String> res = post("/api/projects/invalid-id/environment-readiness", Map.of(
                "targetName", "Test",
                "targetType", "SERVICE",
                "checkStatus", "PASS"
        ));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }
}
