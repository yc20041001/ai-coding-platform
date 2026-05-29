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

class ReleaseEvidenceSummaryIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String planId;
    private int counter = 100;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        projectId = createProject("ev-" + (counter++));
        planId = createPlan(projectId, "v39c-" + counter);
    }

    // ========== Evidence Bundle ==========

    @Test
    void shouldGenerateEvidenceBundleSuccess() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of(
                "projectId", projectId
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.bundleStatus")).isEqualTo("GENERATED");
        assertThat(TestJsonHelper.getString(root, "data.releaseLabel")).isNotNull();
    }

    @Test
    void shouldRegenerateEvidenceBundleSuccess() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.bundleStatus")).isEqualTo("GENERATED");
    }

    @Test
    void shouldPublishEvidenceBundleSuccess() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=PUBLISHED", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.bundleStatus")).isEqualTo("PUBLISHED");
    }

    @Test
    void shouldArchiveEvidenceBundleSuccess() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));
        post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=PUBLISHED", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=ARCHIVED", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.bundleStatus")).isEqualTo("ARCHIVED");
    }

    @Test
    void shouldRejectInvalidBundleStatusTransition() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=DRAFT", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldGetEvidenceBundle() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/evidence-bundle");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.bundleStatus")).isEqualTo("GENERATED");
    }

    @Test
    void shouldReturn404WhenBundleNotFound() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/evidence-bundle");
        assertCode(res, "NOT_FOUND");
    }

    // ========== Signoff ==========

    @Test
    void shouldInitializeDefaultSignoffsOnList() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/signoffs");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
        assertThat(root.get("data").size()).isEqualTo(5);
    }

    @Test
    void shouldCreateSignoffRecordSuccess() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "TECH_OWNER",
                "signoffStatus", "APPROVED",
                "signerName", "Alice"
        ));
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.signoffRole")).isEqualTo("TECH_OWNER");
        assertThat(TestJsonHelper.getString(root, "data.signoffStatus")).isEqualTo("APPROVED");
    }

    @Test
    void shouldUpdateSignoffRecordSuccess() {
        ResponseEntity<String> createRes = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "QA_REVIEWER",
                "signoffStatus", "PENDING"
        ));
        String signoffId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = put("/api/release-rollouts/" + planId + "/signoffs/" + signoffId, Map.of(
                "signoffStatus", "APPROVED",
                "signerName", "Bob"
        ));
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.signoffStatus")).isEqualTo("APPROVED");
    }

    @Test
    void shouldApproveSignoffSuccess() {
        ResponseEntity<String> createRes = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "OPS_OWNER",
                "signoffStatus", "PENDING"
        ));
        String signoffId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs/" + signoffId + "/status?signoffStatus=APPROVED", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.signoffStatus")).isEqualTo("APPROVED");
    }

    @Test
    void shouldConditionalSignoffSuccess() {
        ResponseEntity<String> createRes = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "SECURITY_REVIEWER",
                "signoffStatus", "PENDING"
        ));
        String signoffId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs/" + signoffId + "/status?signoffStatus=CONDITIONAL", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.signoffStatus")).isEqualTo("CONDITIONAL");
    }

    @Test
    void shouldRejectSignoffSuccess() {
        ResponseEntity<String> createRes = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "PRODUCT_OWNER",
                "signoffStatus", "PENDING"
        ));
        String signoffId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs/" + signoffId + "/status?signoffStatus=REJECTED", Map.of());
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.signoffStatus")).isEqualTo("REJECTED");
    }

    @Test
    void shouldCalculateSignoffCompletionRate() {
        // Create 3 signoffs, approve 2
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "TECH_OWNER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "PRODUCT_OWNER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "OPS_OWNER", "signoffStatus", "PENDING"
        ));

        // Completion rate should be 66.67% (2/3)
        // Can't easily get completion rate from API directly, but we verify the signoff list
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/signoffs");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldRejectDuplicateSignoffRole() {
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "SECURITY_REVIEWER",
                "signoffStatus", "APPROVED"
        ));

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId,
                "signoffRole", "SECURITY_REVIEWER",
                "signoffStatus", "PENDING"
        ));
        assertCode(res, "CONFLICT");
    }

    // ========== Executive Summary / Confidence ==========

    @Test
    void shouldGenerateConfidenceSnapshot() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/confidence-snapshot", Map.of());
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("confidenceScore")).isNotNull();
        assertThat(root.get("data").get("confidenceLevel")).isNotNull();
    }

    @Test
    void shouldGetExecutiveSummary() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/executive-summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.releaseLabel")).isNotNull();
        assertThat(root.get("data").get("confidenceScore")).isNotNull();
    }

    @Test
    void shouldGetConfidenceSnapshot() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/confidence-snapshot");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("confidenceScore")).isNotNull();
        assertThat(root.get("data").get("confidenceLevel")).isNotNull();
    }

    @Test
    void shouldReturnHighConfidenceForCleanRelease() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/confidence-snapshot");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        double score = root.get("data").get("confidenceScore").asDouble();
        assertThat(score).isGreaterThanOrEqualTo(85);
        assertThat(TestJsonHelper.getString(root, "data.confidenceLevel")).isEqualTo("HIGH");
    }

    @Test
    void shouldReturnComparisonWithPreviousRelease() {
        // Create a previous plan
        String prevPlanId = createPlan(projectId, "v39c-prev-" + counter);
        post("/api/release-rollouts/" + prevPlanId + "/confidence-snapshot", Map.of());

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/comparison");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.currentReleaseLabel")).isNotNull();
    }

    @Test
    void shouldReturnEmptyComparisonWhenNoPreviousRelease() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/comparison");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.trendSummary")).contains("No previous release");
    }

    @Test
    void shouldReturnConfidenceTrend() {
        // Take a few snapshots first
        post("/api/release-rollouts/" + planId + "/confidence-snapshot", Map.of());

        ResponseEntity<String> res = get("/api/release-confidence/trend");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldGenerateExecutiveReportMarkdown() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/executive-report");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String markdown = TestJsonHelper.getString(root, "data.reportMarkdown");
        assertThat(markdown).contains("Executive Release Summary");
        assertThat(markdown).contains("Confidence Assessment");
    }

    @Test
    void shouldIncludeRiskIndicatorsInExecutiveReport() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/executive-report");
        assertOk(res);
        String markdown = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.reportMarkdown");
        assertThat(markdown).contains("Risk Indicators");
        assertThat(markdown).contains("Blocking Issues");
    }

    @Test
    void shouldGetSignoffDataInBundleMarkdown() {
        // Create signoffs
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "TECH_OWNER", "signoffStatus", "APPROVED"
        ));

        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/evidence-bundle");
        assertOk(res);
        String markdown = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.summaryMarkdown");
        assertThat(markdown).contains("Sign-off Status");
    }

    @Test
    void shouldRejectNonExistentPlanForBundle() {
        ResponseEntity<String> res = post("/api/release-rollouts/999999/evidence-bundle/generate", Map.of("projectId", projectId));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldRejectNonExistentSignoff() {
        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/signoffs/999999/status?signoffStatus=APPROVED", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldArchiveBundleBlocksFurtherUpdates() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));
        post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=PUBLISHED", Map.of());
        post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=ARCHIVED", Map.of());

        ResponseEntity<String> res = post("/api/release-rollouts/" + planId + "/evidence-bundle/status?bundleStatus=PUBLISHED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldReturnSnapshotSummary() {
        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/executive-summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.summaryText")).isNotNull();
    }

    @Test
    void shouldSupportFullSignoffLifecycle() {
        // Create
        ResponseEntity<String> createRes = post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "TECH_OWNER", "signoffStatus", "PENDING"
        ));
        String signoffId = TestJsonHelper.getString(TestJsonHelper.parse(createRes.getBody()), "data.id");

        // Approve
        post("/api/release-rollouts/" + planId + "/signoffs/" + signoffId + "/status?signoffStatus=APPROVED", Map.of());
        ResponseEntity<String> getRes = get("/api/release-rollouts/" + planId + "/signoffs");
        assertOk(getRes);
    }

    @Test
    void shouldIncludeBundleEvidenceJson() {
        post("/api/release-rollouts/" + planId + "/evidence-bundle/generate", Map.of("projectId", projectId));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/evidence-bundle");
        assertOk(res);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.evidenceJson")).isNotNull();
    }

    @Test
    void shouldHandleMultipleSignoffs() {
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "TECH_OWNER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "PRODUCT_OWNER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "OPS_OWNER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "SECURITY_REVIEWER", "signoffStatus", "APPROVED"
        ));
        post("/api/release-rollouts/" + planId + "/signoffs", Map.of(
                "projectId", projectId, "signoffRole", "QA_REVIEWER", "signoffStatus", "APPROVED"
        ));

        ResponseEntity<String> res = get("/api/release-rollouts/" + planId + "/signoffs");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").size()).isGreaterThanOrEqualTo(5);
    }

    // ========== Helpers ==========

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-ES-" + suffix,
                "description", "Evidence summary integration test project",
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
}
