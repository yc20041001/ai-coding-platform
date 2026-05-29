package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.BetaReleaseGateEvaluationEntity;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateEvaluationMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BetaReleaseDecisionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BetaReleaseGateEvaluationMapper betaReleaseGateEvaluationMapper;

    private String projectIdValue;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-BetaDecision-" + suffix,
                "description", "Beta release decision integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private String projectId() {
        return projectIdValue;
    }

    private String createDecision(String releaseLabel, String decisionStatus) {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/beta/release-gate/decisions",
                Map.of("releaseLabel", releaseLabel, "decisionStatus", decisionStatus));
        assertOk(res);
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private void seedEvaluation(Long projectId, String ruleKey, String category, String gateStatus,
                                BigDecimal actualValue, BigDecimal thresholdValue, Integer blocking, String summary) {
        BetaReleaseGateEvaluationEntity eval = new BetaReleaseGateEvaluationEntity();
        eval.setProjectId(projectId);
        eval.setEvaluationTarget("release-" + System.currentTimeMillis());
        eval.setEvaluationType("MANUAL");
        eval.setRuleKey(ruleKey);
        eval.setCategory(category);
        eval.setGateStatus(gateStatus);
        eval.setActualValue(actualValue);
        eval.setThresholdValue(thresholdValue);
        eval.setBlocking(blocking);
        eval.setSummary(summary);
        eval.setEvaluatedAt(LocalDateTime.now());
        betaReleaseGateEvaluationMapper.insert(eval);
    }

    // ========================
    // Create Decision
    // ========================

    @Test
    void shouldCreateDecisionWithGo() {
        Long pid = Long.valueOf(projectId());
        seedEvaluation(pid, "trial_feedback_satisfaction", "TRIAL_FEEDBACK", "PASS",
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.0), 1, "Satisfaction is good");
        seedEvaluation(pid, "env_readiness_pass_rate", "ENVIRONMENT_READINESS", "PASS",
                BigDecimal.valueOf(95.0), BigDecimal.valueOf(80.0), 1, "Environment passes");

        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/beta/release-gate/decisions",
                Map.of("releaseLabel", "v1.0", "decisionStatus", "GO", "decisionReason", "All gates pass"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("GO");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.0");
        assertThat(TestJsonHelper.getString(data, "decisionReason")).isEqualTo("All gates pass");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "approvedAt")).isNotEmpty();
        // With only PASS evaluations, both counts should be zero
        assertThat(TestJsonHelper.getInt(data, "blockingIssueCount")).isEqualTo(0);
        assertThat(TestJsonHelper.getInt(data, "warningIssueCount")).isEqualTo(0);
        assertThat(TestJsonHelper.getString(data, "reportMarkdown")).isNotEmpty();
    }

    @Test
    void shouldCreateDecisionWithNoGo() {
        Long pid = Long.valueOf(projectId());
        seedEvaluation(pid, "incident_open_count", "INCIDENT_RISK", "BLOCK",
                BigDecimal.valueOf(8), BigDecimal.valueOf(5), 1, "Too many open incidents");

        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/beta/release-gate/decisions",
                Map.of("releaseLabel", "v1.0", "decisionStatus", "NO_GO",
                        "decisionReason", "Critical issues found"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("NO_GO");
        assertThat(TestJsonHelper.getString(data, "decisionReason")).isEqualTo("Critical issues found");
        // NO_GO decision does not set approvedAt
        assertThat(TestJsonHelper.getString(data, "approvedAt")).isEmpty();
        // Blocking count should reflect the seeded evaluation
        assertThat(TestJsonHelper.getInt(data, "blockingIssueCount")).isEqualTo(1);
    }

    @Test
    void shouldCreateDecisionWithConditionalGo() {
        Long pid = Long.valueOf(projectId());
        seedEvaluation(pid, "knowledge_doc_coverage", "KNOWLEDGE_QUALITY", "WARN",
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(50.0), 0, "Low doc coverage");

        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/beta/release-gate/decisions",
                Map.of("releaseLabel", "v1.5", "decisionStatus", "CONDITIONAL_GO",
                        "decisionReason", "Approved with conditions"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("CONDITIONAL_GO");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.5");
        assertThat(TestJsonHelper.getString(data, "decisionReason")).isEqualTo("Approved with conditions");
        assertThat(TestJsonHelper.getString(data, "approvedAt")).isNotEmpty();
    }

    // ========================
    // List Decisions
    // ========================

    @Test
    void shouldListDecisions() {
        createDecision("v2.0", "GO");
        createDecision("v2.1", "NO_GO");

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta/release-gate/decisions");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldListDecisionsWithPagination() {
        createDecision("v3.0", "GO");
        createDecision("v3.1", "NO_GO");
        createDecision("v3.2", "CONDITIONAL_GO");

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta/release-gate/decisions?page=1&size=2");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isLessThanOrEqualTo(2);
    }

    // ========================
    // Get Decision
    // ========================

    @Test
    void shouldGetDecision() {
        String decisionId = createDecision("v4.0", "GO");

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta/release-gate/decisions/" + decisionId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(decisionId);
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("GO");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v4.0");
        assertThat(TestJsonHelper.getString(data, "projectId")).isEqualTo(projectId());
    }

    @Test
    void shouldReturn404WhenGettingNonExistentDecision() {
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta/release-gate/decisions/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Update Decision
    // ========================

    @Test
    void shouldUpdateDecisionStatus() {
        String decisionId = createDecision("v5.0", "NO_GO");

        ResponseEntity<String> res = put("/api/projects/" + projectId() + "/beta/release-gate/decisions/" + decisionId,
                Map.of("decisionStatus", "CONDITIONAL_GO"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("CONDITIONAL_GO");
        assertThat(TestJsonHelper.getString(data, "approvedAt")).isNotEmpty();
    }

    @Test
    void shouldUpdateDecisionReason() {
        String decisionId = createDecision("v6.0", "GO");

        ResponseEntity<String> res = put("/api/projects/" + projectId() + "/beta/release-gate/decisions/" + decisionId,
                Map.of("decisionReason", "Updated reason after review"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decisionReason")).isEqualTo("Updated reason after review");
        assertThat(TestJsonHelper.getString(data, "decisionStatus")).isEqualTo("GO");
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentDecision() {
        ResponseEntity<String> res = put("/api/projects/" + projectId() + "/beta/release-gate/decisions/999999",
                Map.of("decisionStatus", "GO"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Readiness Report
    // ========================

    @Test
    void shouldGenerateReadinessReport() {
        Long pid = Long.valueOf(projectId());
        seedEvaluation(pid, "trial_feedback_satisfaction", "TRIAL_FEEDBACK", "PASS",
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.0), 1, "Satisfaction is good");
        seedEvaluation(pid, "env_readiness_pass_rate", "ENVIRONMENT_READINESS", "PASS",
                BigDecimal.valueOf(95.0), BigDecimal.valueOf(80.0), 1, "Environment is ready");

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/beta/release-gate/readiness-report");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isIn("PASS", "WARN", "BLOCK");
        assertThat(TestJsonHelper.getString(data, "reportMarkdown")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isNotEmpty();
        JsonNode evaluations = data.get("evaluations");
        assertThat(evaluations).isNotNull();
        assertThat(evaluations.isArray()).isTrue();
    }

    @Test
    void shouldGenerateReadinessReportWithReleaseLabel() {
        Long pid = Long.valueOf(projectId());
        seedEvaluation(pid, "model_cost_budget", "MODEL_COST", "PASS",
                BigDecimal.valueOf(500.0), BigDecimal.valueOf(1000.0), 1, "Within budget");

        ResponseEntity<String> res = get("/api/projects/" + projectId()
                + "/beta/release-gate/readiness-report?releaseLabel=v2.0");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v2.0");
        assertThat(TestJsonHelper.getString(data, "overallStatus")).isIn("PASS", "WARN", "BLOCK");
        assertThat(TestJsonHelper.getString(data, "reportMarkdown")).isNotEmpty();
    }

    // ========================
    // Error Paths
    // ========================

    @Test
    void shouldHandleInvalidProjectId() {
        ResponseEntity<String> res = post("/api/projects/invalid/beta/release-gate/decisions",
                Map.of("releaseLabel", "v1.0", "decisionStatus", "GO"));
        assertCode(res, "BAD_REQUEST");
    }
}
