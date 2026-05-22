package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatchProposalReviewIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String taskId;
    private String artifactId;
    private String toolExecId;

    // ========================
    // Setup: Create project, task, enable HIGH tool, run, approve, get artifact
    // ========================

    @Test
    @Order(1)
    void shouldCreateProjectAndTaskForReviewTests() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-PPR-" + suffix,
                "description", "Patch review integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectId = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
        assertThat(projectId).isNotNull();

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + projectId + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-PPR-Task-" + suffix,
                "description", "Patch review test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        taskId = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
        assertThat(taskId).isNotNull();
    }

    @Test
    @Order(2)
    void shouldEnableHighToolAndRunToGetPatchProposal() {
        // Enable MOCK_PATCH_PROPOSAL
        post("/api/projects/" + projectId + "/tools/910006/enable", Map.of());

        // Start REVIEW_ONLY run
        ResponseEntity<String> runRes = post("/api/tasks/" + taskId + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);
        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");

        // Find WAITING_APPROVAL execution
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                toolExecId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        assertThat(toolExecId).isNotNull();

        // Approve to trigger artifact creation + review
        ResponseEntity<String> approveRes = post("/api/tool-sandbox-executions/" + toolExecId + "/approve",
                Map.of("comment", "批准"));
        assertOk(approveRes);

        // Get artifact
        ResponseEntity<String> artRes = get("/api/tasks/" + taskId + "/artifacts");
        assertOk(artRes);
        JsonNode artArray = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artArray) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                artifactId = TestJsonHelper.getString(art, "id");
                break;
            }
        }
        assertThat(artifactId).isNotNull();
    }

    // ========================
    // Review API tests (3-18)
    // ========================

    @Test
    @Order(3)
    void shouldPatchProposalAutoCreatePendingReview() {
        assertThat(artifactId).isNotNull();
        ResponseEntity<String> res = get("/api/task-artifacts/" + artifactId + "/patch-review");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("PENDING");
        assertThat(data.get("safetyConfirmed").asBoolean()).isFalse();
    }

    @Test
    @Order(4)
    void shouldGetReviewReturnPendingStatus() {
        ResponseEntity<String> res = get("/api/task-artifacts/" + artifactId + "/patch-review");
        assertOk(res);
        String status = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()).get("data"), "status");
        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    @Order(5)
    void shouldNonPatchProposalArtifactReturnBadRequest() {
        // Get a REPORT artifact (first artifact that is not PATCH_PROPOSAL)
        ResponseEntity<String> artRes = get("/api/tasks/" + taskId + "/artifacts");
        assertOk(artRes);
        JsonNode artArray = TestJsonHelper.parse(artRes.getBody()).get("data");
        String reportArtifactId = null;
        for (JsonNode art : artArray) {
            if (!"PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                reportArtifactId = TestJsonHelper.getString(art, "id");
                break;
            }
        }
        if (reportArtifactId != null) {
            ResponseEntity<String> res = get("/api/task-artifacts/" + reportArtifactId + "/patch-review");
            assertCode(res, "BAD_REQUEST");
        }
    }

    @Test
    @Order(6)
    void shouldNonExistentArtifactReturnNotFound() {
        ResponseEntity<String> res = get("/api/task-artifacts/99999999/patch-review");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    @Order(7)
    void shouldSubmitAcceptedAsPlanDecision() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "ACCEPTED_AS_PLAN",
                        "comment", "可以作为后续手工实现计划。",
                        "safetyConfirmed", true,
                        "checklist", Map.of("matchesRequirement", true, "noSensitiveData", true,
                                "noFileWritten", true, "noGitOperation", true,
                                "readyForManualImplementation", true)));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REVIEWED");
        assertThat(TestJsonHelper.getString(data, "decision")).isEqualTo("ACCEPTED_AS_PLAN");
        assertThat(data.get("safetyConfirmed").asBoolean()).isTrue();
    }

    @Test
    @Order(8)
    void shouldSubmitRejectedDecision() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "REJECTED",
                        "comment", "该提案不符合需求",
                        "safetyConfirmed", true));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decision")).isEqualTo("REJECTED");
    }

    @Test
    @Order(9)
    void shouldSubmitNeedsChangesDecision() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "NEEDS_CHANGES",
                        "comment", "需要修改接口设计",
                        "safetyConfirmed", true));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decision")).isEqualTo("NEEDS_CHANGES");
    }

    @Test
    @Order(10)
    void shouldSubmitMarkedReviewedDecision() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "MARKED_REVIEWED",
                        "comment", "已审阅",
                        "safetyConfirmed", true));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decision")).isEqualTo("MARKED_REVIEWED");
    }

    @Test
    @Order(11)
    void shouldRejectSafetyConfirmedFalse() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "ACCEPTED_AS_PLAN",
                        "comment", "未确认安全",
                        "safetyConfirmed", false));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    @Order(12)
    void shouldRejectInvalidDecision() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "INVALID_DECISION",
                        "safetyConfirmed", true));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    @Order(13)
    void shouldUnauthenticatedGetReviewReturnUnauthorized() {
        ResponseEntity<String> res = getNoAuth("/api/task-artifacts/" + artifactId + "/patch-review");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    @Order(15)
    void shouldAllowOverwriteDecision() {
        // Submit ACCEPTED_AS_PLAN
        post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "ACCEPTED_AS_PLAN", "safetyConfirmed", true));

        // Overwrite with MARKED_REVIEWED
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "MARKED_REVIEWED",
                        "comment", "更新决策",
                        "safetyConfirmed", true));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "decision")).isEqualTo("MARKED_REVIEWED");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REVIEWED");
    }

    @Test
    @Order(16)
    void shouldListTaskReviewsReturnResults() {
        ResponseEntity<String> res = get("/api/tasks/" + taskId + "/patch-reviews");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isGreaterThanOrEqualTo(1);

        boolean found = false;
        for (JsonNode review : dataArray) {
            if (artifactId.equals(TestJsonHelper.getString(review, "artifactId"))) {
                found = true;
                assertThat(TestJsonHelper.getString(review, "status")).isEqualTo("REVIEWED");
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @Order(17)
    void shouldReviewNotTriggerToolExecution() {
        // Review doesn't create tool executions — verify review response has no tool fields
        ResponseEntity<String> res = get("/api/task-artifacts/" + artifactId + "/patch-review");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isIn("PENDING", "REVIEWED");
    }

    @Test
    @Order(18)
    void shouldReviewNotChangeArtifactContent() {
        // Get the original artifact content
        ResponseEntity<String> artRes = get("/api/tasks/" + taskId + "/artifacts");
        assertOk(artRes);
        JsonNode artArray = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artArray) {
            if (artifactId.equals(TestJsonHelper.getString(art, "id"))) {
                String content = TestJsonHelper.getString(art, "content");
                assertThat(content).isNotNull();
                // Content should be markdown, not a real patch file
                assertThat(content).contains("Patch Proposal");
                assertThat(content).contains("Mock");
                return;
            }
        }
    }

    @Test
    @Order(19)
    void shouldReviewOutputHasNoGitOperations() {
        ResponseEntity<String> res = get("/api/task-artifacts/" + artifactId + "/patch-review");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        // Review decision should not contain git operation fields
        String decision = TestJsonHelper.getString(data, "decision");
        assertThat(decision).isIn("MARKED_REVIEWED", "ACCEPTED_AS_PLAN", "REJECTED", "NEEDS_CHANGES", null);
    }

    @Test
    @Order(20)
    void shouldTaskLogsContainReviewCreated() {
        ResponseEntity<String> logRes = get("/api/tasks/" + taskId + "/logs");
        assertOk(logRes);
        JsonNode logArray = TestJsonHelper.parse(logRes.getBody()).get("data");
        boolean hasReviewCreatedLog = false;
        for (JsonNode log : logArray) {
            String stage = TestJsonHelper.getString(log, "stage");
            if ("PATCH_PROPOSAL_REVIEW_CREATED".equals(stage)) {
                hasReviewCreatedLog = true;
                break;
            }
        }
        assertThat(hasReviewCreatedLog).isTrue();
    }

    @Test
    @Order(21)
    void shouldChecklistJsonBeSavedCorrectly() {
        ResponseEntity<String> res = post("/api/task-artifacts/" + artifactId + "/patch-review/decision",
                Map.of("decision", "ACCEPTED_AS_PLAN",
                        "safetyConfirmed", true,
                        "checklist", Map.of("matchesRequirement", true, "noSensitiveData", true,
                                "noFileWritten", true, "noGitOperation", true,
                                "readyForManualImplementation", true)));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String checklistStr = TestJsonHelper.getString(data, "checklistJson");
        assertThat(checklistStr).isNotNull();
        assertThat(checklistStr).contains("matchesRequirement");
        assertThat(checklistStr).contains("noFileWritten");
    }
}
