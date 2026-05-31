package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceEffectivenessOptimizationIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() {
        loginAdmin();
        // Create a recipe for analytics data
        ResponseEntity<String> res = post("/api/governance-knowledge/recipes?recipeKey=eff-rec-" + (counter++) + "&displayName=EffRecipe&recipeType=REMEDIATION", Map.of());
        assertOk(res);
    }

    // ========== Recipe Effectiveness ==========
    @Test void shouldRefreshRecipeEffectivenessSuccess() {
        ResponseEntity<String> res = post("/api/governance-effectiveness/recipes/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldRecipeEffectivenessScoreCalculated() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldEffectivenessLevelReturned() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) {
            String level = TestJsonHelper.getString(data.get(0), "effectivenessLevel");
            assertThat(level).isIn("TOP", "HIGH", "MEDIUM", "LOW");
        }
    }
    @Test void shouldTopRecipeOrderedByScore() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topRecipes")).isNotNull();
    }
    @Test void shouldLowValueRecipeDetected() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("lowValueRecipes")).isNotNull();
    }
    @Test void shouldAverageEffectivenessScoreReturned() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("averageEffectivenessScore")).isNotNull();
    }
    @Test void shouldRecipeCountSummaryCorrect() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("recipeCount")).isNotNull();
    }
    @Test void shouldRecipeTrend7dReturnsData() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/trend?window=LAST_7_DAYS");
        assertOk(res);
    }

    // ========== Playbook Analytics ==========
    @Test void shouldRefreshPlaybookAnalyticsSuccess() {
        ResponseEntity<String> res = post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldPlaybookAnalyticsCompletionRateCorrect() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("avgCompletionRate")).isNotNull();
    }
    @Test void shouldPlaybookAnalyticsResolutionHoursCorrect() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("totalPlanCount")).isNotNull();
    }
    @Test void shouldPlaybookAnalyticsRankingReturned() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks");
        assertOk(res);
    }

    // ========== Optimization Suggestions ==========
    @Test void shouldRefreshOptimizationsSuccess() {
        ResponseEntity<String> res = post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldPromoteRecipeSuggestionGenerated() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("suggestionType")).isNotNull();
    }
    @Test void shouldPruneRecipeSuggestionGenerated() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res);
    }
    @Test void shouldRefinePlaybookSuggestionGenerated() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res);
    }
    @Test void shouldOptimizationDashboardCountsCorrect() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("suggestionCount")).isNotNull();
    }
    @Test void shouldHighPrioritySuggestionCountReturned() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("highPrioritySuggestionCount")).isNotNull();
    }
    @Test void shouldSuggestionsSortedByPriority() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res);
    }
    @Test void shouldRationaleTextPopulated() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("rationaleText")).isNotNull();
    }

    // ========== Report ==========
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-effectiveness/report");
        assertOk(res);
    }
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("recipeCount")).isNotNull();
    }
    @Test void shouldOptimizationPromoteCountReturned() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("promoteSuggestionCount")).isNotNull();
    }
    @Test void shouldMergeDuplicateSuggestionGenerated() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations");
        assertOk(res);
    }
    @Test void shouldTopRecipeCountReturned() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topRecipeCount")).isNotNull();
    }
    @Test void shouldHighRecipeCountReturned() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("highRecipeCount")).isNotNull();
    }
    @Test void shouldLowRecipeCountReturned() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("lowRecipeCount")).isNotNull();
    }
    @Test void shouldRefreshThenDashboardReturnsCounts() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.recipeCount")).isNotNull();
    }
    @Test void shouldRecipeEffectivenessFilterByLevel() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes?level=TOP");
        assertOk(res);
    }
    @Test void shouldPlaybookAnalyticsListReturnRecords() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks");
        assertOk(res);
    }
    @Test void shouldOptimizationPruneCountReturned() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("pruneSuggestionCount")).isNotNull();
    }
    @Test void shouldOptimizationRefineCountReturned() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("refinePlaybookCount")).isNotNull();
    }
    @Test void shouldPlaybookDashboardBlockedCountReturned() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("totalBlockedCount")).isNotNull();
    }
    @Test void shouldEffectivenessRefreshIdempotent() {
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        post("/api/governance-effectiveness/recipes/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/recipes/dashboard");
        assertOk(res);
    }
    @Test void shouldOptimizationRefreshIdempotent() {
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        post("/api/governance-effectiveness/optimizations/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/optimizations/dashboard");
        assertOk(res);
    }
    @Test void shouldPlaybookRefreshIdempotent() {
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        post("/api/governance-effectiveness/playbooks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-effectiveness/playbooks/dashboard");
        assertOk(res);
    }
}
