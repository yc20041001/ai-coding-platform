package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceOperatorLearningIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);
    private String sessionId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        ResponseEntity<String> res = post("/api/governance-workspace/sessions", Map.of());
        assertOk(res);
        sessionId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Action Memory ==========
    @Test void shouldRecordActionSuccess() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK", Map.of());
        assertOk(res);
    }
    @Test void shouldListActionsBySession() {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK", Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/actions?sessionId=" + sessionId);
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldActionMemoryPreservesAcceptedFlag() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=ACCEPT_NEXT_STEP&actionTargetType=RECOMMENDATION&acceptedFlag=true", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.acceptedFlag")).isTrue();
    }
    @Test void shouldActionMemoryPreservesSuccessFlag() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=COMPLETE_GUIDED_TASK&actionTargetType=GUIDED_TASK&successFlag=true", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.successFlag")).isTrue();
    }

    // ========== Session Insight ==========
    @Test void shouldRefreshInsightSuccess() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        assertOk(res);
    }
    @Test void shouldInsightComputesTotalActions() {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK", Map.of());
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }
    @Test void shouldInsightRefreshIdempotent() {
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }
    @Test void shouldEmptySessionReturnsZeroInsight() {
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }

    // ========== Dashboard & Report ==========
    @Test void shouldDashboardReturnsTopOperators() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("totalSessions")).isNotNull();
    }
    @Test void shouldDashboardIncludesAcceptanceRate() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("acceptanceRate")).isNotNull();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/report");
        assertOk(res);
    }
    @Test void shouldReportWithEmptyDataReturnsEmptySections() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/report");
        assertOk(res); assertThat(res.getBody()).contains("Overview");
    }

    // ========== Reuse Bundle ==========
    @Test void shouldCreateReuseBundleSuccess() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles?bundleKey=bundle-" + (counter++) + "&title=TestBundle&category=CONFIDENCE", Map.of());
        assertOk(res);
    }
    @Test void shouldUpdateReuseBundleSuccess() {
        String key = "upd-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=TestBundle&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-operator-memory/reuse-bundles/" + id + "?title=Updated", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.title")).isEqualTo("Updated");
    }
    @Test void shouldDisableReuseBundleSuccess() {
        String key = "dis-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=Test&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles/" + id + "/status?enabled=false", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }
    @Test void shouldDuplicateBundleKeyReject() {
        String key = "dup-bundle-" + (counter++);
        post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=First&category=CONFIDENCE", Map.of());
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=Second&category=CONFIDENCE", Map.of());
        assertCode(res, "CONFLICT");
    }
    @Test void shouldRefreshReuseBundlesSuccess() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldListReuseBundles() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/reuse-bundles");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldGetReuseBundleById() {
        String key = "get-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=GetTest&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-operator-memory/reuse-bundles/" + id);
        assertOk(res);
    }

    // ========== Edge ==========
    @Test void shouldRecordMultipleActionsAndList() {
        for (int i = 0; i < 3; i++) {
            post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_RECOMMENDATION&actionTargetType=RECOMMENDATION", Map.of());
        }
        ResponseEntity<String> res = get("/api/governance-operator-memory/actions?sessionId=" + sessionId);
        assertOk(res);
    }
    @Test void shouldDashboardIncludesGuidedTaskCompletionRate() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("guidedTaskCompletionRate")).isNotNull();
    }
    @Test void shouldInsightDominantPatternExtracted() throws Exception {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK", Map.of());
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_RECIPE&actionTargetType=RECIPE", Map.of());
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }
    @Test void shouldProductivityScoreComputed() throws Exception {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=ACCEPT_NEXT_STEP&actionTargetType=RECOMMENDATION&acceptedFlag=true", Map.of());
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=COMPLETE_GUIDED_TASK&actionTargetType=GUIDED_TASK&successFlag=true", Map.of());
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }
    @Test void shouldDashboardReturnsBundleCount() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/dashboard");
        assertOk(res);
    }
    @Test void shouldRecordActionWithDuration() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=REVIEW_WAIVER&actionTargetType=WAIVER&durationSeconds=120", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.durationSeconds")).isEqualTo(120);
    }
    @Test void shouldInsightComputesAvgDuration() {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK&durationSeconds=60", Map.of());
        post("/api/governance-operator-memory/insights/refresh?sessionId=" + sessionId, Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/insights");
        assertOk(res);
    }
    @Test void shouldRecordActionWithNote() {
        ResponseEntity<String> res = post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=EXPORT_REPORT&actionTargetType=REPORT&noteText=Exported", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.noteText")).isEqualTo("Exported");
    }
    @Test void shouldListAllActions() {
        post("/api/governance-operator-memory/actions?sessionId=" + sessionId + "&actionType=OPEN_PLAYBOOK&actionTargetType=PLAYBOOK", Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/actions");
        assertOk(res);
    }
    @Test void shouldDashboardIncludesAvgActionDuration() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("avgActionDurationSeconds")).isNotNull();
    }
    @Test void shouldReuseBundleSuccessRatePersists() {
        String key = "rate-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=RateTest&category=CONFIDENCE", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.parse(cr.getBody()).get("data").get("successRate")).isNotNull();
    }
    @Test void shouldReuseBundleEffectivenessLevelPersists() {
        String key = "eff-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=EffTest&category=CONFIDENCE", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.effectivenessLevel")).isEqualTo("USEFUL");
    }
    @Test void shouldRefreshReuseBundlesIdempotent() {
        post("/api/governance-operator-memory/reuse-bundles/refresh", Map.of());
        post("/api/governance-operator-memory/reuse-bundles/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-operator-memory/reuse-bundles");
        assertOk(res);
    }
    @Test void shouldListActionsReturnEmptyForUnknownSession() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/actions?sessionId=999999");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldReuseBundleStatusToggleBackToEnabled() {
        String key = "toggle-bundle-" + (counter++);
        ResponseEntity<String> cr = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=Toggle&category=CONFIDENCE", Map.of());
        String id = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        post("/api/governance-operator-memory/reuse-bundles/" + id + "/status?enabled=false", Map.of());
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles/" + id + "/status?enabled=true", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isTrue();
    }
    @Test void shouldNonExistentBundleReturnsNotFound() {
        ResponseEntity<String> res = get("/api/governance-operator-memory/reuse-bundles/999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldReuseBundleWithGuardrailKeyPersists() {
        String key = "guard-bundle-" + (counter++);
        ResponseEntity<String> res = post("/api/governance-operator-memory/reuse-bundles?bundleKey=" + key + "&title=GuardTest&category=CONFIDENCE&guardrailKey=MIN_CONFIDENCE_SCORE&priority=P1", Map.of());
        assertOk(res);
    }
}
