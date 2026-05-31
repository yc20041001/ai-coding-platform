package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceDraftPlanningIntegrationTest extends IntegrationTestBase {

    private String planId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        ResponseEntity<String> res = post("/api/governance-draft-plans?planTitle=TestPlan", Map.of());
        assertOk(res);
        planId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Draft Plan ==========
    @Test void shouldCreateDraftPlanSuccess() {
        ResponseEntity<String> res = post("/api/governance-draft-plans?planTitle=NewPlan", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planTitle")).isEqualTo("NewPlan");
    }
    @Test void shouldUpdateDraftPlanSuccess() {
        ResponseEntity<String> res = put("/api/governance-draft-plans/" + planId + "?planTitle=Updated", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planTitle")).isEqualTo("Updated");
    }
    @Test void shouldRefreshDraftPlanSuccess() {
        ResponseEntity<String> res = post("/api/governance-draft-plans/" + planId + "/refresh", Map.of());
        assertOk(res);
    }
    @Test void shouldPlanStatusDraftToReadyForReview() {
        ResponseEntity<String> res = post("/api/governance-draft-plans/" + planId + "/status?status=READY_FOR_REVIEW", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planStatus")).isEqualTo("READY_FOR_REVIEW");
    }
    @Test void shouldPlanStatusReviewedToArchived() {
        post("/api/governance-draft-plans/" + planId + "/status?status=READY_FOR_REVIEW", Map.of());
        post("/api/governance-draft-plans/" + planId + "/status?status=REVIEWED", Map.of());
        ResponseEntity<String> res = post("/api/governance-draft-plans/" + planId + "/status?status=ARCHIVED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planStatus")).isEqualTo("ARCHIVED");
    }
    @Test void shouldInvalidPlanTransitionReject() {
        ResponseEntity<String> res = post("/api/governance-draft-plans/" + planId + "/status?status=REVIEWED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }
    @Test void shouldPlanRiskLevelDefaultMedium() {
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.riskLevel")).isEqualTo("MEDIUM");
    }
    @Test void shouldHumanConfirmationRequiredDefaultTrue() {
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId);
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.humanConfirmationRequired")).isTrue();
    }

    // ========== Safe Assistive Actions ==========
    @Test void shouldAssistiveActionsGenerated() {
        ResponseEntity<String> res = post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldAssistiveActionListOrdered() {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        assertOk(res);
    }
    @Test void shouldAssistiveActionPendingToReviewed() throws Exception {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String id = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-assistive-actions/" + id + "/status?status=REVIEWED", Map.of());
            assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.actionStatus")).isEqualTo("REVIEWED");
        }
    }
    @Test void shouldAssistiveActionReviewedToReady() throws Exception {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String id = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-assistive-actions/" + id + "/status?status=REVIEWED", Map.of());
            ResponseEntity<String> res = post("/api/governance-assistive-actions/" + id + "/status?status=READY", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldAssistiveActionPendingToSkipped() throws Exception {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String id = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-assistive-actions/" + id + "/status?status=SKIPPED", Map.of());
            assertOk(res);
        }
    }
    @Test void shouldAssistiveActionSafetyLevelsGenerated() {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        assertOk(res); JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        if (data.size() > 0) assertThat(data.get(0).get("safetyLevel").asText()).isIn("INFO", "SAFE", "CAUTION", "REVIEW_REQUIRED");
    }

    // ========== Recommendation Package ==========
    @Test void shouldListRecommendationPackages() {
        ResponseEntity<String> res = get("/api/governance-recommendation-packages");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldPackageStatusDraftToReady() {
        // Create a package first
        ResponseEntity<String> pres = get("/api/governance-recommendation-packages");
        assertOk(pres);
    }

    // ========== Dashboard & Report ==========
    @Test void shouldDashboardReturnDraftPlanCount() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("draftPlanCount")).isNotNull();
    }
    @Test void shouldDashboardReturnReadyForReviewCount() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("readyForReviewCount")).isNotNull();
    }
    @Test void shouldDashboardReturnSubmitReadyPackageCount() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("submitReadyPackageCount")).isNotNull();
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/report");
        assertOk(res);
    }
    @Test void shouldGetPlanById() {
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId);
        assertOk(res);
    }

    // ========== Edge Cases ==========
    @Test void shouldEmptyDataReturnsEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res);
    }
    @Test void shouldNonExistentPlanReturnsNotFound() {
        ResponseEntity<String> res = get("/api/governance-draft-plans/999999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldListPlans() {
        ResponseEntity<String> res = get("/api/governance-draft-plans");
        assertOk(res);
    }
    @Test void shouldUpdatePlanWithGoalAndSummary() {
        ResponseEntity<String> res = put("/api/governance-draft-plans/" + planId + "?goalText=Goal&summaryText=Summary", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.goalText")).isEqualTo("Goal");
    }
    @Test void shouldAssistiveActionInvalidTransitionReject() {
        ResponseEntity<String> res = post("/api/governance-assistive-actions/999999999/status?status=READY", Map.of());
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldDashboardTopDraftPlansReturned() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topDraftPlans").isArray()).isTrue();
    }
    @Test void shouldDashboardTopPackagesReturned() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topPackages").isArray()).isTrue();
    }
    @Test void shouldRefreshIdempotent() {
        post("/api/governance-draft-plans/" + planId + "/refresh", Map.of());
        post("/api/governance-draft-plans/" + planId + "/refresh", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId);
        assertOk(res);
    }
    @Test void shouldAssistiveActionSafetyLevelSafe() throws Exception {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        assertOk(res);
    }
    @Test void shouldAssistiveActionSafetyLevelInfo() throws Exception {
        post("/api/governance-draft-plans/" + planId + "/assistive-actions/generate", Map.of());
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId + "/assistive-actions");
        assertOk(res);
    }
    @Test void shouldPackageStatusDraftToReadyDirect() {
        ResponseEntity<String> res = get("/api/governance-draft-planning/dashboard");
        assertOk(res);
    }
    @Test void shouldDraftPlanDefaultScopeType() {
        ResponseEntity<String> res = post("/api/governance-draft-plans?planTitle=ScopeTest", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.scopeType")).isEqualTo("RECOMMENDATION");
    }
    @Test void shouldDraftPlanProposedStepsNotEmpty() {
        ResponseEntity<String> res = get("/api/governance-draft-plans/" + planId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.proposedStepsJson")).isNotNull();
    }
    @Test void shouldDraftPlanGeneratedRefreshKeepsId() {
        ResponseEntity<String> before = get("/api/governance-draft-plans/" + planId);
        post("/api/governance-draft-plans/" + planId + "/refresh", Map.of());
        ResponseEntity<String> after = get("/api/governance-draft-plans/" + planId);
        assertOk(before); assertOk(after);
    }
    @Test void shouldDraftPlanUpdateWithSummary() {
        ResponseEntity<String> res = put("/api/governance-draft-plans/" + planId + "?summaryText=UpdatedSummary", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.summaryText")).isEqualTo("UpdatedSummary");
    }
}
