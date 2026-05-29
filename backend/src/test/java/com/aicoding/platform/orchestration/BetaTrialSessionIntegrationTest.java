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

class BetaTrialSessionIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String sessionIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;
        String suffix = String.valueOf(System.currentTimeMillis());

        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-BetaSession-" + suffix,
                "description", "Beta trial session integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        ResponseEntity<String> sessionRes = post("/api/beta-sessions", Map.of(
                "projectId", projectIdValue,
                "title", "Test Session " + suffix,
                "participantRole", "DEVELOPER",
                "environmentType", "LOCAL",
                "providerMode", "MOCK",
                "githubOauthStatus", "NOT_CONFIGURED"
        ));
        assertOk(sessionRes);
        sessionIdValue = TestJsonHelper.getString(TestJsonHelper.parse(sessionRes.getBody()), "data.id");
    }

    private String projectId() {
        ensureTestData();
        return Objects.requireNonNull(projectIdValue);
    }

    private String sessionId() {
        ensureTestData();
        return Objects.requireNonNull(sessionIdValue);
    }

    // ========================
    // 1. Happy path
    // ========================

    @Test
    void shouldCreateSessionSuccessfully() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/beta-sessions", Map.of(
                "projectId", projectId(),
                "title", "New Session " + suffix,
                "participantRole", "DEVELOPER",
                "environmentType", "DOCKER_COMPOSE",
                "providerMode", "REAL_MODEL",
                "githubOauthStatus", "NOT_CONFIGURED"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "sessionStatus")).isEqualTo("PLANNED");
        assertThat(TestJsonHelper.getString(data, "title")).contains("New Session");
    }

    @Test
    void shouldGetSessionById() {
        ResponseEntity<String> res = get("/api/beta-sessions/" + sessionId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(sessionId());
        assertThat(TestJsonHelper.getString(data, "title")).isNotEmpty();
    }

    @Test
    void shouldUpdateSessionStatusToInProgress() {
        ResponseEntity<String> res = put("/api/beta-sessions/" + sessionId(),
                Map.of("sessionStatus", "IN_PROGRESS"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "sessionStatus")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldTransitionFromInProgressToCompleted() {
        // First set to IN_PROGRESS
        put("/api/beta-sessions/" + sessionId(), Map.of("sessionStatus", "IN_PROGRESS"));

        ResponseEntity<String> res = put("/api/beta-sessions/" + sessionId(), Map.of(
                "sessionStatus", "COMPLETED",
                "satisfactionScore", 8,
                "continueIntent", "YES",
                "summary", "Trial completed successfully"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "sessionStatus")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getInt(data, "satisfactionScore")).isEqualTo(8);
        assertThat(TestJsonHelper.getString(data, "continueIntent")).isEqualTo("YES");
    }

    @Test
    void shouldTransitionFromInProgressToBlockedAndResume() {
        // Create a dedicated session for blocking flow
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions", Map.of(
                "projectId", projectId(),
                "title", "Block Test " + suffix
        ));
        assertOk(createRes);
        String blockSessionId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        // PLANNED -> IN_PROGRESS
        put("/api/beta-sessions/" + blockSessionId, Map.of("sessionStatus", "IN_PROGRESS"));

        // IN_PROGRESS -> BLOCKED
        ResponseEntity<String> blockRes = put("/api/beta-sessions/" + blockSessionId, Map.of(
                "sessionStatus", "BLOCKED",
                "blockedAtStep", "setup-env",
                "blockerSummary", "Docker not available"
        ));
        assertOk(blockRes);
        JsonNode blockedData = TestJsonHelper.parse(blockRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(blockedData, "sessionStatus")).isEqualTo("BLOCKED");

        // BLOCKED -> IN_PROGRESS (resume)
        ResponseEntity<String> resumeRes = put("/api/beta-sessions/" + blockSessionId,
                Map.of("sessionStatus", "IN_PROGRESS"));
        assertOk(resumeRes);
        JsonNode resumedData = TestJsonHelper.parse(resumeRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(resumedData, "sessionStatus")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldCancelPlannedSession() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions", Map.of(
                "projectId", projectId(),
                "title", "Cancel Test " + suffix
        ));
        assertOk(createRes);
        String cancelId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/beta-sessions/" + cancelId,
                Map.of("sessionStatus", "CANCELED"));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus"))
                .isEqualTo("CANCELED");
    }

    @Test
    void shouldListSessionsByProject() {
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta-sessions");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.elements().hasNext()).isTrue();
    }

    @Test
    void shouldExportSessionMarkdown() {
        // Ensure session has some data
        put("/api/beta-sessions/" + sessionId(), Map.of("sessionStatus", "IN_PROGRESS"));
        put("/api/beta-sessions/" + sessionId(), Map.of(
                "sessionStatus", "COMPLETED",
                "satisfactionScore", 9,
                "continueIntent", "YES",
                "summary", "Great trial session"
        ));

        ResponseEntity<String> res = get("/api/beta-sessions/" + sessionId() + "/export-markdown");
        assertOk(res);
        String markdown = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data");
        assertThat(markdown).contains("# Beta 试用报告");
        assertThat(markdown).contains("COMPLETED");
    }

    // ========================
    // 2. Error paths
    // ========================

    @Test
    void shouldReturn404ForNonExistentSession() {
        ResponseEntity<String> res = get("/api/beta-sessions/99999999");
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions", Map.of(
                "projectId", projectId(),
                "title", "Invalid Transition " + suffix
        ));
        assertOk(createRes);
        String sid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        // PLANNED -> COMPLETED (invalid)
        ResponseEntity<String> res = put("/api/beta-sessions/" + sid,
                Map.of("sessionStatus", "COMPLETED"));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void shouldRejectCompletedToAnything() {
        // Create a session and complete it
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions", Map.of(
                "projectId", projectId(),
                "title", "Already Done " + suffix
        ));
        assertOk(createRes);
        String sid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");
        put("/api/beta-sessions/" + sid, Map.of("sessionStatus", "IN_PROGRESS"));
        put("/api/beta-sessions/" + sid, Map.of("sessionStatus", "COMPLETED"));

        // COMPLETED -> IN_PROGRESS (invalid)
        ResponseEntity<String> res = put("/api/beta-sessions/" + sid,
                Map.of("sessionStatus", "IN_PROGRESS"));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void shouldReturn400ForInvalidSessionIdFormat() {
        ResponseEntity<String> res = get("/api/beta-sessions/invalid-id");
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }
}
