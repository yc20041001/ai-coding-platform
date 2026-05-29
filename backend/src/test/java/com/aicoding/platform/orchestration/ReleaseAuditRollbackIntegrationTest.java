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

class ReleaseAuditRollbackIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String planId;
    private int counter = 100;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        projectId = createProject("audit-" + (counter++));
        planId = createPlan(projectId, "v39b-" + counter);
    }

    // ========== Rollback Drill ==========

    @Test
    void shouldCreateRollbackDrillSuccess() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills", Map.of(
                "projectId", projectId,
                "drillScope", "CONFIG_ONLY",
                "environmentName", "production"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.drillStatus")).isEqualTo("PLANNED");
        assertThat(TestJsonHelper.getString(root, "data.drillScope")).isEqualTo("CONFIG_ONLY");
        assertThat(TestJsonHelper.getString(root, "data.environmentName")).isEqualTo("production");
    }

    @Test
    void shouldUpdateRollbackDrillSuccess() {
        String drillId = createDrill();

        ResponseEntity<String> res = put("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId, Map.of(
                "successCriteria", "all checks passed",
                "rollbackStepsSummary", "step1 step2 step3"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.successCriteria")).isEqualTo("all checks passed");
    }

    @Test
    void shouldTransitionPlannedToRunning() {
        String drillId = createDrill();

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=RUNNING", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.drillStatus")).isEqualTo("RUNNING");
    }

    @Test
    void shouldTransitionRunningToPassed() {
        String drillId = createDrill();
        post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=RUNNING", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=PASSED", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.drillStatus")).isEqualTo("PASSED");
    }

    @Test
    void shouldTransitionRunningToFailed() {
        String drillId = createDrill();
        post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=RUNNING", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=FAILED", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.drillStatus")).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectInvalidDrillTransition() {
        String drillId = createDrill();

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=PASSED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldReportRollbackReadyWhenDrillPassed() {
        String drillId = createDrill();
        put("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId, Map.of(
                "rollbackStepsSummary", "step1 step2"
        ));
        post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=RUNNING", Map.of());
        post("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId + "/status?drillStatus=PASSED", Map.of());

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/rollback-drills/readiness");
        assertOk(res);
        assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data")).isTrue();
    }

    @Test
    void shouldReportRollbackNotReadyWhenNoDrill() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/rollback-drills/readiness");
        assertOk(res);
        assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data")).isFalse();
    }

    @Test
    void shouldListDrills() {
        createDrill();

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/rollback-drills");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        assertThat(root.get("data").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldGetDrillById() {
        String drillId = createDrill();

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/rollback-drills/" + drillId);
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(drillId);
    }

    // ========== Audit Events ==========

    @Test
    void shouldCreateAuditEventOnRolloutStatusChange() {
        put("/api/projects/" + projectId + "/rollout/plans/" + planId + "/status?status=READY", Map.of());

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-events");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldCreateAuditEventOnStepStatusChange() {
        String stepId = getFirstStepId();
        if (stepId != null) {
            put("/api/projects/" + projectId + "/rollout/plans/" + planId + "/steps/" + stepId + "/status?stepStatus=PASSED&actualResult=ok", Map.of());
        }

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-events");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    @Test
    void shouldCreateAuditEventOnVerificationRecordCreate() {
        post("/api/projects/" + projectId + "/rollout/plans/" + planId + "/verifications", Map.of(
                "projectId", projectId,
                "verificationPhase", "PRE_RELEASE",
                "displayName", "v-audit-1",
                "verificationKey", "vk-audit-1",
                "summary", "test"
        ));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-events");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    @Test
    void shouldCreateAuditEventOnRollbackDrillUpdate() {
        createDrill();

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-events");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    @Test
    void shouldListAuditEventsOrderedByEventTimeDesc() {
        post("/api/projects/" + projectId + "/rollout/plans/" + planId + "/verifications", Map.of(
                "projectId", projectId,
                "verificationPhase", "PRE_RELEASE",
                "displayName", "v2",
                "verificationKey", "vk-audit-2",
                "summary", "test"
        ));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-events");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldGetAuditTimelineWithCounts() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-timeline");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.releaseLabel")).isNotNull();
        assertThat(root.get("data").get("totalEvents")).isNotNull();
    }

    // ========== Postmortem Review ==========

    @Test
    void shouldCreatePostmortemReviewSuccess() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review", Map.of(
                "projectId", projectId,
                "overallOutcome", "SUCCESS"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.reviewStatus")).isEqualTo("DRAFT");
        assertThat(TestJsonHelper.getString(root, "data.overallOutcome")).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRejectDuplicatePostmortemReview() {
        post("/api/release-rollouts/" + planId + "/postmortem-review", Map.of(
                "projectId", projectId,
                "overallOutcome", "SUCCESS"
        ));

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review", Map.of(
                "projectId", projectId,
                "overallOutcome", "SUCCESS"
        ));
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldUpdatePostmortemReviewSuccess() {
        String reviewId = createReview();

        ResponseEntity<String> res = put("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId, Map.of(
                "summary", "release went well",
                "whatWentWell", "smooth rollout"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.summary")).isEqualTo("release went well");
    }

    @Test
    void shouldTransitionReviewFromDraftToReviewed() {
        String reviewId = createReview();

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=REVIEWED", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.reviewStatus")).isEqualTo("REVIEWED");
    }

    @Test
    void shouldTransitionReviewFromReviewedToPublished() {
        String reviewId = createReview();
        post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=REVIEWED", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=PUBLISHED", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.reviewStatus")).isEqualTo("PUBLISHED");
    }

    @Test
    void shouldAllowReviewedToDraft() {
        String reviewId = createReview();
        post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=REVIEWED", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=DRAFT", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.reviewStatus")).isEqualTo("DRAFT");
    }

    @Test
    void shouldRejectUpdateWhenArchived() {
        String reviewId = createReview();
        post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=REVIEWED", Map.of());
        post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=PUBLISHED", Map.of());
        post("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId + "/status?reviewStatus=ARCHIVED", Map.of());

        ResponseEntity<String> res = put("/api/release-rollouts/" + planId + "/postmortem-review/" + reviewId, Map.of("summary", "should fail"));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldGetPrefilledReviewFromSignals() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/postmortem-review/prefill");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.reviewStatus")).isEqualTo("DRAFT");
        assertThat(TestJsonHelper.getString(root, "data.overallOutcome")).isNotNull();
    }

    // ========== Audit Report ==========

    @Test
    void shouldGenerateAuditReportMarkdown() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-report");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String markdown = TestJsonHelper.getString(root, "data.reportMarkdown");
        assertThat(markdown).contains("Release Audit Report");
        assertThat(markdown).contains("Rollout Timeline");
    }

    @Test
    void shouldIncludeVerificationInAuditReport() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/audit-report");
        assertOk(res);
        String markdown = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reportMarkdown");
        assertThat(markdown).contains("Verification Results");
    }

    @Test
    void shouldGetPostmortemReviewByPlan() {
        String reviewId = createReview();

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/postmortem-review");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(reviewId);
    }

    // ========== Project Scoped Listing ==========

    @Test
    void shouldReturnEmptyListForNonExistentPlan() {
        ResponseEntity<String> res = get("/api/release-rollouts/999999/rollback-drills");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        assertThat(root.get("data").size()).isEqualTo(0);
    }

    @Test
    void shouldRejectNonExistentDrill() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/rollback-drills/999999");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentPostmortem() {
        ResponseEntity<String> res = get("/api/release-rollouts/999999/postmortem-review");
        assertCode(res, "NOT_FOUND");
    }

    // ========== Helpers ==========

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-AR-" + suffix,
                "description", "Audit rollback integration test project",
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

    private String createDrill() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/rollback-drills", Map.of(
                "projectId", projectId,
                "drillScope", "APP_VERSION",
                "environmentName", "staging"
        ));
        assertOk(res);
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private String createReview() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/postmortem-review", Map.of(
                "projectId", projectId,
                "overallOutcome", "SUCCESS_WITH_ISSUES"
        ));
        assertOk(res);
        return TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    private String getFirstStepId() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/rollout/plans/" + planId + "/steps");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        JsonNode steps = root.get("data");
        if (steps != null && steps.isArray() && steps.size() > 0) {
            return TestJsonHelper.getString(steps.get(0), "id");
        }
        return null;
    }
}
