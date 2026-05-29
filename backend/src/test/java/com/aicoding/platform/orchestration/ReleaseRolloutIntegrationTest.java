package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseRolloutIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-RO-" + suffix,
                "description", "Rollout integration test project",
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

    // ========================
    // Rollout Plan - Create
    // ========================

    @Test
    void shouldCreateRolloutPlan() {
        String pid = createProject("RoCreate");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans", Map.of(
                "releaseLabel", "v1.0.0"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.0.0");
        assertThat(TestJsonHelper.getString(data, "rolloutStatus")).isEqualTo("DRAFT");
        assertThat(TestJsonHelper.getString(data, "rolloutStrategy")).isEqualTo("MANUAL_FULL");
        assertThat(TestJsonHelper.getString(data, "targetEnvironment")).isEqualTo("production");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
    }

    @Test
    void shouldCreateRolloutPlanWithAllFields() {
        String pid = createProject("RoCreateAll");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans", Map.of(
                "releaseLabel", "v2.0.0-rc1",
                "rolloutStrategy", "AUTO_CANARY",
                "targetEnvironment", "staging",
                "observationWindowMinutes", 120,
                "rollbackTriggerSummary", "Error rate > 5%",
                "successCriteriaSummary", "All smoke tests pass",
                "readinessSummary", "Ready for rollout"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v2.0.0-rc1");
        assertThat(TestJsonHelper.getString(data, "rolloutStrategy")).isEqualTo("AUTO_CANARY");
        assertThat(TestJsonHelper.getString(data, "targetEnvironment")).isEqualTo("staging");
        assertThat(data.get("observationWindowMinutes").asInt()).isEqualTo(120);
    }

    @Test
    void shouldFailCreateDuplicatePlan() {
        String pid = createProject("RoDup");
        createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans", Map.of(
                "releaseLabel", "v1.0.0"
        ));
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldFailCreatePlanWithoutLabel() {
        String pid = createProject("RoNoLabel");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans", Map.of());
        assertCode(res, "VALIDATION_ERROR");
    }

    // ========================
    // Rollout Plan - List / Get
    // ========================

    @Test
    void shouldListRolloutPlans() {
        String pid = createProject("RoList");
        createPlan(pid, "v1.0.0");
        createPlan(pid, "v1.1.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldGetRolloutPlan() {
        String pid = createProject("RoGet");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(planId);
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.0.0");
    }

    @Test
    void shouldFailGetNonexistentPlan() {
        String pid = createProject("RoGetNone");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Rollout Plan - Update
    // ========================

    @Test
    void shouldUpdateRolloutPlan() {
        String pid = createProject("RoUpdate");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId, Map.of(
                "rolloutStrategy", "AUTO_BLUE_GREEN",
                "targetEnvironment", "production-us-east",
                "observationWindowMinutes", 180
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "rolloutStrategy")).isEqualTo("AUTO_BLUE_GREEN");
        assertThat(TestJsonHelper.getString(data, "targetEnvironment")).isEqualTo("production-us-east");
        assertThat(data.get("observationWindowMinutes").asInt()).isEqualTo(180);
    }

    // ========================
    // Rollout Plan - Status Transitions
    // ========================

    @Test
    void shouldTransitionDraftToReady() {
        String pid = createProject("RoDraft2Ready");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=READY", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "rolloutStatus"))
                .isEqualTo("READY");
    }

    @Test
    void shouldTransitionDraftToCancelled() {
        String pid = createProject("RoDraft2Cancelled");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=CANCELLED", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "rolloutStatus"))
                .isEqualTo("CANCELLED");
    }

    @Test
    void shouldFailInvalidTransitionFromDraft() {
        String pid = createProject("RoInvalidDraft");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=COMPLETED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldFailInvalidTransitionFromCompleted() {
        String pid = createProject("RoCompleteLocked");
        String planId = createPlan(pid, "v1.0.0");
        put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=READY", Map.of());
        put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=CANCELLED", Map.of());
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/status?status=COMPLETED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    // ========================
    // Rollout Steps
    // ========================

    @Test
    void shouldInitDefaultSteps() {
        String pid = createProject("RoSteps");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(steps.isArray()).isTrue();
        assertThat(steps.size()).isEqualTo(5);
        assertThat(TestJsonHelper.getString(steps.get(0), "stepKey")).isEqualTo("CODE_REVIEW");
        assertThat(TestJsonHelper.getString(steps.get(0), "stepStatus")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(steps.get(4), "stepKey")).isEqualTo("STAGING_DEPLOY");
    }

    @Test
    void shouldCreateStep() {
        String pid = createProject("RoStepCreate");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps", Map.of(
                "stepOrder", 10,
                "stepKey", "CUSTOM_CHECK",
                "displayName", "Custom Security Check",
                "verificationScope", "PRE_RELEASE",
                "required", 1,
                "blocking", 1,
                "instructions", "Run security scan",
                "expectedResult", "No critical findings"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "stepKey")).isEqualTo("CUSTOM_CHECK");
        assertThat(TestJsonHelper.getString(data, "stepStatus")).isEqualTo("PENDING");
    }

    @Test
    void shouldUpdateStepStatusPassed() {
        String pid = createProject("RoStepPass");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        String stepId = TestJsonHelper.getString(TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/" + stepId
                + "/status?stepStatus=PASSED&actualResult=passed&operatorId=1", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "stepStatus"))
                .isEqualTo("PASSED");
    }

    @Test
    void shouldUpdateStepStatusFailed() {
        String pid = createProject("RoStepFail");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        String stepId = TestJsonHelper.getString(TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/" + stepId
                + "/status?stepStatus=FAILED&actualResult=failed", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "stepStatus"))
                .isEqualTo("FAILED");
    }

    @Test
    void shouldUpdateStep() {
        String pid = createProject("RoStepUpd");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        String stepId = TestJsonHelper.getString(TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/" + stepId, Map.of(
                "stepStatus", "IN_PROGRESS",
                "startedAt", "2025-01-01T10:00:00"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "stepStatus"))
                .isEqualTo("IN_PROGRESS");
    }

    // ========================
    // Verification Records
    // ========================

    @Test
    void shouldCreateVerification() {
        String pid = createProject("RoVerCreate");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "PRE_RELEASE",
                "verificationKey", "SMOKE_TEST_1",
                "displayName", "Smoke Test",
                "verificationStatus", "PASSED",
                "severity", "MEDIUM",
                "summary", "All smoke tests passed"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "displayName")).isEqualTo("Smoke Test");
        assertThat(TestJsonHelper.getString(data, "verificationStatus")).isEqualTo("PASSED");
    }

    @Test
    void shouldCreateVerificationWithIncidentReference() {
        String pid = createProject("RoVerIncident");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "OBSERVATION",
                "verificationKey", "INCIDENT_001",
                "displayName", "Incident Check",
                "verificationStatus", "FAILED",
                "severity", "CRITICAL",
                "summary", "Open critical incident",
                "detail", "Incident #12345 is still open"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "verificationStatus")).isEqualTo("FAILED");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("CRITICAL");
    }

    @Test
    void shouldListVerifications() {
        String pid = createProject("RoVerList");
        String planId = createPlan(pid, "v1.0.0");
        post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "PRE_RELEASE",
                "displayName", "Verification 1",
                "severity", "LOW",
                "summary", "v1"
        ));
        post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "OBSERVATION",
                "displayName", "Verification 2",
                "severity", "HIGH",
                "summary", "v2"
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldListVerificationsByPhase() {
        String pid = createProject("RoVerPhase");
        String planId = createPlan(pid, "v1.0.0");
        post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "PRE_RELEASE",
                "displayName", "Pre-release check",
                "severity", "MEDIUM",
                "summary", "pre"
        ));
        post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "OBSERVATION",
                "displayName", "Observation check",
                "severity", "HIGH",
                "summary", "obs"
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId
                + "/verifications?phase=OBSERVATION");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getString(data.get(0), "verificationPhase")).isEqualTo("OBSERVATION");
    }

    @Test
    void shouldUpdateVerification() {
        String pid = createProject("RoVerUpd");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> createRes = post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "PRE_RELEASE",
                "displayName", "Check",
                "verificationStatus", "PENDING",
                "severity", "MEDIUM",
                "summary", "check"
        ));
        String verId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()).get("data"), "id");

        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications/" + verId,
                Map.of("verificationStatus", "FAILED", "severity", "CRITICAL"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "verificationStatus")).isEqualTo("FAILED");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("CRITICAL");
    }

    // ========================
    // Dashboard, Summary, Report
    // ========================

    @Test
    void shouldGetDashboard() {
        String pid = createProject("RoDashboard");
        createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/readiness-dashboard?releaseLabel=v1.0.0");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "projectId")).isEqualTo(pid);
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.0.0");
        assertThat(data.has("overallReadinessStatus")).isTrue();
    }

    @Test
    void shouldGetSummary() {
        String pid = createProject("RoSummary");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/summary");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "planId")).isEqualTo(planId);
        assertThat(data.get("totalSteps").asInt()).isEqualTo(5);
    }

    @Test
    void shouldGenerateReport() {
        String pid = createProject("RoReport");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/report");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "releaseLabel")).isEqualTo("v1.0.0");
        assertThat(TestJsonHelper.getString(data, "reportMarkdown")).contains("Release Readiness Report");
        assertThat(data.get("steps").isArray()).isTrue();
        assertThat(data.get("verifications").isArray()).isTrue();
    }

    @Test
    void shouldFailGetSummaryForNonexistentPlan() {
        String pid = createProject("RoSumBad");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/999999/summary");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Plan with steps and verifications
    // ========================

    @Test
    void shouldGetPlanWithStepCounts() {
        String pid = createProject("RoPlanCounts");
        String planId = createPlan(pid, "v1.0.0");

        // Pass one step
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        String stepId = TestJsonHelper.getString(TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");
        put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/" + stepId
                + "/status?stepStatus=PASSED", Map.of());

        // Create a failed verification
        post("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications", Map.of(
                "verificationPhase", "PRE_RELEASE",
                "displayName", "Blocking Check",
                "verificationStatus", "FAILED",
                "severity", "BLOCKING",
                "summary", "blocking issue"
        ));

        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("stepCount").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(data.get("passedStepCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(data.get("verificationCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(data.get("blockingVerificationCount").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldUpdateStepWithEvidence() {
        String pid = createProject("RoStepEvidence");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps");
        String stepId = TestJsonHelper.getString(TestJsonHelper.parse(listRes.getBody()).get("data").get(0), "id");

        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/" + stepId
                + "/status?stepStatus=PASSED&actualResult=ok", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "stepStatus")).isEqualTo("PASSED");
    }

    @Test
    void shouldFailUpdateNonexistentStep() {
        String pid = createProject("RoStepBad");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = put("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps/999999"
                + "/status?stepStatus=PASSED", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldCreateStepWithDefaults() {
        String pid = createProject("RoStepDefault");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = post("/api/projects/" + pid + "/rollout/plans/" + planId + "/steps", Map.of(
                "stepKey", "DEFAULT_CHECK",
                "displayName", "Default Check"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "stepStatus")).isEqualTo("PENDING");
    }

    @Test
    void shouldListEmptyVerifications() {
        String pid = createProject("RoVerEmpty");
        String planId = createPlan(pid, "v1.0.0");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/plans/" + planId + "/verifications");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(0);
    }

    @Test
    void shouldDashboardReturnDefaultsWithNoData() {
        String pid = createProject("RoDashEmpty");
        ResponseEntity<String> res = get("/api/projects/" + pid + "/rollout/readiness-dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "projectId")).isEqualTo(pid);
        assertThat(data.has("overallReadinessStatus")).isTrue();
    }
}
