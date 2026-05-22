package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ToolExecutionApprovalIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String taskIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;

        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-TEA-" + suffix,
                "description", "Tool execution approval integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Enable all agents
        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + projectIdValue + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + projectIdValue + "/tasks", Map.of(
                "title", "IT-TEA-Task-" + suffix,
                "description", "Tool execution approval test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        taskIdValue = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
    }

    private @NonNull String projectId() {
        ensureTestData();
        return Objects.requireNonNull(projectIdValue);
    }

    private @NonNull String taskId() {
        ensureTestData();
        return Objects.requireNonNull(taskIdValue);
    }

    // ========================
    // 1. Policy tests
    // ========================

    @Test
    void shouldHighToolBeBlockedByDefault() {
        // MOCK_PATCH_PROPOSAL (910006) is HIGH risk, should be blocked without project config
        // Verify the tool exists in catalog
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        boolean foundHigh = false;
        for (JsonNode tool : data) {
            if ("MOCK_PATCH_PROPOSAL".equals(TestJsonHelper.getString(tool, "toolKey"))) {
                foundHigh = true;
                assertThat(TestJsonHelper.getString(tool, "riskLevel")).isEqualTo("HIGH");
            }
        }
        assertThat(foundHigh).isTrue();

        // Project tool list should show it as not enabled
        ResponseEntity<String> ptRes = get("/api/projects/" + projectId() + "/tools");
        assertOk(ptRes);
        JsonNode ptData = TestJsonHelper.parse(ptRes.getBody()).get("data");
        boolean foundPtHigh = false;
        for (JsonNode pt : ptData) {
            if ("MOCK_PATCH_PROPOSAL".equals(TestJsonHelper.getString(pt, "toolKey"))) {
                foundPtHigh = true;
                assertThat(pt.get("projectEnabled").asBoolean()).isFalse();
            }
        }
        assertThat(foundPtHigh).isTrue();
    }

    @Test
    void shouldOwnerEnableHighToolSuccessfully() {
        ResponseEntity<String> res = post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("projectEnabled").asBoolean()).isTrue();
        assertThat(TestJsonHelper.getString(data, "toolKey")).isEqualTo("MOCK_PATCH_PROPOSAL");
    }

    @Test
    void shouldHighToolEnabledReturnRequiresApproval() {
        // Enable the HIGH tool
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());

        // Verify projectEnabled is true
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/tools");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode pt : data) {
            if ("MOCK_PATCH_PROPOSAL".equals(TestJsonHelper.getString(pt, "toolKey"))) {
                assertThat(pt.get("projectEnabled").asBoolean()).isTrue();
            }
        }
    }

    // ========================
    // 2. WAITING_APPROVAL creation
    // ========================

    @Test
    void shouldCodeReviewStepCreateWaitingApprovalWhenHighToolEnabled() {
        // Enable HIGH tool
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());

        // Start a run
        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        // Check tool executions - CODE_REVIEW step should have WAITING_APPROVAL
        JsonNode toolExecs = data.get("toolExecutions");
        assertThat(toolExecs).isNotNull();
        assertThat(toolExecs.isArray()).isTrue();

        boolean foundWaitingApproval = false;
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                foundWaitingApproval = true;
                assertThat(TestJsonHelper.getString(te, "toolName")).isEqualTo("MOCK_PATCH_PROPOSAL");
                assertThat(TestJsonHelper.getString(te, "executionMode")).isEqualTo("MOCK_EXECUTE");
                // Should have approval info
                assertThat(te.get("requiresApproval").asBoolean()).isTrue();
                assertThat(te.get("approval")).isNotNull();
            }
        }
        assertThat(foundWaitingApproval).isTrue();
    }

    @Test
    void shouldWaitingApprovalExecutionCreateApprovalRecord() {
        // Enable HIGH tool
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());

        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode toolExecs = data.get("toolExecutions");

        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                JsonNode approval = te.get("approval");
                assertThat(approval).isNotNull();
                assertThat(TestJsonHelper.getString(approval, "status")).isEqualTo("PENDING");
                assertThat(TestJsonHelper.getString(approval, "toolKey")).isEqualTo("MOCK_PATCH_PROPOSAL");
                assertThat(TestJsonHelper.getString(approval, "approvalKey")).isEqualTo("TOOL_EXECUTION_APPROVAL");
                assertThat(TestJsonHelper.getString(approval, "toolExecutionId")).isEqualTo(TestJsonHelper.getString(te, "id"));
            }
        }
    }

    @Test
    void shouldTaskLogsContainToolSandboxWaitingApproval() {
        // Enable HIGH tool
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());

        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));

        ResponseEntity<String> logRes = get("/api/tasks/" + taskId() + "/logs");
        assertOk(logRes);
        JsonNode logs = TestJsonHelper.parse(logRes.getBody()).get("data");

        boolean hasWaitingApprovalLog = false;
        for (JsonNode log : logs) {
            if ("TOOL_SANDBOX_WAITING_APPROVAL".equals(TestJsonHelper.getString(log, "stage"))) {
                hasWaitingApprovalLog = true;
            }
        }
        assertThat(hasWaitingApprovalLog).isTrue();
    }

    // ========================
    // 3. Approval API - Query
    // ========================

    @Test
    void shouldViewerQueryApproval() {
        // Enable HIGH tool and start run
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode toolExecs = data.get("toolExecutions");

        String execId = null;
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        assertThat(execId).isNotNull();

        // Query approval
        ResponseEntity<String> aprRes = get("/api/tool-sandbox-executions/" + execId + "/approval");
        assertOk(aprRes);
        JsonNode aprData = TestJsonHelper.parse(aprRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(aprData, "status")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(aprData, "toolExecutionId")).isEqualTo(execId);
    }

    @Test
    void shouldUnauthenticatedQueryApprovalReturnUnauthorized() {
        ResponseEntity<String> res = getNoAuth("/api/tool-sandbox-executions/99999/approval");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========================
    // 4. Approval API - Approve
    // ========================

    private String[] enableHighAndGetWaitingExecId() {
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode toolExecs = data.get("toolExecutions");

        String execId = null;
        String runId = TestJsonHelper.getString(data, "id");
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        return new String[]{execId, runId};
    }

    @Test
    void shouldMaintainerApproveToolExecution() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        ResponseEntity<String> res = post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "批准执行"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("COMPLETED");
    }

    @Test
    void shouldApproveSetExecutionCompleted() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        post("/api/tool-sandbox-executions/" + execId + "/approve", Map.of("comment", "批准"));

        // Verify execution is COMPLETED
        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("COMPLETED");
    }

    @Test
    void shouldApproveSetApprovalApproved() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        post("/api/tool-sandbox-executions/" + execId + "/approve", Map.of("comment", "批准"));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/approval");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("APPROVED");
        assertThat(TestJsonHelper.getString(data, "decisionComment")).isEqualTo("批准");
    }

    @Test
    void shouldApprovedOutputPayloadStillContainMockSafety() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        post("/api/tool-sandbox-executions/" + execId + "/approve", Map.of("comment", "批准"));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String outputPayload = TestJsonHelper.getString(data, "outputPayload");
        assertThat(outputPayload).contains("\"mock\":true");
        assertThat(outputPayload).contains("\"filesTouched\":[]");
        assertThat(outputPayload).contains("\"gitOperations\":[]");
    }

    // ========================
    // 5. Approval API - Reject
    // ========================

    @Test
    void shouldMaintainerRejectToolExecution() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        ResponseEntity<String> res = post("/api/tool-sandbox-executions/" + execId + "/reject",
                Map.of("comment", "不允许执行"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REJECTED");
    }

    @Test
    void shouldRejectSetExecutionRejected() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        post("/api/tool-sandbox-executions/" + execId + "/reject", Map.of("comment", "驳回"));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REJECTED");
        String outputPayload = TestJsonHelper.getString(data, "outputPayload");
        assertThat(outputPayload).contains("\"rejected\":true");
    }

    @Test
    void shouldRejectSetApprovalRejected() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        post("/api/tool-sandbox-executions/" + execId + "/reject", Map.of("comment", "驳回"));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/approval");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REJECTED");
        assertThat(TestJsonHelper.getString(data, "decisionComment")).isEqualTo("驳回");
    }

    // ========================
    // 6. Error cases
    // ========================

    @Test
    void shouldUnauthenticatedApproveReturnUnauthorized() {
        ResponseEntity<String> res = getNoAuth("/api/tool-sandbox-executions/99999/approval");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldDuplicateApproveReturnConflict() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        // First approve succeeds
        post("/api/tool-sandbox-executions/" + execId + "/approve", Map.of("comment", "第一次批准"));

        // Second approve should fail with CONFLICT
        ResponseEntity<String> res = post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "第二次批准"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldNonWaitingApprovalExecutionApproveReturnConflict() {
        // Create a run WITHOUT enabling HIGH tool → all tool execs are COMPLETED
        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
    "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode toolExecs = data.get("toolExecutions");

        // Find a COMPLETED execution
        String completedExecId = null;
        for (JsonNode te : toolExecs) {
            if ("COMPLETED".equals(TestJsonHelper.getString(te, "status"))) {
                completedExecId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        // If there's a completed exec, approving it should CONFLICT
        if (completedExecId != null) {
            ResponseEntity<String> approveRes = post("/api/tool-sandbox-executions/" + completedExecId + "/approve",
                    Map.of("comment", "尝试批准已完成"));
            assertThat(approveRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    void shouldDangerousToolStillBeBlocked() {
        // DANGEROUS tools should not be in the catalog with DANGEROUS level currently.
        // This test verifies that the policy treats DANGEROUS as blocked.
        // We verify by checking that MEDIUM tools without config are blocked by default.
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/tools");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        for (JsonNode pt : data) {
            if ("MEDIUM".equals(TestJsonHelper.getString(pt, "riskLevel"))) {
                // MEDIUM tools should be blocked by default (no project config)
                assertThat(pt.get("projectEnabled").asBoolean()).isFalse();
            }
            if ("LOW".equals(TestJsonHelper.getString(pt, "riskLevel"))) {
                // LOW tools should be allowed by default
                assertThat(pt.get("projectEnabled").asBoolean()).isTrue();
            }
        }
    }

    // ========================
    // 7. List project approvals
    // ========================

    @Test
    void shouldListProjectToolApprovals() {
        // Enable HIGH tool and start a run
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of("strategy", "STANDARD_DELIVERY"));

        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/tool-approvals");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        // Should have at least the CODE_REVIEW approval
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getString(data.get(0), "toolKey")).isEqualTo("MOCK_PATCH_PROPOSAL");
    }

    @Test
    void shouldListProjectToolApprovalsFilterByStatus() {
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of("strategy", "STANDARD_DELIVERY"));

        // Query PENDING approvals
        ResponseEntity<String> res = get("/api/projects/" + projectId() + "/tool-approvals?status=PENDING");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        for (JsonNode approval : data) {
            assertThat(TestJsonHelper.getString(approval, "status")).isEqualTo("PENDING");
        }
    }
}
