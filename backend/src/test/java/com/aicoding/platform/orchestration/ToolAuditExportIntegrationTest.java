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

class ToolAuditExportIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Export-" + suffix,
                "description", "Audit export test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private void enableAllAgents(String projectId) {
        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + projectId + "/agents/" + agentId + "/enable", Map.of());
        }
    }

    private String createTask(String projectId, String suffix) {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-Export-Task-" + suffix,
                "description", "Export test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String[] freshCompletedRun() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-CT";
        String pid = createProject(suffix);
        enableAllAgents(pid);
        String tid = createTask(pid, suffix);

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "STANDARD_DELIVERY"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");
        String runId = Objects.requireNonNull(TestJsonHelper.getString(data, "id"));
        String gateId = Objects.requireNonNull(TestJsonHelper.getString(data.get("pendingApprovalGate"), "id"));

        ResponseEntity<String> approveRes = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                Map.of("comment", "批准"));
        assertOk(approveRes);
        return new String[]{
                Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(approveRes.getBody()).get("data"), "id")),
                tid,
        };
    }

    private String getFirstExecutionId(String runId) {
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        return Objects.requireNonNull(TestJsonHelper.getString(data.get(0), "id"));
    }

    // ========================
    // Export Execution Trace
    // ========================

    @Test
    void shouldExportExecutionTrace() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = get("/api/orchestration/executions/" + executionId + "/audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "targetType")).isEqualTo("TOOL_EXECUTION");
        assertThat(TestJsonHelper.getString(data, "targetId")).isEqualTo(executionId);
        assertThat(TestJsonHelper.getString(data, "contentType")).isEqualTo("text/markdown");
        assertThat(TestJsonHelper.getString(data, "fileName")).contains("tool-execution-" + executionId);
        assertThat(TestJsonHelper.getString(data, "markdown")).isNotEmpty();
        assertThat(TestJsonHelper.getInt(data, "traceCount")).isEqualTo(1);
    }

    @Test
    void shouldExportExecutionTraceNotFound() {
        ResponseEntity<String> res = get("/api/orchestration/executions/99999999/audit-export");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldExportExecutionTraceResponseStructure() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = get("/api/orchestration/executions/" + executionId + "/audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("markdown")).isTrue();
        assertThat(data.has("generatedAt")).isTrue();
        assertThat(data.has("redacted")).isTrue();
        assertThat(data.has("truncated")).isTrue();
        assertThat(data.has("traceCount")).isTrue();
        assertThat(data.has("contentType")).isTrue();
        assertThat(data.has("fileName")).isTrue();
    }

    @Test
    void shouldExportExecutionTraceIncludesToolInfo() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = get("/api/orchestration/executions/" + executionId + "/audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String md = TestJsonHelper.getString(data, "markdown");
        assertThat(md).contains("Tool Execution Audit Report");
        assertThat(md).contains("Execution ID");
    }

    // ========================
    // Export Run Evidence
    // ========================

    @Test
    void shouldExportRunEvidence() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/runs/" + runInfo[0] + "/evidence-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "targetType")).isEqualTo("MULTI_AGENT_RUN");
        assertThat(TestJsonHelper.getString(data, "targetId")).isEqualTo(runInfo[0]);
        assertThat(TestJsonHelper.getString(data, "markdown")).isNotEmpty();
        assertThat(TestJsonHelper.getInt(data, "traceCount")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldExportRunEvidenceEmptyRun() {
        ResponseEntity<String> res = get("/api/orchestration/runs/99999999/evidence-export");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldExportRunEvidenceContainsRedactedFlag() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/runs/" + runInfo[0] + "/evidence-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("redacted")).isTrue();
        assertThat(data.has("truncated")).isTrue();
    }

    @Test
    void shouldExportRunEvidenceContainsTraceCount() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/runs/" + runInfo[0] + "/evidence-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        int traceCount = TestJsonHelper.getInt(data, "traceCount");
        assertThat(traceCount).as("Run should have at least 1 trace").isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldExportRunEvidenceMarkdownContainsTraces() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/runs/" + runInfo[0] + "/evidence-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String md = TestJsonHelper.getString(data, "markdown");
        assertThat(md).contains("Multi-Agent Run Tool Evidence Report");
        assertThat(md).contains("Trace 1");
    }

    // ========================
    // Export Task Tool Audit
    // ========================

    @Test
    void shouldExportTaskToolAudit() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/tasks/" + runInfo[1] + "/tool-audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "targetType")).isEqualTo("TASK");
        assertThat(TestJsonHelper.getString(data, "targetId")).isEqualTo(runInfo[1]);
        assertThat(TestJsonHelper.getString(data, "markdown")).isNotEmpty();
        assertThat(TestJsonHelper.getInt(data, "traceCount")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldExportTaskToolAuditEmptyTask() {
        ResponseEntity<String> res = get("/api/orchestration/tasks/99999999/tool-audit-export");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldExportTaskToolAuditContainsSummary() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/tasks/" + runInfo[1] + "/tool-audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String md = TestJsonHelper.getString(data, "markdown");
        assertThat(md).contains("Task Tool Audit Report");
        assertThat(md).contains("Task Summary");
    }

    @Test
    void shouldExportTaskToolAuditCorrectTraceCount() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = get("/api/orchestration/tasks/" + runInfo[1] + "/tool-audit-export");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        int traceCount = TestJsonHelper.getInt(data, "traceCount");
        assertThat(traceCount).as("Task should have at least 1 trace").isGreaterThanOrEqualTo(1);
    }

    // ========================
    // Export Permission Check
    // ========================

    @Test
    void shouldExportExecutionTraceUnauthorizedWithoutAuth() {
        ResponseEntity<String> res = getNoAuth("/api/orchestration/executions/1/audit-export");
        // Should return 401 or similar
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
    }
}
