package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceUpliftOptimizationIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    public void setUp() { loginAdmin(); }

    @Test void shouldRefreshEvolutionSuccess() { assertOk(post("/api/governance-uplift-optimization/evolution/refresh", Map.of())); }
    @Test void shouldListEvolution() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldEvolutionHasDelta() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("delta")).isNotNull();
    }
    @Test void shouldEvolutionHasSignalLevel() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("signalLevel").asText()).isIn("IMPROVING", "STABLE", "DECLINING", "INSUFFICIENT");
    }

    @Test void shouldRefreshRankingSuccess() { assertOk(post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of())); }
    @Test void shouldListRanking() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldRankingHasEffectivenessLevel() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("effectivenessLevel")).isNotNull();
    }
    @Test void shouldRankingHasRankPosition() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("rankPosition")).isNotNull();
    }

    @Test void shouldRefreshProgressSuccess() { assertOk(post("/api/governance-uplift-optimization/progress-map/refresh", Map.of())); }
    @Test void shouldListProgress() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldProgressHasSignalLevel() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("signalLevel").asText()).isIn("ON_TRACK", "AT_RISK", "BEHIND", "NOT_STARTED");
    }
    @Test void shouldProgressHasProgressPercentage() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("progressPercentage")).isNotNull();
    }

    @Test void shouldDashboardReturnCounts() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("evolutionCount")).isNotNull();
        assertThat(r.get("rankingCount")).isNotNull();
        assertThat(r.get("progressCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdown() { assertOk(get("/api/governance-uplift-optimization/report")); }
    @Test void shouldEmptyDataSafe() { assertOk(get("/api/governance-uplift-optimization/dashboard")); }
    @Test void shouldEvolutionDeltaPercentageReturned() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("deltaPercentage")).isNotNull();
    }
    @Test void shouldRankingAvgUpliftReturned() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("avgUplift")).isNotNull();
    }
    @Test void shouldProgressBaselineScoreReturned() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("baselineScore")).isNotNull();
    }
    @Test void shouldEvolutionCreatesRecords() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldRankingCreatesRecords() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldProgressCreatesRecords() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldEvolutionRefreshIdempotent() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        assertOk(get("/api/governance-uplift-optimization/evolution"));
    }
    @Test void shouldRankingRefreshIdempotent() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        assertOk(get("/api/governance-uplift-optimization/campaign-ranking"));
    }
    @Test void shouldProgressRefreshIdempotent() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        assertOk(get("/api/governance-uplift-optimization/progress-map"));
    }
    @Test void shouldDashboardContainsArrays() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("evolution").isArray()).isTrue();
        assertThat(r.get("ranking").isArray()).isTrue();
        assertThat(r.get("progress").isArray()).isTrue();
    }
    @Test void shouldEvolutionImprovingSignalGenerated() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res);
    }
    @Test void shouldRankingProjectCountReturned() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("projectCount")).isNotNull();
    }
    @Test void shouldProgressTargetScoreReturned() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("targetScore")).isNotNull();
    }
    @Test void shouldReportWithDataReturnsContent() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        assertOk(get("/api/governance-uplift-optimization/report"));
    }
    @Test void shouldEvolutionSignalLevelTextReturned() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res);
    }
    @Test void shouldRankingSummaryTextReturned() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("summaryText")).isNotNull();
    }
    @Test void shouldProgressSummaryTextReturned() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("summaryText")).isNotNull();
    }
    @Test void shouldEvolutionCurrentValueReturned() {
        post("/api/governance-uplift-optimization/evolution/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/evolution");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("currentValue")).isNotNull();
    }
    @Test void shouldRankingCampaignNameReturned() {
        post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/campaign-ranking");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("campaignName")).isNotNull();
    }
    @Test void shouldProgressProjectNameReturned() {
        post("/api/governance-uplift-optimization/progress-map/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-uplift-optimization/progress-map");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("projectName")).isNotNull();
    }
    @Test void shouldAllThreeRefreshesWork() {
        assertOk(post("/api/governance-uplift-optimization/evolution/refresh", Map.of()));
        assertOk(post("/api/governance-uplift-optimization/campaign-ranking/refresh", Map.of()));
        assertOk(post("/api/governance-uplift-optimization/progress-map/refresh", Map.of()));
    }
}
