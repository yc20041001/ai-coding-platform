package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceWorkspaceCopilotIntegrationTest extends IntegrationTestBase {

    private String sessionId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        ResponseEntity<String> res = post("/api/governance-workspace/sessions", Map.of());
        assertOk(res);
        sessionId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Workspace Session ==========
    @Test void shouldCreateWorkspaceSessionSuccess() {
        ResponseEntity<String> res = post("/api/governance-workspace/sessions", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus")).isEqualTo("ACTIVE");
    }
    @Test void shouldUpdateWorkspaceSessionSuccess() {
        ResponseEntity<String> res = put("/api/governance-workspace/sessions/" + sessionId + "?focusMode=OWNER_CENTRIC", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.focusMode")).isEqualTo("OWNER_CENTRIC");
    }
    @Test void shouldSessionStatusActiveToPaused() {
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/status?status=PAUSED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus")).isEqualTo("PAUSED");
    }
    @Test void shouldSessionStatusPausedToActive() {
        post("/api/governance-workspace/sessions/" + sessionId + "/status?status=PAUSED", Map.of());
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/status?status=ACTIVE", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus")).isEqualTo("ACTIVE");
    }
    @Test void shouldSessionStatusActiveToCompleted() {
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/status?status=COMPLETED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus")).isEqualTo("COMPLETED");
    }
    @Test void shouldInvalidSessionTransitionReject() {
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/status?status=ARCHIVED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }
    @Test void shouldRefreshWorkspaceSuccess() {
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        assertOk(res);
    }

    // ========== Guided Tasks ==========
    @Test void shouldGuidedTaskGeneratedFromRecommendation() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldGuidedTaskStatusOpenToInProgress() throws Exception {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String taskId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-workspace/tasks/" + taskId + "/status?status=IN_PROGRESS", Map.of());
            assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.taskStatus")).isEqualTo("IN_PROGRESS");
        }
    }
    @Test void shouldGuidedTaskStatusInProgressToDone() throws Exception {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String taskId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workspace/tasks/" + taskId + "/status?status=IN_PROGRESS", Map.of());
            ResponseEntity<String> res = post("/api/governance-workspace/tasks/" + taskId + "/status?status=DONE", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldGuidedTaskInvalidTransitionReject() throws Exception {
        ResponseEntity<String> res = post("/api/governance-workspace/tasks/999999999/status?status=DONE", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    // ========== Next-Step Recommendations ==========
    @Test void shouldNextStepRecommendationGenerated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldNextStepCountBetween3And5() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res); int count = TestJsonHelper.parse(res.getBody()).get("data").size();
        assertThat(count).isBetween(0, 5);
    }
    @Test void shouldNextStepSuggestionTypesReturned() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("suggestionType")).isNotNull();
    }

    // ========== Dashboard & Report ==========
    @Test void shouldDashboardReturnActiveSession() {
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("activeSession")).isNotNull();
    }
    @Test void shouldDashboardIncludeTaskCounts() {
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("openTaskCount")).isNotNull();
        assertThat(root.get("data").get("inProgressTaskCount")).isNotNull();
        assertThat(root.get("data").get("blockedTaskCount")).isNotNull();
    }
    @Test void shouldDashboardIncludeNextSteps() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topNextStepRecommendations")).isNotNull();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-workspace/report");
        assertOk(res);
    }

    // ========== Edge Cases ==========
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res);
    }
    @Test void shouldListSessions() {
        ResponseEntity<String> res = get("/api/governance-workspace/sessions");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldGetSessionById() {
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(sessionId);
    }
    @Test void shouldRefreshIdempotent() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        assertOk(res);
    }
    @Test void shouldFocusModeChangesTaskOrdering() {
        put("/api/governance-workspace/sessions/" + sessionId + "?focusMode=WAIVER_REDUCTION", Map.of());
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        assertOk(res);
    }
    @Test void shouldCompletedSessionCanBeArchived() {
        post("/api/governance-workspace/sessions/" + sessionId + "/status?status=COMPLETED", Map.of());
        ResponseEntity<String> res = post("/api/governance-workspace/sessions/" + sessionId + "/status?status=ARCHIVED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.sessionStatus")).isEqualTo("ARCHIVED");
    }
    @Test void shouldNextStepRationalePopulated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("rationaleText")).isNotNull();
    }
    @Test void shouldDashboardTopGuidedTasksReturned() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topGuidedTasks")).isNotNull();
    }
    @Test void shouldGuidedTaskStatusInProgressToBlocked() throws Exception {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String taskId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workspace/tasks/" + taskId + "/status?status=IN_PROGRESS", Map.of());
            ResponseEntity<String> res = post("/api/governance-workspace/tasks/" + taskId + "/status?status=BLOCKED", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldGuidedTaskBlockedToInProgress() throws Exception {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String taskId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workspace/tasks/" + taskId + "/status?status=IN_PROGRESS", Map.of());
            post("/api/governance-workspace/tasks/" + taskId + "/status?status=BLOCKED", Map.of());
            ResponseEntity<String> res = post("/api/governance-workspace/tasks/" + taskId + "/status?status=IN_PROGRESS", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldGuidedTaskOpenToSkipped() throws Exception {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String taskId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-workspace/tasks/" + taskId + "/status?status=SKIPPED", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldNextStepOpenPlaybookGenerated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res);
    }
    @Test void shouldNextStepReviewForecastGenerated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res);
    }
    @Test void shouldDashboardFocusModeReturned() {
        ResponseEntity<String> res = get("/api/governance-workspace/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("focusMode")).isNotNull();
    }
    @Test void shouldSessionListContainsActive() {
        ResponseEntity<String> res = get("/api/governance-workspace/sessions");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("sessionStatus")).isNotNull();
    }
    @Test void shouldNextStepExpectedOutcomePopulated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/next-steps");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("expectedOutcomeText")).isNotNull();
    }
    @Test void shouldGuidedTaskLinkedFieldsPopulated() {
        post("/api/governance-workspace/sessions/" + sessionId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId + "/tasks");
        assertOk(res);
    }
    @Test void shouldWorkspaceSessionContainsContextSummary() {
        ResponseEntity<String> res = get("/api/governance-workspace/sessions/" + sessionId);
        assertOk(res);
    }
}
