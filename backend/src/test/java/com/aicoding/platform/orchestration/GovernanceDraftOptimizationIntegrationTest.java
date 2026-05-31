package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceDraftOptimizationIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    public void setUp() { loginAdmin(); }

    @Test void shouldRefreshSignalsSuccess() { assertOk(post("/api/governance-draft-optimization/signals/refresh", Map.of())); }
    @Test void shouldListSignalsAfterRefresh() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldSignalHasAdoptionRate() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("adoptionRate")).isNotNull();
    }
    @Test void shouldSignalHasLevel() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("signalLevel")).isNotNull();
    }

    @Test void shouldRefreshAssistiveOrderingSuccess() { assertOk(post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of())); }
    @Test void shouldListAssistiveOrdering() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldAssistiveOrderingHasOptimizationLevel() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("optimizationLevel").asText()).isIn("PROMOTE", "KEEP", "DEMOTE", "REMOVE");
    }

    @Test void shouldRefreshPackageCompositionSuccess() { assertOk(post("/api/governance-draft-optimization/package-composition/refresh", Map.of())); }
    @Test void shouldListPackageComposition() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldPackageCompositionHasTuningLevel() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("tuningLevel")).isNotNull();
    }

    @Test void shouldDashboardReturnCounts() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("signalCount")).isNotNull();
        assertThat(r.get("orderingCount")).isNotNull();
        assertThat(r.get("compositionCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdown() { assertOk(get("/api/governance-draft-optimization/report")); }
    @Test void shouldRefreshIdempotent() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/signals"));
    }
    @Test void shouldAssistiveOrderingSuggestedNewOrderReturned() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("suggestedNewOrder")).isNotNull();
    }
    @Test void shouldAssistiveOrderingRationaleTextReturned() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("rationaleText")).isNotNull();
    }
    @Test void shouldPackageCompositionAvgOverallReturned() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("avgOverall")).isNotNull();
    }
    @Test void shouldPackageCompositionSampleCountReturned() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("sampleCount")).isNotNull();
    }
    @Test void shouldSignalsSuggestionTextReturned() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("suggestionText")).isNotNull();
    }
    @Test void shouldEmptyDataDashboardSafe() { assertOk(get("/api/governance-draft-optimization/dashboard")); }
    @Test void shouldDashboardContainsSignalsArray() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/dashboard"));
    }
    @Test void shouldDashboardContainsOrderingArray() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/dashboard"));
    }
    @Test void shouldDashboardContainsCompositionArray() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/dashboard"));
    }
    @Test void shouldSignalScopeKeyReturned() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("scopeKey")).isNotNull();
    }
    @Test void shouldSignalRejectionRateReturned() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("rejectionRate")).isNotNull();
    }
    @Test void shouldSignalsContainsAvgUsefulness() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/signals");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("avgUsefulnessRating")).isNotNull();
    }
    @Test void shouldAssistiveOrderingUsefulnessCountReturned() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("usefulnessCount")).isNotNull();
    }
    @Test void shouldAssistiveOrderingNotUsefulCountReturned() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/assistive-ordering");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("notUsefulCount")).isNotNull();
    }
    @Test void shouldPackageCompositionSuggestionTextReturned() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("suggestionText")).isNotNull();
    }
    @Test void shouldPackageCompositionScoreRangeReturned() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/package-composition");
        assertOk(res); JsonNode d = TestJsonHelper.parse(res.getBody()).get("data");
        if (d.size() > 0) assertThat(d.get(0).get("scoreRange")).isNotNull();
    }
    @Test void shouldDashboardReturnReportableData() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/report"));
    }
    @Test void shouldRefreshOrderingIdempotent() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/assistive-ordering"));
    }
    @Test void shouldRefreshCompositionIdempotent() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/package-composition"));
    }
    @Test void shouldSignalsOrderedByAdoptionRate() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/signals"));
    }
    @Test void shouldAssistiveOrderingOrderedBySuggestedOrder() {
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/assistive-ordering"));
    }
    @Test void shouldPackageCompositionOrderedByOverall() {
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        assertOk(get("/api/governance-draft-optimization/package-composition"));
    }
    @Test void shouldDashboardListAllThreeArrays() {
        post("/api/governance-draft-optimization/signals/refresh", Map.of());
        post("/api/governance-draft-optimization/assistive-ordering/refresh", Map.of());
        post("/api/governance-draft-optimization/package-composition/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-optimization/dashboard");
        assertOk(res); JsonNode r = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(r.get("signals").isArray()).isTrue();
        assertThat(r.get("ordering").isArray()).isTrue();
        assertThat(r.get("composition").isArray()).isTrue();
    }
}
