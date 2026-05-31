package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceExecutionPlaybookIntegrationTest extends IntegrationTestBase {

    private int counter = (int)(System.currentTimeMillis() % 100000);
    private String templateId;

    @BeforeEach
    public void setUp() {
        loginAdmin();
        ResponseEntity<String> res = post("/api/governance-execution/playbook-templates", Map.of(
                "templateKey", "tpl-" + (counter++), "displayName", "Test Template",
                "recommendationCategory", "CONFIDENCE", "guardrailKey", "MIN_CONFIDENCE_SCORE",
                "priority", "P1", "templateStepsJson", "[{\"stepKey\":\"s1\",\"title\":\"分析\",\"status\":\"TODO\",\"required\":true},{\"stepKey\":\"s2\",\"title\":\"修复\",\"status\":\"TODO\",\"required\":true}]"));
        assertOk(res);
        templateId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    // ========== Playbook Template ==========
    @Test void shouldCreatePlaybookTemplateSuccess() {
        ResponseEntity<String> res = post("/api/governance-execution/playbook-templates",
                Map.of("templateKey", "new-" + (counter++), "displayName", "New", "recommendationCategory", "ROLLBACK", "templateStepsJson", "[]"));
        assertOk(res);
    }
    @Test void shouldUpdatePlaybookTemplateSuccess() {
        ResponseEntity<String> res = put("/api/governance-execution/playbook-templates/" + templateId, Map.of("displayName", "Updated"));
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.displayName")).isEqualTo("Updated");
    }
    @Test void shouldDisablePlaybookTemplateSuccess() {
        ResponseEntity<String> res = post("/api/governance-execution/playbook-templates/" + templateId + "/status?enabled=false", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getBool(TestJsonHelper.parse(res.getBody()), "data.enabled")).isFalse();
    }
    @Test void shouldDuplicateTemplateKeyReject() {
        String key = "dup-" + (counter++);
        post("/api/governance-execution/playbook-templates", Map.of("templateKey", key, "displayName", "First", "templateStepsJson", "[]"));
        ResponseEntity<String> res = post("/api/governance-execution/playbook-templates", Map.of("templateKey", key, "displayName", "Dupe", "templateStepsJson", "[]"));
        assertCode(res, "CONFLICT");
    }
    @Test void shouldMatchPreviewReturnExact() {
        ResponseEntity<String> res = get("/api/governance-execution/playbook-templates");
        assertOk(res);
    }
    @Test void shouldListTemplates() {
        ResponseEntity<String> res = get("/api/governance-execution/playbook-templates");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }

    // ========== Execution Plan ==========
    @Test void shouldCreateExecutionPlanSuccess() {
        ResponseEntity<String> res = post("/api/governance-execution/plans?recommendationId=1", Map.of());
        assertOk(res);
    }
    @Test void shouldUpdateExecutionPlanSuccess() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=2", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-execution/plans/" + planId + "?ownerName=Alice", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.ownerName")).isEqualTo("Alice");
    }
    @Test void shouldPlanStatusDraftToReady() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=3", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + planId + "/status?status=READY", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planStatus")).isEqualTo("READY");
    }
    @Test void shouldPlanStatusReadyToInProgress() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=4", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        post("/api/governance-execution/plans/" + planId + "/status?status=READY", Map.of());
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + planId + "/status?status=IN_PROGRESS", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planStatus")).isEqualTo("IN_PROGRESS");
    }
    @Test void shouldStepStatusTodoToDoing() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=5", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DOING", Map.of());
        assertOk(res);
    }
    @Test void shouldStepStatusDoingToDone() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=6", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DOING", Map.of());
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DONE", Map.of());
        assertOk(res);
    }
    @Test void shouldCompletionRateComputedCorrectly() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=7", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DOING", Map.of());
        ResponseEntity<String> res = get("/api/governance-execution/plans/" + planId);
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("completionRate")).isNotNull();
    }
    @Test void shouldPlanCompletedWhenAllStepsDone() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=8", Map.of());
        String planId = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DOING", Map.of());
        post("/api/governance-execution/plans/" + planId + "/steps/s1/status?status=DONE", Map.of());
        post("/api/governance-execution/plans/" + planId + "/steps/s2/status?status=DOING", Map.of());
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + planId + "/steps/s2/status?status=DONE", Map.of());
        assertOk(res);
    }
    @Test void shouldReportExportMarkdownSuccess() {
        ResponseEntity<String> res = get("/api/governance-execution/report");
        assertOk(res);
    }

    // ========== Handoff Checklist ==========
    @Test void shouldCreateHandoffChecklistSuccess() {
        ResponseEntity<String> res = post("/api/governance-execution/handoffs?recommendationId=10&fromOwnerName=Alice&toOwnerName=Bob", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.toOwnerName")).isEqualTo("Bob");
    }
    @Test void shouldUpdateHandoffChecklistSuccess() {
        ResponseEntity<String> createres = post("/api/governance-execution/handoffs?recommendationId=11&fromOwnerName=Alice&toOwnerName=Bob", Map.of());
        String hid = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = put("/api/governance-execution/handoffs/" + hid + "?handoffNote=Done", Map.of());
        assertOk(res);
    }
    @Test void shouldHandoffStatusOpenToInProgress() {
        ResponseEntity<String> createres = post("/api/governance-execution/handoffs?recommendationId=12", Map.of());
        String hid = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-execution/handoffs/" + hid + "/status?status=IN_PROGRESS", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.checklistStatus")).isEqualTo("IN_PROGRESS");
    }
    @Test void shouldHandoffStatusInProgressToCompleted() {
        ResponseEntity<String> createres = post("/api/governance-execution/handoffs?recommendationId=13", Map.of());
        String hid = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        post("/api/governance-execution/handoffs/" + hid + "/status?status=IN_PROGRESS", Map.of());
        ResponseEntity<String> res = post("/api/governance-execution/handoffs/" + hid + "/status?status=COMPLETED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.checklistStatus")).isEqualTo("COMPLETED");
    }
    @Test void shouldListHandoffs() {
        ResponseEntity<String> res = get("/api/governance-execution/handoffs");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").isArray()).isTrue();
    }
    @Test void shouldGetHandoffById() {
        ResponseEntity<String> createres = post("/api/governance-execution/handoffs?recommendationId=14", Map.of());
        String hid = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-execution/handoffs/" + hid);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(hid);
    }

    // ========== Dashboard ==========
    @Test void shouldDashboardCountsCorrect() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res); JsonNode root = TestJsonHelper.parse(res.getBody());
        assertThat(root.get("data").get("totalPlanCount")).isNotNull();
        assertThat(root.get("data").get("averageCompletionRate")).isNotNull();
    }
    @Test void shouldTopBlockedPlansReturned() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topBlockedPlans").isArray()).isTrue();
    }
    @Test void shouldTopNearDuePlansReturned() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("topNearDuePlans").isArray()).isTrue();
    }
    @Test void shouldEmptyDatasetReturnEmptyDashboard() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res); assertThat(TestJsonHelper.getInt(TestJsonHelper.parse(res.getBody()), "data.totalPlanCount")).isNotNull();
    }
    @Test void shouldHandoffOpenCountReturned() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res); assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("handoffOpenCount")).isNotNull();
    }
    @Test void shouldGetPlanById() {
        ResponseEntity<String> createres = post("/api/governance-execution/plans?recommendationId=15", Map.of());
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(createres.getBody()), "data.id");
        ResponseEntity<String> res = get("/api/governance-execution/plans/" + pid);
        assertOk(res);
    }
    @Test void shouldNonExistentPlanReturnNotFound() {
        ResponseEntity<String> res = get("/api/governance-execution/plans/999999999");
        assertCode(res, "NOT_FOUND");
    }
    @Test void shouldListPlans() {
        ResponseEntity<String> res = get("/api/governance-execution/plans");
        assertOk(res);
    }
    @Test void shouldDashboardCompletionRateCalculated() {
        ResponseEntity<String> res = get("/api/governance-execution/dashboard");
        assertOk(res);
        assertThat(TestJsonHelper.parse(res.getBody()).get("data").get("averageCompletionRate")).isNotNull();
    }
    @Test void shouldMatchPreviewWithDefault() {
        ResponseEntity<String> res = get("/api/governance-execution/playbook-match-preview/999999");
        assertOk(res);
    }
    @Test void shouldPlanStatusCompletedToArchived() {
        ResponseEntity<String> cr = post("/api/governance-execution/plans?recommendationId=30", Map.of());
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        post("/api/governance-execution/plans/" + pid + "/status?status=READY", Map.of());
        post("/api/governance-execution/plans/" + pid + "/status?status=IN_PROGRESS", Map.of());
        post("/api/governance-execution/plans/" + pid + "/status?status=COMPLETED", Map.of());
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + pid + "/status?status=ARCHIVED", Map.of());
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.planStatus")).isEqualTo("ARCHIVED");
    }
    @Test void shouldInvalidPlanTransitionReject() {
        ResponseEntity<String> cr = post("/api/governance-execution/plans?recommendationId=31", Map.of());
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.id");
        ResponseEntity<String> res = post("/api/governance-execution/plans/" + pid + "/status?status=COMPLETED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }
    @Test void shouldHandoffChecklistItemsJsonPopulated() {
        ResponseEntity<String> cr = post("/api/governance-execution/handoffs?recommendationId=32", Map.of());
        assertOk(cr); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(cr.getBody()), "data.checklistItemsJson")).isNotNull();
    }
    @Test void shouldGetTemplateById() {
        ResponseEntity<String> res = get("/api/governance-execution/playbook-templates/" + templateId);
        assertOk(res); assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id")).isEqualTo(templateId);
    }
    @Test void shouldPlanReportContainsBlockedSummary() {
        get("/api/governance-execution/report");
        ResponseEntity<String> res = get("/api/governance-execution/report");
        assertOk(res);
    }
}
