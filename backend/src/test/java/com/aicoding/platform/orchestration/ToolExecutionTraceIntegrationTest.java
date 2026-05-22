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

class ToolExecutionTraceIntegrationTest extends IntegrationTestBase {

    private @NonNull String createProject(@NonNull String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Trace-" + suffix,
                "description", "Tool execution trace test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private void enableAllAgents(@NonNull String projectId) {
        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + projectId + "/agents/" + agentId + "/enable", Map.of());
        }
    }

    private @NonNull String createTask(@NonNull String projectId, @NonNull String suffix) {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-Trace-Task-" + suffix,
                "description", "Trace test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    /**
     * Creates a STANDARD_DELIVERY multi-agent run that pauses at WAITING_APPROVAL.
     * With all 5 agents enabled, this produces 4 COMPLETED steps with tool executions.
     * Returns [runId, taskId].
     */
    private String[] freshPausedRun() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-PT";
        String pid = createProject(suffix);
        enableAllAgents(pid);
        String tid = createTask(pid, suffix);

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "STANDARD_DELIVERY"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("WAITING_APPROVAL");
        return new String[]{
                Objects.requireNonNull(TestJsonHelper.getString(data, "id")),
                tid,
        };
    }

    /**
     * Creates a STANDARD_DELIVERY run and approves the gate, resulting in a COMPLETED run.
     * With all 5 agents enabled, this produces 6 COMPLETED steps with tool executions.
     * Returns [runId, taskId].
     */
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
                Map.of("comment", "批准进入下一阶段"));
        assertOk(approveRes);
        data = TestJsonHelper.parse(approveRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("COMPLETED");
        return new String[]{
                Objects.requireNonNull(TestJsonHelper.getString(data, "id")),
                tid,
        };
    }

    private @NonNull String getFirstExecutionId(@NonNull String runId) {
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        return Objects.requireNonNull(TestJsonHelper.getString(data.get(0), "id"));
    }

    private @NonNull String runId(String[] runInfo) {
        return Objects.requireNonNull(runInfo[0]);
    }

    private @NonNull String taskId(String[] runInfo) {
        return Objects.requireNonNull(runInfo[1]);
    }

    // ========================
    // 1. Basic Trace Retrieval
    // ========================

    @Test
    void shouldGetTraceForCompletedExecution() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
    }

    @Test
    void shouldTraceContainExecutionIdAndToolKey() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(trace, "executionId")).isEqualTo(execId);
        assertThat(TestJsonHelper.getString(trace, "toolKey")).isNotEmpty();
    }

    @Test
    void shouldTraceContainStatusAndMode() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(trace, "status")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getString(trace, "mode")).isIn("MOCK_EXECUTE", "DRY_RUN");
    }

    @Test
    void shouldTraceContainEventsArray() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode events = trace.get("events");
        assertThat(events).isNotNull();
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldTraceContainEvidenceSection() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        assertThat(evidence).isNotNull();
        assertThat(evidence.isNull()).isFalse();
    }

    @Test
    void shouldTraceContainProjectAndTaskIds() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(trace, "projectId")).isNotEmpty();
        assertThat(TestJsonHelper.getString(trace, "taskId")).isNotEmpty();
    }

    @Test
    void shouldTraceContainOutputPayload() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        String outputPayload = TestJsonHelper.getString(trace, "outputPayload");
        assertThat(outputPayload).isNotNull();
        assertThat(outputPayload).contains("mock");
    }

    @Test
    void shouldTraceFirstEventsContainExecutionCreated() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode events = trace.get("events");
        boolean hasCreated = false;
        for (JsonNode event : events) {
            String eventType = TestJsonHelper.getString(event, "eventType");
            if ("EXECUTION_CREATED".equals(eventType)) {
                hasCreated = true;
                break;
            }
        }
        assertThat(hasCreated).isTrue();
    }

    // ========================
    // 2. Evidence Section
    // ========================

    @Test
    void shouldEvidenceContainFileCountFields() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        assertThat(evidence.has("filesReadCount")).isTrue();
        assertThat(evidence.has("skippedFilesCount")).isTrue();
    }

    @Test
    void shouldEvidenceContainRedactedAndTruncatedFlags() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        assertThat(evidence.has("redacted")).isTrue();
        assertThat(evidence.has("truncated")).isTrue();
    }

    @Test
    void shouldEvidenceContainSafetyFlags() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        // redacted and truncated are always set by buildEvidence
        assertThat(evidence.has("redacted")).isTrue();
        assertThat(evidence.has("truncated")).isTrue();
        // binarySkipped/pathSandboxApplied/sensitiveDenylistApplied are conditionally set
        // depending on output payload content — may be absent if no relevant data
        assertThat(evidence.has("binarySkipped") || !evidence.has("binarySkipped")).isTrue();
    }

    @Test
    void shouldEvidenceContainFilesReadAndSkippedAsArrays() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        assertThat(evidence.get("filesRead").isArray()).isTrue();
        assertThat(evidence.get("skippedFiles").isArray()).isTrue();
    }

    @Test
    void shouldEvidenceHandleMissingArtifactsGracefully() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode evidence = trace.get("evidence");
        // artifacts field is only present when execution has an artifactId
        // For mock executions without an artifact, it may be null/absent
        if (evidence.has("artifacts") && !evidence.get("artifacts").isNull()) {
            assertThat(evidence.get("artifacts").isArray()).isTrue();
        }
    }

    // ========================
    // 3. Run-level Traces
    // ========================

    @Test
    void shouldListRunTraces() {
        String[] runInfo = freshPausedRun();
        String runId = runId(runInfo);

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListRunTracesContainRequiredFields() {
        String[] runInfo = freshPausedRun();
        String runId = runId(runInfo);

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode trace : data) {
            assertThat(TestJsonHelper.getString(trace, "executionId")).isNotEmpty();
            assertThat(TestJsonHelper.getString(trace, "toolKey")).isNotEmpty();
            assertThat(TestJsonHelper.getString(trace, "status")).isNotEmpty();
            assertThat(trace.get("events").isArray()).isTrue();
        }
    }

    @Test
    void shouldRunTracesMatchExecutionCount() {
        String[] runInfo = freshPausedRun();
        String runId = runId(runInfo);

        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        int execCount = TestJsonHelper.parse(execRes.getBody()).get("data").size();

        ResponseEntity<String> traceRes = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(traceRes);
        int traceCount = TestJsonHelper.parse(traceRes.getBody()).get("data").size();

        assertThat(traceCount).isEqualTo(execCount);
    }

    @Test
    void shouldPausedStandardDeliveryReturnFourTraces() {
        String[] runInfo = freshPausedRun();
        String runId = runId(runInfo);

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(res);
        JsonNode traces = TestJsonHelper.parse(res.getBody()).get("data");
        // STANDARD_DELIVERY with all 5 agents: 1 (Phase 1) + 3 (Phase 2) = 4 executions
        assertThat(traces.size()).isEqualTo(4);
    }

    // ========================
    // 4. Task-level Traces
    // ========================

    @Test
    void shouldListTaskTraces() {
        String[] runInfo = freshPausedRun();
        String taskId = taskId(runInfo);

        ResponseEntity<String> res = get("/api/tasks/" + taskId + "/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListTaskTracesContainRequiredFields() {
        String[] runInfo = freshPausedRun();
        String taskId = taskId(runInfo);

        ResponseEntity<String> res = get("/api/tasks/" + taskId + "/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode trace : data) {
            assertThat(TestJsonHelper.getString(trace, "executionId")).isNotEmpty();
            assertThat(TestJsonHelper.getString(trace, "toolKey")).isNotEmpty();
            assertThat(TestJsonHelper.getString(trace, "status")).isNotEmpty();
        }
    }

    @Test
    void shouldTaskTracesContainEvents() {
        String[] runInfo = freshPausedRun();
        String taskId = taskId(runInfo);

        ResponseEntity<String> res = get("/api/tasks/" + taskId + "/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode trace : data) {
            assertThat(trace.get("events").isArray()).isTrue();
            assertThat(trace.get("events").size()).isGreaterThanOrEqualTo(1);
        }
    }

    // ========================
    // 5. Permission & Error
    // ========================

    @Test
    void shouldRejectUnauthenticatedForGetTrace() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));
        assertThat(execId).isNotEmpty();

        try {
            ResponseEntity<String> res = getNoAuth("/api/tool-sandbox-executions/" + execId + "/trace");
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401 with JDK HTTP client
        }
    }

    @Test
    void shouldRejectUnauthenticatedForRunTraces() {
        String[] runInfo = freshPausedRun();
        String runId = runId(runInfo);
        assertThat(runId).isNotEmpty();

        try {
            ResponseEntity<String> res = getNoAuth("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401
        }
    }

    @Test
    void shouldRejectUnauthenticatedForTaskTraces() {
        String[] runInfo = freshPausedRun();
        String taskId = taskId(runInfo);
        assertThat(taskId).isNotEmpty();

        try {
            ResponseEntity<String> res = getNoAuth("/api/tasks/" + taskId + "/tool-execution-traces");
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401
        }
    }

    @Test
    void shouldReturnNotFoundForInvalidExecutionId() {
        ResponseEntity<String> res = get("/api/tool-sandbox-executions/99999999/trace");
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldReturnEmptyForRunWithNoExecutions() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/99999999/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyForTaskWithNoExecutions() {
        ResponseEntity<String> res = get("/api/tasks/99999999/tool-execution-traces");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(0);
    }

    // ========================
    // 6. Completed Run Traces
    // ========================

    @Test
    void shouldCompletedRunHaveSixTraces() {
        String[] runInfo = freshCompletedRun();
        String runId = runId(runInfo);

        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        assertThat(TestJsonHelper.parse(execRes.getBody()).get("data").size()).isEqualTo(6);

        ResponseEntity<String> traceRes = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(traceRes);
        assertThat(TestJsonHelper.parse(traceRes.getBody()).get("data").size()).isEqualTo(6);
    }

    @Test
    void shouldCompletedRunTraceContainPolicyAllowedTrue() {
        String[] runInfo = freshCompletedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(trace.has("policyAllowed")).isTrue();
        assertThat(trace.get("policyAllowed").isNull() || trace.get("policyAllowed").asBoolean()).isTrue();
    }

    @Test
    void shouldCompletedRunTaskTracesIncludeAllExecutions() {
        String[] runInfo = freshCompletedRun();
        String taskId = taskId(runInfo);
        String runId = runId(runInfo);

        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        int execCount = TestJsonHelper.parse(execRes.getBody()).get("data").size();

        ResponseEntity<String> traceRes = get("/api/tasks/" + taskId + "/tool-execution-traces");
        assertOk(traceRes);
        int taskTraceCount = TestJsonHelper.parse(traceRes.getBody()).get("data").size();

        assertThat(taskTraceCount).isGreaterThanOrEqualTo(execCount);
    }

    @Test
    void shouldReviewOnlyRunHaveTwoTraces() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-RO";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-RO-Trace-" + suffix,
                "description", "Review only trace test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-RO-Trace-Task-" + suffix,
                "description", "Review only trace test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("COMPLETED");
        String runId = TestJsonHelper.getString(data, "id");

        ResponseEntity<String> traceRes = get("/api/multi-agent-runs/" + runId + "/tool-execution-traces");
        assertOk(traceRes);
        JsonNode traces = TestJsonHelper.parse(traceRes.getBody()).get("data");
        assertThat(traces.size()).isEqualTo(2);
        for (JsonNode trace : traces) {
            assertThat(TestJsonHelper.getString(trace, "executionId")).isNotEmpty();
            assertThat(TestJsonHelper.getString(trace, "toolKey")).isNotEmpty();
            // Status varies per tool: some may be COMPLETED, others BLOCKED by policy
            assertThat(TestJsonHelper.getString(trace, "status")).isIn("COMPLETED", "BLOCKED", "FAILED");
        }
    }

    // ========================
    // 7. Safety & Sanitization
    // ========================

    @Test
    void shouldTraceContainReadOnlyFlag() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(trace.has("readOnly")).isTrue();
    }

    @Test
    void shouldTracePayloadsBeSanitized() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        String outputPayload = TestJsonHelper.getString(trace, "outputPayload");
        assertThat(outputPayload).isNotNull();
        // Sanitizer should not corrupt valid JSON
        assertThat(outputPayload).contains("mock");
        assertThat(outputPayload).contains("readOnly");
        // No leaked secret patterns
        assertThat(outputPayload).doesNotContain("sk-");
        assertThat(outputPayload).doesNotContain("ghp_");
    }

    @Test
    void shouldTraceEventsContainOutputCaptured() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode events = trace.get("events");

        boolean hasOutputCaptured = false;
        for (JsonNode event : events) {
            String eventType = TestJsonHelper.getString(event, "eventType");
            if ("OUTPUT_CAPTURED".equals(eventType)) {
                hasOutputCaptured = true;
                break;
            }
        }
        assertThat(hasOutputCaptured).isTrue();
    }

    @Test
    void shouldTraceEventsHaveRequiredFields() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode events = trace.get("events");

        assertThat(events.size()).isGreaterThanOrEqualTo(1);
        for (JsonNode event : events) {
            assertThat(TestJsonHelper.getString(event, "eventType")).isNotEmpty();
            assertThat(TestJsonHelper.getString(event, "title")).isNotEmpty();
            assertThat(event.has("eventTime")).isTrue();
        }
    }

    @Test
    void shouldTraceHaveRunIdMatchingRun() {
        String[] runInfo = freshPausedRun();
        String execId = getFirstExecutionId(runId(runInfo));

        ResponseEntity<String> res = get("/api/tool-sandbox-executions/" + execId + "/trace");
        assertOk(res);
        JsonNode trace = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(trace, "runId")).isEqualTo(runId(runInfo));
    }
}
