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

class PatchProposalArtifactIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String taskIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;

        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-PPA-" + suffix,
                "description", "Patch proposal artifact integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Enable all 5 agents (architect, planner, backend, frontend, reviewer)
        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + projectIdValue + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + projectIdValue + "/tasks", Map.of(
                "title", "IT-PPA-Task-" + suffix,
                "description", "Patch proposal artifact test task",
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

    /**
     * Enable HIGH tool MOCK_PATCH_PROPOSAL (910006), start a REVIEW_ONLY run,
     * and return [executionId, runId] for the WAITING_APPROVAL execution.
     */
    private String[] enableHighAndGetWaitingExecId() {
        post("/api/projects/" + projectId() + "/tools/910006/enable", Map.of());
        ResponseEntity<String> res = post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        String execId = null;
        JsonNode toolExecs = data.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        return new String[]{execId, TestJsonHelper.getString(data, "id")};
    }

    /**
     * Approve a WAITING_APPROVAL execution and return the approve response.
     */
    private ResponseEntity<String> approveExecution(String execId) {
        return post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "批准"));
    }

    /**
     * Get the execution details.
     */
    private ResponseEntity<String> getExecution(String execId) {
        return get("/api/tool-sandbox-executions/" + execId);
    }

    /**
     * Get task artifacts.
     */
    private ResponseEntity<String> getTaskArtifacts(String tid) {
        return get("/api/tasks/" + tid + "/artifacts");
    }

    /**
     * Get task logs.
     */
    private ResponseEntity<String> getTaskLogs(String tid) {
        return get("/api/tasks/" + tid + "/logs");
    }

    // ========================
    // 1. Enum / type support
    // ========================

    @Test
    void shouldTaskArtifactTypeSupportPatchProposal() {
        // Enable HIGH tool and approve MOCK_PATCH_PROPOSAL
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        // Verify PATCH_PROPOSAL artifact exists
        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        boolean hasPatchProposal = false;
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                hasPatchProposal = true;
                break;
            }
        }
        assertThat(hasPatchProposal).isTrue();
    }

    // ========================
    // 2. Approve generates artifact
    // ========================

    @Test
    void shouldApproveMockPatchProposalGenerateArtifact() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();

        ResponseEntity<String> approveRes = approveExecution(execAndRun[0]);
        assertOk(approveRes);

        // Verify the approve response contains artifact info
        JsonNode data = TestJsonHelper.parse(approveRes.getBody()).get("data");
        String artifactId = TestJsonHelper.getString(data, "artifactId");
        assertThat(artifactId).isNotEmpty();
    }

    // ========================
    // 3. artifactType = PATCH_PROPOSAL
    // ========================

    @Test
    void shouldArtifactTypeBePatchProposal() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                assertThat(TestJsonHelper.getString(art, "artifactType")).isEqualTo("PATCH_PROPOSAL");
                return;
            }
        }
        // Should have found at least one
        assertThat(false).isTrue();
    }

    // ========================
    // 4. artifact name
    // ========================

    @Test
    void shouldArtifactNameContainMockPatchProposal() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                assertThat(TestJsonHelper.getString(art, "name")).contains("Mock Patch Proposal");
                return;
            }
        }
        assertThat(false).isTrue();
    }

    // ========================
    // 5. artifact content safety notice
    // ========================

    @Test
    void shouldArtifactContentContainSafetyNotice() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                String content = TestJsonHelper.getString(art, "content");
                assertThat(content).contains("仅用于审阅");
                return;
            }
        }
        assertThat(false).isTrue();
    }

    // ========================
    // 6. artifact content diff block
    // ========================

    @Test
    void shouldArtifactContentContainDiffBlock() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                String content = TestJsonHelper.getString(art, "content");
                assertThat(content).contains("```diff");
                return;
            }
        }
        assertThat(false).isTrue();
    }

    // ========================
    // 7. artifact content applied false
    // ========================

    @Test
    void shouldArtifactContentContainAppliedFalse() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                String content = TestJsonHelper.getString(art, "content");
                assertThat(content).contains("applied: false");
                return;
            }
        }
        assertThat(false).isTrue();
    }

    // ========================
    // 8. artifact content no local paths
    // ========================

    @Test
    void shouldArtifactContentNotContainLocalPaths() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                String content = TestJsonHelper.getString(art, "content");
                assertThat(content).doesNotContain("/Users/");
                assertThat(content).doesNotContain("/home/");
                assertThat(content).doesNotContain("C:\\");
                assertThat(content).doesNotContain("D:\\");
                return;
            }
        }
        assertThat(false).isTrue();
    }

    // ========================
    // 9. execution artifactId populated
    // ========================

    @Test
    void shouldExecutionArtifactIdPopulated() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> getRes = getExecution(execAndRun[0]);
        assertOk(getRes);
        JsonNode data = TestJsonHelper.parse(getRes.getBody()).get("data");
        String artifactId = TestJsonHelper.getString(data, "artifactId");
        assertThat(artifactId).isNotEmpty();
    }

    // ========================
    // 10. outputPayload contains artifactId
    // ========================

    @Test
    void shouldOutputPayloadContainArtifactId() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> getRes = getExecution(execAndRun[0]);
        assertOk(getRes);
        JsonNode data = TestJsonHelper.parse(getRes.getBody()).get("data");
        String outputPayload = TestJsonHelper.getString(data, "outputPayload");
        String artifactId = TestJsonHelper.getString(data, "artifactId");
        assertThat(outputPayload).contains("\"artifactId\":\"" + artifactId + "\"");
    }

    // ========================
    // 11. task logs contain PATCH_PROPOSAL_CREATED
    // ========================

    @Test
    void shouldTaskLogsContainPatchProposalCreated() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        ResponseEntity<String> logRes = getTaskLogs(taskId());
        assertOk(logRes);
        JsonNode logs = TestJsonHelper.parse(logRes.getBody()).get("data");
        boolean hasPatchCreated = false;
        for (JsonNode log : logs) {
            if ("PATCH_PROPOSAL_CREATED".equals(TestJsonHelper.getString(log, "stage"))) {
                hasPatchCreated = true;
                break;
            }
        }
        assertThat(hasPatchCreated).isTrue();
    }

    // ========================
    // 12. reject does NOT generate artifact
    // ========================

    @Test
    void shouldRejectNotGenerateArtifact() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();

        // Reject instead of approve
        ResponseEntity<String> rejectRes = post(
                "/api/tool-sandbox-executions/" + execAndRun[0] + "/reject",
                Map.of("comment", "不允许执行"));
        assertOk(rejectRes);

        // Verify the execution is REJECTED and has no artifactId
        ResponseEntity<String> getRes = getExecution(execAndRun[0]);
        assertOk(getRes);
        JsonNode data = TestJsonHelper.parse(getRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("REJECTED");
        assertThat(TestJsonHelper.getString(data, "artifactId")).isEmpty();
    }

    // ========================
    // 13. non-MOCK_PATCH_PROPOSAL tool does NOT generate PATCH_PROPOSAL
    // ========================

    @Test
    void shouldNonMockPatchProposalNotGeneratePatchProposal() {
        // Create a fresh project/task WITHOUT enabling the HIGH tool
        // so CODE_REVIEW uses the default MOCK_SECURITY_REVIEW instead of MOCK_PATCH_PROPOSAL
        String suffix = String.valueOf(System.currentTimeMillis()) + "-NMP";
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-NMP-" + suffix,
                "description", "Non-MOCK_PATCH_PROPOSAL test",
                "techStack", List.of("Java")
        ));
        assertOk(prjRes);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");

        // Enable only architect (300001) and review (300005) agents
        for (long agentId : new long[]{300001L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-NMP-Task-" + suffix,
                "description", "Non-MOCK_PATCH_PROPOSAL test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        // Start REVIEW_ONLY run without HIGH tool — should complete directly
        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"));
        assertOk(runRes);
        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");

        // REVIEW_ONLY without HIGH tool should complete immediately
        assertThat(TestJsonHelper.getString(runData, "status")).isEqualTo("COMPLETED");

        // Verify no PATCH_PROPOSAL artifact exists in task artifacts
        ResponseEntity<String> artRes = getTaskArtifacts(tid);
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        for (JsonNode art : artifacts) {
            assertThat(TestJsonHelper.getString(art, "artifactType")).isNotEqualTo("PATCH_PROPOSAL");
        }
    }

    // ========================
    // 14. duplicate approve does NOT generate duplicate artifact
    // ========================

    @Test
    void shouldDuplicateApproveNotGenerateDuplicateArtifact() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        String execId = execAndRun[0];
        assertThat(execId).isNotNull();

        // First approve should succeed and produce an artifactId
        ResponseEntity<String> firstRes = approveExecution(execId);
        assertOk(firstRes);
        JsonNode firstData = TestJsonHelper.parse(firstRes.getBody()).get("data");
        String artifactIdAfterFirst = TestJsonHelper.getString(firstData, "artifactId");
        assertThat(artifactIdAfterFirst).isNotEmpty();

        // Second approve should fail with CONFLICT
        ResponseEntity<String> secondRes = post(
                "/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "第二次批准"));
        assertThat(secondRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Verify the execution still has the same artifactId (no duplicate)
        ResponseEntity<String> getRes = getExecution(execId);
        assertOk(getRes);
        JsonNode getData = TestJsonHelper.parse(getRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(getData, "artifactId")).isEqualTo(artifactIdAfterFirst);
    }

    // ========================
    // 15. GET /api/tasks/{taskId}/artifacts returns PATCH_PROPOSAL
    // ========================

    @Test
    void shouldGetArtifactsReturnPatchProposal() {
        String[] execAndRun = enableHighAndGetWaitingExecId();
        assertThat(execAndRun[0]).isNotNull();
        approveExecution(execAndRun[0]);

        // GET /api/tasks/{taskId}/artifacts should include the PATCH_PROPOSAL
        ResponseEntity<String> artRes = getTaskArtifacts(taskId());
        assertOk(artRes);
        JsonNode body = TestJsonHelper.parse(artRes.getBody());
        assertThat(TestJsonHelper.getString(body, "code")).isEqualTo("OK");
        JsonNode artifacts = body.get("data");
        assertThat(artifacts.isArray()).isTrue();

        boolean hasPatchProposal = false;
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                hasPatchProposal = true;
                assertThat(TestJsonHelper.getString(art, "id")).isNotEmpty();
                assertThat(TestJsonHelper.getString(art, "name")).isNotEmpty();
                assertThat(TestJsonHelper.getString(art, "content")).isNotEmpty();
                break;
            }
        }
        assertThat(hasPatchProposal).isTrue();
    }
}
