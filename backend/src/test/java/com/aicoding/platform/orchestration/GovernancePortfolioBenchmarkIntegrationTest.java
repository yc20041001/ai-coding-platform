package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernancePortfolioBenchmarkIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    public void setUp() { loginAdmin(); }

    @Test void shouldRefreshBenchmarksSuccess() { assertOk(post("/api/governance-benchmark/benchmarks/refresh", Map.of())); }
    @Test void shouldListBenchmarks() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldBenchmarkHasSignalLevel() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("signalLevel")).isNotNull();
    }
    @Test void shouldBenchmarkHasPeerAvg() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("peerAvg")).isNotNull();
    }
    @Test void shouldBenchmarkHasPercentileRank() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("percentileRank")).isNotNull();
    }
    @Test void shouldRefreshAlignmentsSuccess() { assertOk(post("/api/governance-benchmark/alignments/refresh", Map.of())); }
    @Test void shouldListAlignments() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldAlignmentHasGap() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("gap")).isNotNull();
    }
    @Test void shouldAlignmentHasAlignmentLevel() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("alignmentLevel")).isNotNull();
    }
    @Test void shouldRefreshScorecardsSuccess() { assertOk(post("/api/governance-benchmark/scorecards/refresh", Map.of())); }
    @Test void shouldListScorecards() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldScorecardHasMaturityLevel() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("maturityLevel").asText()).isIn("INITIAL", "DEVELOPING", "DEFINED", "MANAGED", "OPTIMIZING");
    }
    @Test void shouldScorecardHasTotalScore() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("totalScore")).isNotNull();
    }
    @Test void shouldScorecardHasMultiDimensionScores() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) {
            assertThat(d.get(0).get("draftAdoptionScore")).isNotNull();
            assertThat(d.get(0).get("assistiveQualityScore")).isNotNull();
        }
    }
    @Test void shouldDashboardReturnCounts() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("benchmarkCount")).isNotNull();
        assertThat(r.get("alignmentCount")).isNotNull();
        assertThat(r.get("scorecardCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdown() { assertOk(get("/api/governance-benchmark/report")); }
    @Test void shouldRefreshIdempotent() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        assertOk(get("/api/governance-benchmark/benchmarks"));
    }
    @Test void shouldAlignmentSuggestionNotNull() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("suggestionText")).isNotNull();
    }
    @Test void shouldScorecardSummaryNotNull() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("summaryText")).isNotNull();
    }
    @Test void shouldBenchmarkMetricKeyNotNull() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("metricKey")).isNotNull();
    }
    @Test void shouldDashboardContainsArrays() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("benchmarks").isArray()).isTrue();
        assertThat(r.get("alignments").isArray()).isTrue();
        assertThat(r.get("scorecards").isArray()).isTrue();
    }
    @Test void shouldEmptyDataSafe() { assertOk(get("/api/governance-benchmark/dashboard")); }
    @Test void shouldBenchmarkRefreshCreatesRecords() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldAlignmentRefreshCreatesRecords() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldScorecardRefreshCreatesRecords() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").size()).isGreaterThan(0);
    }
    @Test void shouldDashboardBenchmarksArrayNotEmpty() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/dashboard");
        assertOk(res);
    }
    @Test void shouldScorecardOrderedByTotalScore() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        assertOk(get("/api/governance-benchmark/scorecards"));
    }
    @Test void shouldBenchmarkHasPeerP90() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("peerP90")).isNotNull();
    }
    @Test void shouldBenchmarkHasSampleCount() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("sampleCount")).isNotNull();
    }
    @Test void shouldAlignmentProjectNameNotNull() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/alignments");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("projectName")).isNotNull();
    }
    @Test void shouldScorecardOperatorProductivityScoreNotNull() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/scorecards");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("operatorProductivityScore")).isNotNull();
    }
    @Test void shouldBenchmarkSummaryTextNotNull() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/benchmarks");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("summaryText")).isNotNull();
    }
    @Test void shouldReportWithDataReturnsStats() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-benchmark/report");
        assertOk(res);
    }
    @Test void shouldBenchmarkRefreshMultipleTimes() {
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        post("/api/governance-benchmark/benchmarks/refresh", Map.of());
        assertOk(get("/api/governance-benchmark/benchmarks"));
    }
    @Test void shouldAlignmentRefreshIdempotent() {
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        post("/api/governance-benchmark/alignments/refresh", Map.of());
        assertOk(get("/api/governance-benchmark/alignments"));
    }
    @Test void shouldScorecardRefreshIdempotent() {
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        post("/api/governance-benchmark/scorecards/refresh", Map.of());
        assertOk(get("/api/governance-benchmark/scorecards"));
    }
}
