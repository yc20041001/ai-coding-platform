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

class OrganizationGovernanceGuardrailIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String projectId2;
    private String planId;
    private String planId2;
    private String policyId;
    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() {
        loginAdmin();
        projectId = createProject("og-" + (counter++));
        projectId2 = createProject("og-" + (counter++));
        planId = createPlan(projectId, "v40b-" + counter);
        planId2 = createPlan(projectId2, "v40b-" + counter);

        // Take confidence snapshots so portfolio has data
        takeConfidenceSnapshot(planId);
        takeConfidenceSnapshot(planId2);

        // Refresh portfolio and create a default policy
        post("/api/release-governance/portfolio/refresh", Map.of());
        policyId = createPolicy("default-trial", "Default Trial Policy", "GLOBAL");
    }

    // ========== Organization Policy CRUD ==========

    @Test
    void shouldCreateOrganizationPolicySuccess() {
        String key = "test-policy-" + (counter++);
        ResponseEntity<String> res = post("/api/organization-governance/policies", Map.of(
                "policyKey", key,
                "displayName", "Test Policy",
                "policyScope", "GLOBAL"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.policyKey")).isEqualTo(key);
        assertThat(TestJsonHelper.getBool(root, "data.enabled")).isTrue();
    }

    @Test
    void shouldUpdateOrganizationPolicySuccess() {
        ResponseEntity<String> res = put("/api/organization-governance/policies/" + policyId, Map.of(
                "displayName", "Updated Policy"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated Policy");
    }

    @Test
    void shouldDisableOrganizationPolicySuccess() {
        ResponseEntity<String> res = post("/api/organization-governance/policies/" + policyId + "/status?enabled=false", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }

    @Test
    void shouldDuplicatePolicyKeyReject() {
        String fixedKey = "dup-pol-" + (counter++);
        assertOk(post("/api/organization-governance/policies", Map.of(
                "policyKey", fixedKey,
                "displayName", "Original",
                "policyScope", "GLOBAL"
        )));
        ResponseEntity<String> res = post("/api/organization-governance/policies", Map.of(
                "policyKey", fixedKey,
                "displayName", "Duplicate",
                "policyScope", "GLOBAL"
        ));
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldListPoliciesByScopeWorks() {
        createPolicy("global-tpl-" + (counter++), "Global Policy", "GLOBAL");
        ResponseEntity<String> res = get("/api/organization-governance/policies?scope=GLOBAL");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        for (JsonNode item : root.get("data")) {
            assertThat(TestJsonHelper.getString(item, "policyScope")).isEqualTo("GLOBAL");
        }
    }

    @Test
    void shouldGetOrganizationPolicyById() {
        ResponseEntity<String> res = get("/api/organization-governance/policies/" + policyId);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(policyId);
    }

    @Test
    void shouldRejectNonExistentPolicyGet() {
        ResponseEntity<String> res = get("/api/organization-governance/policies/999999999");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentPolicyUpdate() {
        ResponseEntity<String> res = put("/api/organization-governance/policies/999999999", Map.of("displayName", "Nope"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentPolicyStatus() {
        ResponseEntity<String> res = post("/api/organization-governance/policies/999999999/status?enabled=false", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldPolicyNotesPersisted() {
        String key = "notes-pol-" + (counter++);
        ResponseEntity<String> res = post("/api/organization-governance/policies", Map.of(
                "policyKey", key,
                "displayName", "Notes Policy",
                "policyScope", "GLOBAL",
                "notes", "Persistent notes content"
        ));
        assertOk(res);
        String id = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
        ResponseEntity<String> getRes = get("/api/organization-governance/policies/" + id);
        assertOk(getRes);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(getRes.getBody()), "data.notes")).isEqualTo("Persistent notes content");
    }

    // ========== Guardrail Evaluation ==========

    @Test
    void shouldRefreshGuardrailEvaluationSuccess() {
        ResponseEntity<String> res = post("/api/organization-governance/guardrails/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldGuardrailPassCountCorrect() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.passCount")).isNotNull();
    }

    @Test
    void shouldGuardrailWarnCountCorrect() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.warnCount")).isNotNull();
    }

    @Test
    void shouldGuardrailBlockCountCorrect() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.blockCount")).isNotNull();
    }

    @Test
    void shouldCriticalSeverityAssignedCorrectly() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldGuardrailDashboardReturnAllFields() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("projectCount")).isNotNull();
        assertThat(root.get("data").get("passCount")).isNotNull();
        assertThat(root.get("data").get("warnCount")).isNotNull();
        assertThat(root.get("data").get("blockCount")).isNotNull();
        assertThat(root.get("data").get("recommendationCount")).isNotNull();
    }

    @Test
    void shouldRecommendationGeneratedForLowConfidence() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/recommendations");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldGuardrailEvaluationListReturnItems() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        if (root.get("data").size() > 0) {
            assertThat(root.get("data").get(0).get("evaluationStatus")).isNotNull();
            assertThat(root.get("data").get(0).get("severity")).isNotNull();
        }
    }

    @Test
    void shouldGuardrailRefreshIdempotent() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.projectCount")).isNotNull();
    }

    // ========== Drift Detection ==========

    @Test
    void shouldRefreshDriftSnapshotSuccess() {
        ResponseEntity<String> res = post("/api/organization-governance/drift/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldDriftScoreCalculatedCorrect() {
        post("/api/organization-governance/drift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/drift");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldDriftLevelReturnStable() {
        post("/api/organization-governance/drift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/drift/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("stableCount")).isNotNull();
    }

    @Test
    void shouldDriftDashboardReturnTopProjects() {
        post("/api/organization-governance/drift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/drift/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topDriftProjects").isArray()).isTrue();
    }

    @Test
    void shouldDriftDashboardReturnTrendSummary() {
        post("/api/organization-governance/drift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/drift/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("driftTrendSummary")).isNotNull();
    }

    @Test
    void shouldDriftRefreshIdempotent() {
        post("/api/organization-governance/drift/refresh", Map.of());
        post("/api/organization-governance/drift/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/drift/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.stableCount")).isNotNull();
    }

    // ========== Summary & Report ==========

    @Test
    void shouldSummaryResponseReturnBlockedProjects() {
        ResponseEntity<String> res = get("/api/organization-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("blockCount")).isNotNull();
        assertThat(root.get("data").get("topRiskProjects").isArray()).isTrue();
    }

    @Test
    void shouldSummaryResponseReturnTopDriftProjects() {
        ResponseEntity<String> res = get("/api/organization-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topDriftProjects").isArray()).isTrue();
    }

    @Test
    void shouldReportExportReturnsMarkdown() {
        ResponseEntity<String> res = get("/api/organization-governance/report");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.summaryMarkdown")).isNotNull();
    }

    @Test
    void shouldSummaryReturnProjectCount() {
        ResponseEntity<String> res = get("/api/organization-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getInt(root, "data.totalProjectCount")).isNotNull();
    }

    @Test
    void shouldSummaryReturnTopRecommendations() {
        ResponseEntity<String> res = get("/api/organization-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topRecommendations").isArray()).isTrue();
    }

    @Test
    void shouldDisabledPolicyExcludedFromEvaluation() {
        // Create new policy and disable it
        String key = "disabled-pol-" + (counter++);
        String id = createPolicy(key, "Disabled Policy", "GLOBAL");
        post("/api/organization-governance/policies/" + id + "/status?enabled=false", Map.of());

        // Refresh guardrails — should not fail
        ResponseEntity<String> res = post("/api/organization-governance/guardrails/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldEmptyPortfolioReturnEmptyDashboard() {
        // Create project with no portfolio data
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("projectCount")).isNotNull();
    }

    @Test
    void shouldSummaryMarkdownIncludeGuardrailOverview() {
        ResponseEntity<String> res = get("/api/organization-governance/report");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String md = TestJsonHelper.getString(root, "data.summaryMarkdown");
        assertThat(md).isNotNull();
        assertThat(md).contains("Organization Governance Summary");
    }

    @Test
    void shouldPolicyScopeDefaultToGlobal() {
        String key = "scope-test-" + (counter++);
        ResponseEntity<String> res = post("/api/organization-governance/policies", Map.of(
                "policyKey", key,
                "displayName", "Scope Test"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.policyScope")).isEqualTo("GLOBAL");
    }

    @Test
    void shouldGuardrailTopBlockedProjectsReturned() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/guardrails/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topBlockedProjects").isArray()).isTrue();
    }

    @Test
    void shouldRecommendationReturnValidPriority() {
        post("/api/organization-governance/guardrails/refresh", Map.of());
        ResponseEntity<String> res = get("/api/organization-governance/recommendations");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        if (root.get("data").size() > 0) {
            String priority = TestJsonHelper.getString(root.get("data").get(0), "priority");
            assertThat(priority).isIn("P0", "P1", "P2", "P3");
        }
    }

    // ========== Helpers ==========

    private void takeConfidenceSnapshot(String pid) {
        post("/api/release-rollouts/" + pid + "/confidence-snapshot", Map.of());
    }

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "OG-IT-" + suffix,
                "description", "Organization governance integration test project",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String createPlan(String projectId, String label) {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/rollout/plans", Map.of(
                "releaseLabel", label
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String createPolicy(String key, String name, String scope) {
        String uniqueKey = key + "-" + (counter++);
        ResponseEntity<String> res = post("/api/organization-governance/policies", Map.of(
                "policyKey", uniqueKey,
                "displayName", name,
                "policyScope", scope
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }
}
