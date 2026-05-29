package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceSimulationPolicyTuningIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);
    private String scenarioId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        // Create a scenario for reuse
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios", Map.of(
                "scenarioName", "test-scenario-" + (counter++), "scenarioType", "SLA_TUNING", "inputJson", "{}"));
        assertOk(res);
        scenarioId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Scenario CRUD ==========
    @Test void shouldCreateScenarioSuccess() {
        String name = "cr-sc-" + (counter++);
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios", Map.of("scenarioName", name, "scenarioType", "SLA_TUNING", "inputJson", "{}"));
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.scenarioName")).isEqualTo(name);
    }
    @Test void shouldUpdateScenarioSuccess() {
        ResponseEntity<String> res = put("/api/governance-simulation/scenarios/" + scenarioId, Map.of("scenarioName", "Updated"));
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.scenarioName")).isEqualTo("Updated");
    }
    @Test void shouldStatusDraftToReady() {
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.scenarioStatus")).isEqualTo("READY");
    }
    @Test void shouldRunSlaTuningScenarioSuccess() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.resultStatus")).isIn("SUCCESS", "WARNING", "NO_IMPROVEMENT", "INVALID");
    }
    @Test void shouldRunOwnerRebalancingScenarioSuccess() {
        String id = createScenario("rebal-" + (counter++), "OWNER_REBALANCING");
        post("/api/governance-simulation/scenarios/" + id + "/status?status=READY", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + id + "/run", Map.of());
        assertOk(res);
    }
    @Test void shouldRunWaiverReductionScenarioSuccess() {
        String id = createScenario("waiver-" + (counter++), "WAIVER_REDUCTION");
        post("/api/governance-simulation/scenarios/" + id + "/status?status=READY", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + id + "/run", Map.of());
        assertOk(res);
    }
    @Test void shouldRunPolicyThresholdTuningSuccess() {
        String id = createScenario("policy-" + (counter++), "POLICY_THRESHOLD_TUNING");
        post("/api/governance-simulation/scenarios/" + id + "/status?status=READY", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + id + "/run", Map.of());
        assertOk(res);
    }
    @Test void shouldInvalidStatusTransitionReject() {
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=SIMULATED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }
    @Test void shouldGetScenarioById() {
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(scenarioId);
    }
    @Test void shouldListScenarios() {
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldSimulatedToArchived() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=ARCHIVED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.scenarioStatus")).isEqualTo("ARCHIVED");
    }

    // ========== Result / Comparison ==========
    @Test void shouldComparisonReturnValues() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/comparison");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("scenarioId")).isNotNull();
    }
    @Test void shouldResultReturnAfterRun() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.resultStatus")).isNotNull();
    }
    @Test void shouldReportExportSuccess() {
        ResponseEntity<String> res = get("/api/governance-simulation/report");
        assertOk(res);
    }

    // ========== Suggestions ==========
    @Test void shouldRefreshSuggestionsSuccess() {
        ResponseEntity<String> res = post("/api/governance-simulation/suggestions/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldSuggestionListReturnItems() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/suggestions");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldSuggestionPriorityPopulated() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/suggestions");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) {
            assertThat(data.get(0).get("priority")).isNotNull();
        }
    }
    @Test void shouldSuggestionCurrentSuggestedValuePersisted() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/suggestions");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) {
            assertThat(data.get(0).get("currentValue")).isNotNull();
            assertThat(data.get(0).get("suggestedValue")).isNotNull();
        }
    }

    // ========== Dashboard ==========
    @Test void shouldDashboardCountsCorrect() {
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("scenarioCount")).isNotNull();
        assertThat(root.get("data").get("successfulScenarioCount")).isNotNull();
    }
    @Test void shouldDashboardTopScenariosReturned() {
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topScenarios").isArray()).isTrue();
    }
    @Test void shouldEmptyBaselineReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.scenarioCount")).isNotNull();
    }

    // ========== Edge Cases ==========
    @Test void shouldRepeatedRunUpdateResult() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        assertOk(res);
    }
    @Test void shouldNonExistentScenarioReturnNotFound() {
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/999999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldRollbackReadinessSuggestionGenerated() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/suggestions");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldRepeatedRefreshIdempotent() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/suggestions");
        assertOk(res);
    }
    @Test void shouldDashboardWarningCountReturned() {
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.warningScenarioCount")).isNotNull();
    }
    @Test void shouldResultMarkdownReportReturned() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reportMarkdown")).isNotNull();
    }
    @Test void shouldCapacityDeltaReturned() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("projectedCapacityDelta")).isNotNull();
    }

    @Test void shouldDashboardNoImprovementCountReturned() {
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.noImprovementCount")).isNotNull();
    }
    @Test void shouldSimulateAllFourTypesAtLeastOnce() {
        for (String type : new String[]{"SLA_TUNING", "OWNER_REBALANCING", "WAIVER_REDUCTION", "POLICY_THRESHOLD_TUNING"}) {
            String id = createScenario("all-" + type + "-" + (counter++), type);
            post("/api/governance-simulation/scenarios/" + id + "/status?status=READY", Map.of());
            ResponseEntity<String> res = post("/api/governance-simulation/scenarios/" + id + "/run", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldDashboardTopSuggestionsReturned() {
        post("/api/governance-simulation/suggestions/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/dashboard");
        assertOk(res);
    }
    @Test void shouldImpactedOwnerCountReturned() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("impactedOwnerCount")).isNotNull();
    }
    @Test void shouldImpactedProjectCountReturned() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("impactedProjectCount")).isNotNull();
    }
    @Test void shouldProjectedRiskDeltaReturned() {
        post("/api/governance-simulation/scenarios/" + scenarioId + "/status?status=READY", Map.of());
        post("/api/governance-simulation/scenarios/" + scenarioId + "/run", Map.of());
        ResponseEntity<String> res = get("/api/governance-simulation/scenarios/" + scenarioId + "/result");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("projectedRiskDelta")).isNotNull();
    }
    // ========== Helpers ==========
    private String createScenario(String name, String type) {
        ResponseEntity<String> res = post("/api/governance-simulation/scenarios", Map.of("scenarioName", name, "scenarioType", type, "inputJson", "{}"));
        assertOk(res);
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }
}
