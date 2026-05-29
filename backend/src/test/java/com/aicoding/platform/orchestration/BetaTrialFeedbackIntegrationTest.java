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

class BetaTrialFeedbackIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String sessionIdValue;
    private String feedbackIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;
        String suffix = String.valueOf(System.currentTimeMillis());

        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-BetaFeedback-" + suffix,
                "description", "Beta trial feedback integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        ResponseEntity<String> sessionRes = post("/api/beta-sessions", Map.of(
                "projectId", projectIdValue,
                "title", "Feedback Test Session " + suffix
        ));
        assertOk(sessionRes);
        sessionIdValue = TestJsonHelper.getString(TestJsonHelper.parse(sessionRes.getBody()), "data.id");

        // Create feedback entry
        ResponseEntity<String> fbRes = post("/api/beta-sessions/" + sessionIdValue + "/feedback", Map.of(
                "severity", "P2",
                "title", "Test feedback item",
                "category", "BUG",
                "sourceType", "MANUAL",
                "releaseBlocking", false
        ));
        assertOk(fbRes);
        feedbackIdValue = TestJsonHelper.getString(TestJsonHelper.parse(fbRes.getBody()), "data.id");
    }

    private String projectId() {
        ensureTestData();
        return Objects.requireNonNull(projectIdValue);
    }

    private String sessionId() {
        ensureTestData();
        return Objects.requireNonNull(sessionIdValue);
    }

    private String feedbackId() {
        ensureTestData();
        return Objects.requireNonNull(feedbackIdValue);
    }

    // ========================
    // 1. Happy path
    // ========================

    @Test
    void shouldCreateFeedbackSuccessfully() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/beta-sessions/" + sessionId() + "/feedback", Map.of(
                "severity", "P1",
                "title", "Critical bug found " + suffix,
                "category", "BUG",
                "subcategory", "BACKEND_ERROR",
                "detail", "Null pointer exception on login",
                "sourceType", "MANUAL",
                "releaseBlocking", true
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("P1");
        assertThat(TestJsonHelper.getString(data, "triageStatus")).isEqualTo("NEW");
        assertThat(TestJsonHelper.getBool(data, "releaseBlocking")).isTrue();
    }

    @Test
    void shouldGetFeedbackById() {
        ResponseEntity<String> res = get("/api/beta-feedback/" + feedbackId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(feedbackId());
        assertThat(TestJsonHelper.getString(data, "title")).isNotEmpty();
    }

    @Test
    void shouldUpdateFeedbackTriageStatus() {
        ResponseEntity<String> res = put("/api/beta-feedback/" + feedbackId(), Map.of(
                "triageStatus", "TRIAGED",
                "category", "BUG"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "triageStatus")).isEqualTo("TRIAGED");
    }

    @Test
    void shouldUpdateFeedbackFullFlow() {
        // NEW -> TRIAGED -> SCHEDULED -> DONE
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions/" + sessionId() + "/feedback", Map.of(
                "severity", "P0",
                "title", "Triage flow test " + suffix,
                "releaseBlocking", true
        ));
        assertOk(createRes);
        String fid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        // TRIAGED
        put("/api/beta-feedback/" + fid, Map.of("triageStatus", "TRIAGED"));
        ResponseEntity<String> scheduledRes = put("/api/beta-feedback/" + fid,
                Map.of("triageStatus", "SCHEDULED"));
        assertOk(scheduledRes);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(scheduledRes.getBody()), "data.triageStatus"))
                .isEqualTo("SCHEDULED");

        // DONE
        ResponseEntity<String> doneRes = put("/api/beta-feedback/" + fid,
                Map.of("triageStatus", "DONE"));
        assertOk(doneRes);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(doneRes.getBody()), "data.triageStatus"))
                .isEqualTo("DONE");
    }

    @Test
    void shouldListFeedbackWithFilters() {
        // List all
        ResponseEntity<String> allRes = get("/api/beta-sessions/" + sessionId() + "/feedback");
        assertOk(allRes);
        JsonNode allData = TestJsonHelper.parse(allRes.getBody()).get("data");
        assertThat(allData.isArray()).isTrue();

        // Filter by severity
        ResponseEntity<String> filteredRes = get("/api/beta-sessions/" + sessionId()
                + "/feedback?severity=P2&triageStatus=NEW");
        assertOk(filteredRes);
        JsonNode filteredData = TestJsonHelper.parse(filteredRes.getBody()).get("data");
        assertThat(filteredData.isArray()).isTrue();
    }

    @Test
    void shouldGetPassBlockSummary() {
        // Create P0 release-blocking feedback
        post("/api/beta-sessions/" + sessionId() + "/feedback", Map.of(
                "severity", "P0",
                "title", "Release blocker",
                "releaseBlocking", true
        ));

        ResponseEntity<String> res = get("/api/beta-sessions/" + sessionId()
                + "/feedback/pass-block-summary");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "totalFeedback")).isPositive();
        assertThat(TestJsonHelper.getLong(data, "p0Count")).isPositive();
    }

    @Test
    void shouldDeleteFeedback() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> createRes = post("/api/beta-sessions/" + sessionId() + "/feedback", Map.of(
                "severity", "P3",
                "title", "To be deleted " + suffix
        ));
        assertOk(createRes);
        String fid = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> delRes = delete("/api/beta-feedback/" + fid);
        assertOk(delRes);

        // Verify deleted
        ResponseEntity<String> getRes = get("/api/beta-feedback/" + fid);
        assertThat(getRes.getStatusCodeValue()).isEqualTo(404);
    }

    // ========================
    // 2. Error paths
    // ========================

    @Test
    void shouldReturn404ForNonExistentFeedback() {
        ResponseEntity<String> res = get("/api/beta-feedback/99999999");
        assertThat(res.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void shouldReturn404ForFeedbackInNonExistentSession() {
        ResponseEntity<String> res = post("/api/beta-sessions/99999999/feedback", Map.of(
                "severity", "P2",
                "title", "Orphan feedback"
        ));
        assertThat(res.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void shouldReturn404ForNonExistentSessionOnList() {
        ResponseEntity<String> res = get("/api/beta-sessions/99999999/feedback");
        assertThat(res.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void shouldReturn404ForNonExistentSessionOnSummary() {
        ResponseEntity<String> res = get("/api/beta-sessions/99999999/feedback/pass-block-summary");
        assertThat(res.getStatusCodeValue()).isEqualTo(404);
    }
}
