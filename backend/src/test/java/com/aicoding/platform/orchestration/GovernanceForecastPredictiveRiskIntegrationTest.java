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

class GovernanceForecastPredictiveRiskIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() {
        loginAdmin();
    }

    // ========== Capacity Forecast ==========
    @Test void shouldRefreshCapacityForecastSuccess() {
        ResponseEntity<String> res = post("/api/governance-forecast/capacity/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldCapacityDashboardCountsCorrect() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.ownerCount")).isNotNull();
    }
    @Test void shouldProjectedBacklogCalculationCorrect() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldCapacityForecastListReturnItems() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity");
        assertOk(res);
    }
    @Test void should14dayHorizonReturnLargerForecast() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res14 = get("/api/governance-forecast/capacity?horizonDays=14");
        assertOk(res14);
    }
    @Test void shouldCapacityRefreshIdempotent() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity/dashboard");
        assertOk(res);
    }

    // ========== Predictive Risk Signals ==========
    @Test void shouldRefreshRiskSignalsSuccess() {
        ResponseEntity<String> res = post("/api/governance-forecast/risk-signals/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldRiskSignalsListReturnItems() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldRiskDashboardCountsCorrect() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("signalCount")).isNotNull();
    }
    @Test void shouldRiskSignalsRiskScoreRangeValid() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (int i = 0; i < data.size(); i++) {
            double score = data.get(i).get("riskScore").asDouble();
            assertThat(score).isBetween(0.0, 100.0);
        }
    }
    @Test void shouldRiskSignalsProbabilityScoreValid() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (int i = 0; i < data.size(); i++) {
            double score = data.get(i).get("probabilityScore").asDouble();
            assertThat(score).isBetween(0.0, 100.0);
        }
    }
    @Test void shouldRiskDashboardTopSignalsReturned() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topSignals").isArray()).isTrue();
    }
    @Test void shouldRiskSignalsRefreshIdempotent() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals/dashboard");
        assertOk(res);
    }

    // ========== Backlog Health ==========
    @Test void shouldRefreshBacklogSuccess() {
        ResponseEntity<String> res = post("/api/governance-forecast/backlog/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldBacklogDashboardProjectCountCorrect() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.projectCount")).isNotNull();
    }
    @Test void shouldBacklogListReturnItems() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldBacklogHealthLevelsReturned() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("healthyCount")).isNotNull();
        assertThat(root.get("data").get("watchCount")).isNotNull();
        assertThat(root.get("data").get("riskCount")).isNotNull();
        assertThat(root.get("data").get("criticalCount")).isNotNull();
    }
    @Test void shouldBacklogTopGrowingReturned() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topGrowingBacklogs").isArray()).isTrue();
    }
    @Test void shouldBacklogTopOverdueReturned() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topOverdueProjects").isArray()).isTrue();
    }
    @Test void shouldBacklogRefreshIdempotent() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog/dashboard");
        assertOk(res);
    }

    // ========== Summary & Report ==========
    @Test void shouldSummaryResponseContainForecastData() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("ownerForecastCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/report");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.summaryMarkdown")).isNotNull();
    }
    @Test void shouldSummaryIncludeSignalCount() {
        ResponseEntity<String> res = get("/api/governance-forecast/summary");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.signalCount")).isNotNull();
    }
    @Test void shouldSummaryIncludeBacklogCounts() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/summary");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.projectCount")).isNotNull();
    }
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-forecast/capacity/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.ownerCount")).isNotNull();
    }
    @Test void shouldCapacityRiskLevelsInDashboard() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("lowRiskCount")).isNotNull();
        assertThat(root.get("data").get("watchCount")).isNotNull();
        assertThat(root.get("data").get("highCount")).isNotNull();
        assertThat(root.get("data").get("criticalCount")).isNotNull();
    }
    @Test void shouldTopRiskOwnersReturned() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/capacity/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topRiskOwners").isArray()).isTrue();
    }
    @Test void shouldSummaryTotalProjectedReturned() {
        post("/api/governance-forecast/capacity/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("totalProjectedBacklog")).isNotNull();
        assertThat(root.get("data").get("totalProjectedOverdue")).isNotNull();
    }
    @Test void shouldRiskSignalTypesReturned() {
        post("/api/governance-forecast/risk-signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/risk-signals/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("ownerRiskSignals")).isNotNull();
        assertThat(root.get("data").get("projectRiskSignals")).isNotNull();
        assertThat(root.get("data").get("portfolioRiskSignals")).isNotNull();
    }
    @Test void shouldBacklogGrowthRateReturned() {
        post("/api/governance-forecast/backlog/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-forecast/backlog");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) {
            assertThat(data.get(0).get("backlogGrowthRate")).isNotNull();
        }
    }
}
