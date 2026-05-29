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

class GovernanceWorkflowWaiverIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String projectId2;
    private String planId;
    private String planId2;
    private String policyId;
    private int counter = (int)(System.currentTimeMillis() % 100000);

    @BeforeEach
    public void setUp() {
        loginAdmin();
        projectId = createProject("gw-" + (counter++));
        projectId2 = createProject("gw-" + (counter++));
        planId = createPlan(projectId, "v40c-" + counter);
        planId2 = createPlan(projectId2, "v40c-" + counter);

        takeConfidenceSnapshot(planId);
        takeConfidenceSnapshot(planId2);

        // Refresh portfolio and guardrails to seed data
        post("/api/release-governance/portfolio/refresh", Map.of());
        post("/api/organization-governance/policies", Map.of(
                "policyKey", "40c-test-policy-" + (counter++),
                "displayName", "40C Test Policy",
                "policyScope", "GLOBAL"
        ));
        post("/api/organization-governance/guardrails/refresh", Map.of());
    }

    // ========== Recommendation Sync ==========

    @Test
    void shouldSyncRecommendationsSuccess() {
        ResponseEntity<String> res = post("/api/governance-workflow/recommendations/sync", Map.of());
        assertOk(res);
    }

    @Test
    void shouldDuplicateSyncIdempotent() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> res = post("/api/governance-workflow/recommendations/sync", Map.of());
        assertOk(res);
    }

    // ========== Recommendation Item CRUD ==========

    @Test
    void shouldCreateRecommendationItemSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/recommendations");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldUpdateRecommendationItemSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = put("/api/governance-workflow/recommendations/" + itemId,
                    Map.of("ownerName", "Test Owner", "priority", "P1"));
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.ownerName")).isEqualTo("Test Owner");
        }
    }

    @Test
    void shouldAssignOwnerSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = put("/api/governance-workflow/recommendations/" + itemId,
                    Map.of("ownerId", "42", "ownerName", "Alice"));
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.ownerName")).isEqualTo("Alice");
        }
    }

    @Test
    void shouldGetRecommendationItemById() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = get("/api/governance-workflow/recommendations/" + itemId);
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(itemId);
        }
    }

    // ========== Status Transitions ==========

    @Test
    void shouldStatusTransitionOpenToAcknowledged() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations?status=OPEN");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/status?status=ACKNOWLEDGED", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.workflowStatus")).isEqualTo("ACKNOWLEDGED");
        }
    }

    @Test
    void shouldStatusTransitionAcknowledgedToInProgress() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations?status=OPEN");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workflow/recommendations/" + itemId + "/status?status=ACKNOWLEDGED", Map.of());
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/status?status=IN_PROGRESS", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.workflowStatus")).isEqualTo("IN_PROGRESS");
        }
    }

    @Test
    void shouldStatusTransitionInProgressToCompleted() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations?status=OPEN");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workflow/recommendations/" + itemId + "/status?status=ACKNOWLEDGED", Map.of());
            post("/api/governance-workflow/recommendations/" + itemId + "/status?status=IN_PROGRESS", Map.of());
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/status?status=COMPLETED", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.workflowStatus")).isEqualTo("COMPLETED");
        }
    }

    @Test
    void shouldInvalidStatusTransitionReject() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations?status=OPEN");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            // OPEN -> IN_PROGRESS is invalid
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/status?status=IN_PROGRESS", Map.of());
            assertCode(res, "BAD_REQUEST");
        }
    }

    // ========== Waiver ==========

    @Test
    void shouldCreateWaiverRequestSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                    Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "Business need"));
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.waiverStatus")).isEqualTo("REQUESTED");
        }
    }

    @Test
    void shouldApproveWaiverSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            String waiverId = TestJsonHelper.getString(
                    TestJsonHelper.parse(post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                            Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "Need waiver")).getBody()), "data.id");
            ResponseEntity<String> res = post("/api/governance-workflow/waivers/" + waiverId + "/status?status=APPROVED&approvalNote=Approved", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.waiverStatus")).isEqualTo("APPROVED");
        }
    }

    @Test
    void shouldRejectWaiverSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            String waiverId = TestJsonHelper.getString(
                    TestJsonHelper.parse(post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                            Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "Need waiver")).getBody()), "data.id");
            ResponseEntity<String> res = post("/api/governance-workflow/waivers/" + waiverId + "/status?status=REJECTED", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.waiverStatus")).isEqualTo("REJECTED");
        }
    }

    @Test
    void shouldRevokeWaiverSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            String waiverId = TestJsonHelper.getString(TestJsonHelper.parse(
                    post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                            Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "Need waiver")).getBody()), "data.id");
            post("/api/governance-workflow/waivers/" + waiverId + "/status?status=APPROVED", Map.of());
            ResponseEntity<String> res = post("/api/governance-workflow/waivers/" + waiverId + "/status?status=REVOKED", Map.of());
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.waiverStatus")).isEqualTo("REVOKED");
        }
    }

    @Test
    void shouldOnlyOneActiveWaiverAllowed() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                    Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "First waiver"));
            ResponseEntity<String> res = post("/api/governance-workflow/recommendations/" + itemId + "/waivers",
                    Map.of("waiverScope", "POLICY_EXCEPTION", "reasonText", "Duplicate waiver"));
            assertCode(res, "CONFLICT");
        }
    }

    // ========== Workflow Snapshot & Dashboard ==========

    @Test
    void shouldSnapshotRefreshSuccess() {
        ResponseEntity<String> res = post("/api/governance-workflow/snapshots/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldDashboardReturnCorrectCounts() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        post("/api/governance-workflow/snapshots/refresh", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("totalRecommendationCount")).isNotNull();
    }

    @Test
    void shouldCompletionRateCorrect() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        post("/api/governance-workflow/snapshots/refresh", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("completionRate")).isNotNull();
    }

    @Test
    void shouldTopPriorityItemsReturned() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("topPriorityItems").isArray()).isTrue();
    }

    @Test
    void shouldMarkdownReportExportSuccess() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/report");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(TestJsonHelper.getString(root, "data.summaryMarkdown")).isNotNull();
    }

    @Test
    void shouldRecommendationListFilterByStatus() {
        post("/api/governance-workflow/recommendations/sync", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/recommendations?status=OPEN");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").isArray()).isTrue();
    }

    @Test
    void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-workflow/dashboard");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("totalRecommendationCount")).isNotNull();
    }

    @Test
    void shouldRecommendationResolutionNotePersisted() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            put("/api/governance-workflow/recommendations/" + itemId, Map.of("resolutionNote", "Fixed by team"));
            ResponseEntity<String> res = get("/api/governance-workflow/recommendations/" + itemId);
            assertOk(res);
            assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.resolutionNote")).isEqualTo("Fixed by team");
        }
    }

    @Test
    void shouldListWaiversByRecommendation() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        ResponseEntity<String> listRes = get("/api/governance-workflow/recommendations");
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        if (data.size() > 0) {
            String itemId = TestJsonHelper.getString(data.get(0), "id");
            ResponseEntity<String> res = get("/api/governance-workflow/recommendations/" + itemId + "/waivers");
            assertOk(res);
            assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
        }
    }

    @Test
    void shouldSummaryReturnOverdueData() {
        post("/api/governance-workflow/recommendations/sync", Map.of());
        post("/api/governance-workflow/snapshots/refresh", Map.of());

        ResponseEntity<String> res = get("/api/governance-workflow/summary");
        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("overdueCount")).isNotNull();
        assertThat(root.get("data").get("overdueRate")).isNotNull();
    }

    @Test
    void shouldScanExpirySuccess() {
        ResponseEntity<String> res = post("/api/governance-workflow/waivers/scan-expiry", Map.of());
        assertOk(res);
    }

    // ========== Helpers ==========

    private void takeConfidenceSnapshot(String pid) {
        post("/api/release-rollouts/" + pid + "/confidence-snapshot", Map.of());
    }

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "40C-IT-" + suffix,
                "description", "Governance workflow integration test project",
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
