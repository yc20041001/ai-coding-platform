package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class BetaReleaseGateEvaluationIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-ReleaseGate-" + suffix,
                "description", "Release gate evaluation integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private String projectId() {
        return Objects.requireNonNull(projectIdValue);
    }

    // ================================================================
    // 1. Evaluation - Happy paths
    // ================================================================

    @Test
    void shouldEvaluateWithNoData() {
        // When no feedback, incidents, or other data exists, all 9 rules still
        // evaluate. PR_REVIEW_ADOPTION_RATIO (GTE 0.30, actual=0) is BLOCK.
        ResponseEntity<String> res = post(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(9);

        int passCount = 0;
        int blockCount = 0;
        for (int i = 0; i < data.size(); i++) {
            JsonNode eval = data.get(i);
            String status = TestJsonHelper.getString(eval, "gateStatus");
            String ruleKey = TestJsonHelper.getString(eval, "ruleKey");
            assertThat(TestJsonHelper.getString(eval, "evaluationType")).isEqualTo("MANUAL");
            assertThat(TestJsonHelper.getString(eval, "projectId")).isEqualTo(projectId());
            assertThat(TestJsonHelper.getString(eval, "id")).isNotEmpty();
            if ("PASS".equals(status)) {
                passCount++;
            } else if ("BLOCK".equals(status)) {
                blockCount++;
                assertThat(ruleKey).isEqualTo("PR_REVIEW_ADOPTION_RATIO");
            }
        }
        assertThat(passCount).isEqualTo(8);
        assertThat(blockCount).isEqualTo(1);
    }

    @Test
    void shouldEvaluateAndPersistEvaluations() {
        // Evaluate once to persist evaluations
        ResponseEntity<String> evalRes = post(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());
        assertOk(evalRes);

        // Retrieve evaluations via list endpoint
        ResponseEntity<String> listRes = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(9);

        // Verify each evaluation has required fields
        for (int i = 0; i < data.size(); i++) {
            JsonNode eval = data.get(i);
            assertThat(TestJsonHelper.getString(eval, "id")).isNotEmpty();
            assertThat(TestJsonHelper.getString(eval, "ruleKey")).isNotEmpty();
            assertThat(TestJsonHelper.getString(eval, "gateStatus")).isIn("PASS", "BLOCK", "WARN", "SKIP");
            assertThat(TestJsonHelper.getString(eval, "evaluatedAt")).isNotEmpty();
        }
    }

    @Test
    void shouldEvaluateWithSpecifiedTarget() {
        String target = "release-v2-rc1";

        ResponseEntity<String> res = post(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluate?"
                        + "evaluationType=MANUAL&evaluationTarget=" + target,
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(9);

        for (int i = 0; i < data.size(); i++) {
            assertThat(TestJsonHelper.getString(data.get(i), "evaluationTarget"))
                    .isEqualTo(target);
        }
    }

    // ================================================================
    // 2. List evaluations - various filters
    // ================================================================

    @Test
    void shouldListEvaluations() {
        // Seed evaluations
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        ResponseEntity<String> res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        // At minimum the 9 rules created above
        assertThat(data.size()).isGreaterThanOrEqualTo(9);
    }

    @Test
    void shouldListEvaluationsWithTargetFilter() {
        // Create evaluations with a known target
        String target = "release-filter-target";
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?"
                        + "evaluationType=MANUAL&evaluationTarget=" + target,
                Map.of());

        // Filter by that target
        ResponseEntity<String> res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations"
                        + "?evaluationTarget=" + target);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(9);
        for (int i = 0; i < data.size(); i++) {
            assertThat(TestJsonHelper.getString(data.get(i), "evaluationTarget"))
                    .isEqualTo(target);
        }

        // Filter by a non-existent target should return empty
        ResponseEntity<String> emptyRes = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations"
                        + "?evaluationTarget=nonexistent-target");
        assertOk(emptyRes);
        JsonNode emptyData = TestJsonHelper.parse(emptyRes.getBody()).get("data");
        assertThat(emptyData.isArray()).isTrue();
        assertThat(emptyData.size()).isEqualTo(0);
    }

    @Test
    void shouldListEvaluationsWithPagination() {
        // Create multiple batches of evaluations
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        // Page 1 with size 5 — should return at most 5
        ResponseEntity<String> page1res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations?page=1&size=5");
        assertOk(page1res);
        JsonNode page1 = TestJsonHelper.parse(page1res.getBody()).get("data");
        assertThat(page1.isArray()).isTrue();
        assertThat(page1.size()).isLessThanOrEqualTo(5);
        if (page1.size() > 0) {
            assertThat(TestJsonHelper.getString(page1.get(0), "id")).isNotEmpty();
        }

        // Page 2 with size 5
        ResponseEntity<String> page2res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations?page=2&size=5");
        assertOk(page2res);
        JsonNode page2 = TestJsonHelper.parse(page2res.getBody()).get("data");
        assertThat(page2.isArray()).isTrue();
    }

    @Test
    void shouldReturnEvaluationsSortedByDateDesc() {
        // Create first batch
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        // Create second batch with a small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        // List all evaluations — they should be sorted by evaluatedAt descending
        ResponseEntity<String> res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        // At least 18 evaluations across both batches
        assertThat(data.size()).isGreaterThanOrEqualTo(18);

        // Verify ordering: evaluatedAt values should be non-increasing
        String prev = null;
        for (int i = 0; i < data.size(); i++) {
            String evaluatedAt = TestJsonHelper.getString(data.get(i), "evaluatedAt");
            if (prev != null) {
                assertThat(evaluatedAt).isLessThanOrEqualTo(prev);
            }
            prev = evaluatedAt;
        }
    }

    // ================================================================
    // 3. Dashboard
    // ================================================================

    @Test
    void shouldGetGateDashboard() {
        // Evaluate to populate dashboard data
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        ResponseEntity<String> res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        // Summary
        JsonNode summary = data.get("summary");
        assertThat(summary).isNotNull();
        assertThat(TestJsonHelper.getLong(summary, "totalRules")).isGreaterThanOrEqualTo(9);
        assertThat(TestJsonHelper.getLong(summary, "passCount")).isEqualTo(8);
        assertThat(TestJsonHelper.getLong(summary, "blockingFailures")).isEqualTo(0);
        assertThat(TestJsonHelper.getLong(summary, "warningCount")).isEqualTo(1);
        assertThat(TestJsonHelper.getString(summary, "overallStatus")).isEqualTo("WARN");

        // Evaluations list
        JsonNode evaluations = data.get("evaluations");
        assertThat(evaluations.isArray()).isTrue();
        assertThat(evaluations.size()).isEqualTo(9);

        // Recent decisions
        JsonNode recentDecisions = data.get("recentDecisions");
        assertThat(recentDecisions.isArray()).isTrue();
        assertThat(recentDecisions.size()).isEqualTo(0);
    }

    @Test
    void shouldGetDashboardWithRecentDecisions() {
        // Evaluate first so decision can count blocking/warning issues
        post("/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());

        // Create a release decision
        ResponseEntity<String> decisionRes = post(
                "/api/projects/" + projectId() + "/beta/release-gate/decisions",
                Map.of(
                        "releaseLabel", "v1.0-rc1",
                        "decisionStatus", "CONDITIONAL_GO",
                        "decisionReason", "Non-blocking warnings reviewed",
                        "approverId", AGENT_ID
                ));
        assertOk(decisionRes);

        // Fetch dashboard
        ResponseEntity<String> res = get(
                "/api/projects/" + projectId() + "/beta/release-gate/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        // Verify recent decisions are present
        JsonNode recentDecisions = data.get("recentDecisions");
        assertThat(recentDecisions.isArray()).isTrue();
        assertThat(recentDecisions.size()).isGreaterThanOrEqualTo(1);

        String decisionId = TestJsonHelper.getString(recentDecisions.get(0), "id");
        assertThat(decisionId).isNotEmpty();
        assertThat(TestJsonHelper.getString(recentDecisions.get(0), "decisionStatus"))
                .isEqualTo("CONDITIONAL_GO");
    }

    @Test
    void shouldEvaluateMultipleTimesAndKeepLatestForEachRule() {
        // First evaluation batch
        ResponseEntity<String> firstEval = post(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=MANUAL",
                Map.of());
        assertOk(firstEval);

        // Second evaluation batch
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        ResponseEntity<String> secondEval = post(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluate?evaluationType=SCHEDULED",
                Map.of());
        assertOk(secondEval);

        // List should contain all evaluations from both batches
        ResponseEntity<String> listRes = get(
                "/api/projects/" + projectId() + "/beta/release-gate/evaluations");
        assertOk(listRes);
        JsonNode listData = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(listData.size()).isGreaterThanOrEqualTo(18);

        // Dashboard should only include the latest evaluation per rule (2nd batch)
        ResponseEntity<String> dashRes = get(
                "/api/projects/" + projectId() + "/beta/release-gate/dashboard");
        assertOk(dashRes);
        JsonNode dashData = TestJsonHelper.parse(dashRes.getBody()).get("data");

        // Summary counts should reflect latest batch (same counts as single batch)
        JsonNode summary = dashData.get("summary");
        assertThat(TestJsonHelper.getLong(summary, "totalRules")).isGreaterThanOrEqualTo(9);
        assertThat(TestJsonHelper.getLong(summary, "passCount")).isEqualTo(8);
        assertThat(TestJsonHelper.getLong(summary, "warningCount")).isEqualTo(1);

        // Dashboard evaluations should have non-MANUAL type from 2nd batch
        JsonNode evaluations = dashData.get("evaluations");
        assertThat(evaluations.isArray()).isTrue();
        assertThat(evaluations.size()).isEqualTo(9);
        for (int i = 0; i < evaluations.size(); i++) {
            assertThat(TestJsonHelper.getString(evaluations.get(i), "evaluationType"))
                    .isEqualTo("SCHEDULED");
        }
    }

    // ================================================================
    // 4. Error paths
    // ================================================================

    @Test
    void shouldHandleInvalidProjectId() {
        ResponseEntity<String> res = get(
                "/api/projects/invalid/beta/release-gate/evaluations");
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldReturn404WhenProjectNotFound() {
        ResponseEntity<String> res = get(
                "/api/projects/999999999/beta/release-gate/evaluations");
        assertCode(res, "PROJECT_ACCESS_DENIED");
    }
}
