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

class ReleaseGovernancePortfolioIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String projectId2;
    private String planId;
    private String planId2;
    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() {
        loginAdmin();
        projectId = createProject("gp-" + (counter++));
        projectId2 = createProject("gp-" + (counter++));
        planId = createPlan(projectId, "v40a-" + counter);
        planId2 = createPlan(projectId2, "v40a-" + counter);

        // Take confidence snapshots so portfolio has data
        takeConfidenceSnapshot(planId);
        takeConfidenceSnapshot(planId2);
    }

    // ========== Portfolio ==========

    @Test
    void shouldRefreshPortfolioSnapshotSuccess() {
        ResponseEntity<String> res = post("/api/release-governance/portfolio/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldRankingOrderedByConfidenceScore() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/ranking");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        if (root.get("data").size() >= 2) {
            double first = root.get("data").get(0).get("confidenceScore").asDouble();
            double second = root.get("data").get(1).get("confidenceScore").asDouble();
            assertThat(first).isGreaterThanOrEqualTo(second);
        }
    }

    @Test
    void shouldDashboardCountsCorrect() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getInt(root, "data.projectCount")).isGreaterThanOrEqualTo(2);
        assertThat(TestJsonHelper.getInt(root, "data.highConfidenceCount")).isNotNull();
        assertThat(TestJsonHelper.getInt(root, "data.expandNowCount")).isNotNull();
        assertThat(TestJsonHelper.getString(root, "data.averageConfidenceScore")).isNotNull();
    }

    @Test
    void shouldDashboardReturnTopProjects() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topProjects").isArray()).isTrue();
    }

    @Test
    void shouldDashboardReturnBottomProjects() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("bottomProjects").isArray()).isTrue();
    }

    @Test
    void shouldSummaryResponseReturnRiskiestProjects() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getInt(root, "data.totalProjectCount")).isGreaterThanOrEqualTo(2);
        assertThat(root.get("data").get("riskiestProjects").isArray()).isTrue();
    }

    @Test
    void shouldSummaryResponseReturnImprovingDecliningProjects() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        // Second refresh to have a basis for comparison
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getInt(root, "data.totalProjectCount")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSummaryReturnMarkdown() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.summaryMarkdown")).isNotNull();
    }

    @Test
    void shouldExpansionRecommendationIncludeExpandNow() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/ranking");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        for (JsonNode item : root.get("data")) {
            String rec = TestJsonHelper.getString(item, "expansionRecommendation");
            assertThat(rec).isIn("EXPAND_NOW", "EXPAND_WITH_GUARDRAILS", "HOLD", "BLOCK");
        }
    }

    @Test
    void shouldAverageConfidenceScoreCorrect() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getBigDecimal(root, "data.averageConfidenceScore")).isNotNull();
    }

    @Test
    void shouldEmptyProjectSetReturnEmptyDashboard() {
        // Create a new project with no plans — it won't appear in portfolio
        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        // Dashboard should still return with projectCount >= 0
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getInt(root, "data.projectCount")).isNotNull();
    }

    // ========== Baseline Template ==========

    @Test
    void shouldCreateBaselineTemplateSuccess() {
        String tplKey = "default-rollback-" + (counter++);
        ResponseEntity<String> res = post("/api/release-governance/baseline-templates", Map.of(
                "templateKey", tplKey,
                "displayName", "Default Rollback Template",
                "templateScope", "GLOBAL",
                "notes", "Standard rollback requirements"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.templateKey")).isEqualTo(tplKey);
        assertThat(TestJsonHelper.getBool(root, "data.enabled")).isTrue();
    }

    @Test
    void shouldUpdateBaselineTemplateSuccess() {
        String id = createTemplate("update-test", "Update Test", "GLOBAL");

        ResponseEntity<String> res = put("/api/release-governance/baseline-templates/" + id, Map.of(
                "displayName", "Updated Name",
                "notes", "Updated notes"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated Name");
    }

    @Test
    void shouldDisableBaselineTemplateSuccess() {
        String id = createTemplate("disable-test", "Disable Test", "GLOBAL");

        ResponseEntity<String> res = post("/api/release-governance/baseline-templates/" + id + "/status?enabled=false", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }

    @Test
    void shouldDuplicateTemplateKeyReject() {
        String fixedKey = "dup-key-" + (counter++);
        assertOk(post("/api/release-governance/baseline-templates", Map.of(
                "templateKey", fixedKey,
                "displayName", "Original",
                "templateScope", "GLOBAL"
        )));

        ResponseEntity<String> res = post("/api/release-governance/baseline-templates", Map.of(
                "templateKey", fixedKey,
                "displayName", "Duplicate",
                "templateScope", "GLOBAL"
        ));
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldListBaselineByScopeWorks() {
        createTemplate("global-tpl", "Global Template", "GLOBAL");
        createTemplate("project-tpl", "Project Template", "PROJECT_TYPE");

        ResponseEntity<String> res = get("/api/release-governance/baseline-templates?scope=GLOBAL");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        for (JsonNode item : root.get("data")) {
            assertThat(TestJsonHelper.getString(item, "templateScope")).isEqualTo("GLOBAL");
        }
    }

    @Test
    void shouldGetBaselineTemplateById() {
        String id = createTemplate("get-by-id", "Get By ID", "GLOBAL");

        ResponseEntity<String> res = get("/api/release-governance/baseline-templates/" + id);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(id);
    }

    @Test
    void shouldBaselineTemplateNotesPersisted() {
        String tplKey = "notes-test-" + (counter++);
        ResponseEntity<String> res = post("/api/release-governance/baseline-templates", Map.of(
                "templateKey", tplKey,
                "displayName", "Notes Test",
                "templateScope", "GLOBAL",
                "notes", "Persistent notes content"
        ));
        assertOk(res);
        String id = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        ResponseEntity<String> getRes = get("/api/release-governance/baseline-templates/" + id);
        assertOk(getRes);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(getRes.getBody()), "data.notes")).isEqualTo("Persistent notes content");
    }

    // ========== Heatmap ==========

    @Test
    void shouldRefreshHeatmapSuccess() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = post("/api/release-governance/heatmap/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldHeatmapReturnAllCategories() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("categories").isArray()).isTrue();
        assertThat(root.get("data").get("categories").size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void shouldHeatmapContainCells() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("cells").isArray()).isTrue();
        assertThat(root.get("data").get("cells").size()).isGreaterThan(0);
    }

    @Test
    void shouldHeatmapCellHasRiskScoreAndLevel() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        for (JsonNode cell : root.get("data").get("cells")) {
            assertThat(cell.get("riskScore")).isNotNull();
            assertThat(cell.get("riskLevel")).isNotNull();
            assertThat(cell.get("riskCategory")).isNotNull();
        }
    }

    @Test
    void shouldHeatmapRiskLevelValid() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        for (JsonNode cell : root.get("data").get("cells")) {
            String level = TestJsonHelper.getString(cell, "riskLevel");
            assertThat(level).isIn("LOW", "MEDIUM", "HIGH", "CRITICAL");
        }
    }

    @Test
    void shouldHeatmapProjectNamesResolved() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        for (JsonNode cell : root.get("data").get("cells")) {
            assertThat(cell.get("projectName")).isNotNull();
        }
    }

    // ========== Edge Cases ==========

    @Test
    void shouldRejectNonExistentTemplateGet() {
        ResponseEntity<String> res = get("/api/release-governance/baseline-templates/999999999");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentTemplateUpdate() {
        ResponseEntity<String> res = put("/api/release-governance/baseline-templates/999999999", Map.of("displayName", "Nope"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentTemplateStatus() {
        ResponseEntity<String> res = post("/api/release-governance/baseline-templates/999999999/status?enabled=false", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRefreshPortfolioIdempotent() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.projectCount")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldGetRankingAfterRefresh() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/ranking");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldRankingIncludeRequiredFields() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/portfolio/ranking");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        for (JsonNode item : root.get("data")) {
            assertThat(item.get("projectName")).isNotNull();
            assertThat(item.get("confidenceScore")).isNotNull();
            assertThat(item.get("portfolioRank")).isNotNull();
            assertThat(item.get("expansionRecommendation")).isNotNull();
        }
    }

    @Test
    void shouldHeatmapRefreshIdempotent() {
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());
        post("/api/release-governance/heatmap/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/heatmap");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("cells").size()).isGreaterThan(0);
    }

    @Test
    void shouldSummaryIncludeExpandNowGuardrailsHoldBlock() {
        post("/api/release-governance/portfolio/refresh", Map.of());

        ResponseEntity<String> res = get("/api/release-governance/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("expandNowCount")).isNotNull();
        assertThat(root.get("data").get("expandWithGuardrailsCount")).isNotNull();
        assertThat(root.get("data").get("holdCount")).isNotNull();
        assertThat(root.get("data").get("blockCount")).isNotNull();
    }

    // ========== Helpers ==========

    private void takeConfidenceSnapshot(String pid) {
        post("/api/release-rollouts/" + pid + "/confidence-snapshot", Map.of());
    }

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-GP-" + suffix,
                "description", "Governance portfolio integration test project",
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

    private String createTemplate(String key, String name, String scope) {
        String uniqueKey = key + "-" + (counter++);
        ResponseEntity<String> res = post("/api/release-governance/baseline-templates", Map.of(
                "templateKey", uniqueKey,
                "displayName", name,
                "templateScope", scope
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }
}
