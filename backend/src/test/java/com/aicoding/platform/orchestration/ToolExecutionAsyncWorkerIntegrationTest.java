package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.domain.ToolExecutionStatus;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolSandboxExecutionMapper;
import com.aicoding.platform.orchestration.worker.ToolExecutionJobMessage;
import com.aicoding.platform.orchestration.worker.ToolExecutionJobPublisher;
import com.aicoding.platform.orchestration.worker.ToolExecutionWorkerService;
import com.aicoding.platform.orchestration.worker.ToolWorkerProperties;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolExecutionAsyncWorkerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ToolWorkerProperties toolWorkerProperties;

    @Autowired
    private ToolExecutionWorkerService toolExecutionWorkerService;

    @Autowired
    private ToolExecutionJobMapper toolExecutionJobMapper;

    @Autowired
    private ToolSandboxExecutionMapper toolSandboxExecutionMapper;

    @Autowired(required = false)
    private ToolExecutionJobPublisher toolExecutionJobPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================
    // 1-3: Worker Configuration
    // ========================

    @Test
    void shouldLoadWorkerPropertiesFromTestConfig() {
        assertThat(toolWorkerProperties.getMode()).isEqualTo("SYNC_MOCK");
        assertThat(toolWorkerProperties.isQueueEnabled()).isFalse();
        assertThat(toolWorkerProperties.isWorkerEnabled()).isFalse();
        assertThat(toolWorkerProperties.getExchange()).isEqualTo("tool.execution.exchange");
        assertThat(toolWorkerProperties.getQueue()).isEqualTo("tool.execution.queue");
        assertThat(toolWorkerProperties.getRoutingKey()).isEqualTo("tool.execution.run");
        assertThat(toolWorkerProperties.getMaxRetryCount()).isEqualTo(2);
        assertThat(toolWorkerProperties.getPollIntervalMs()).isEqualTo(1500);
    }

    @Test
    void shouldSyncModeReturnFalseForIsAsyncMode() {
        assertThat(toolWorkerProperties.isAsyncMode()).isFalse();
    }

    @Test
    void shouldAsyncModeReturnTrueWhenModeIsAsyncRabbitmq() {
        toolWorkerProperties.setMode("ASYNC_RABBITMQ");
        assertThat(toolWorkerProperties.isAsyncMode()).isTrue();
        // Reset for other tests
        toolWorkerProperties.setMode("SYNC_MOCK");
    }

    // ========================
    // 4-7: WorkerService.process()
    // ========================

    @Test
    void shouldProcessPendingJobToCompleted() {
        // Arrange: create project and task, get a tool execution with a PENDING job
        String[] pt = createProjectAndTask("wproc-" + System.currentTimeMillis());
        String pid = pt[0], tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        String execId = null;
        String toolKey = null;
        for (JsonNode te : toolExecs) {
            execId = TestJsonHelper.getString(te, "id");
            toolKey = TestJsonHelper.getString(te, "toolName");
            if (execId != null && !execId.isEmpty()) break;
        }
        assertThat(execId).isNotNull();

        // Insert a PENDING job
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pid));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        // Act: process via worker service
        toolExecutionWorkerService.process(job.getId());

        // Assert: job should be COMPLETED
        ToolExecutionJobEntity updated = toolExecutionJobMapper.selectById(job.getId());
        assertThat(updated.getStatus()).isEqualTo(ToolExecutionJobStatus.COMPLETED.name());
        assertThat(updated.getResultPayload()).isNotEmpty();
        assertThat(updated.getStartedAt()).isNotNull();
        assertThat(updated.getFinishedAt()).isNotNull();
        assertThat(updated.getDurationMs()).isGreaterThan(0);
    }

    @Test
    void shouldWorkerServiceSkipNonPendingJob() {
        String[] pt = createProjectAndTask("wskp-" + System.currentTimeMillis());
        String pid = pt[0], tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "id");
        String toolKey = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "toolName");

        // Insert a RUNNING job
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pid));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.RUNNING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        // Process - should skip since status is RUNNING not PENDING
        toolExecutionWorkerService.process(job.getId());

        // Status should remain RUNNING
        ToolExecutionJobEntity updated = toolExecutionJobMapper.selectById(job.getId());
        assertThat(updated.getStatus()).isEqualTo(ToolExecutionJobStatus.RUNNING.name());
    }

    @Test
    void shouldWorkerServiceHandleMissingExecutionGracefully() {
        // Create a job referencing a non-existent execution
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(99999L);
        job.setTaskId(99999L);
        job.setToolExecutionId(999999L);
        job.setToolKey("PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        // Process - should handle missing execution gracefully
        toolExecutionWorkerService.process(job.getId());

        // Job should be RETRY_PENDING (enters retry backoff flow instead of immediate FAILED)
        ToolExecutionJobEntity updated = toolExecutionJobMapper.selectById(job.getId());
        assertThat(updated.getStatus()).isEqualTo(ToolExecutionJobStatus.RETRY_PENDING.name());
        assertThat(updated.getLastError()).isNotEmpty();
    }

    @Test
    void shouldWorkerServiceHandleNonexistentJobId() {
        // Processing a non-existent job should not throw
        toolExecutionWorkerService.process(999999999L);
        // No exception = success
    }

    // ========================
    // 8-10: Job Result Payload
    // ========================

    @Test
    void shouldProcessedJobContainMockTrueInResult() {
        String[] pt = createProjectAndTask("jrm-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "id");
        String toolKey = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "toolName");

        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pt[0]));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        toolExecutionWorkerService.process(job.getId());

        ToolExecutionJobEntity updated = toolExecutionJobMapper.selectById(job.getId());
        assertThat(updated.getResultPayload()).contains("\"mock\":true");
        assertThat(updated.getResultPayload()).contains("\"jobCompleted\":true");
    }

    @Test
    void shouldProcessedJobUpdateExecutionToCompleted() {
        String[] pt = createProjectAndTask("juec-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "id");
        String toolKey = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "toolName");

        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pt[0]));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        toolExecutionWorkerService.process(job.getId());

        // Verify the execution is updated
        var execution = toolSandboxExecutionMapper.selectById(Long.valueOf(execId));
        assertThat(execution).isNotNull();
        assertThat(execution.getStatus()).isEqualTo(ToolExecutionStatus.COMPLETED.name());
        assertThat(execution.getOutputPayload()).isNotEmpty();
        assertThat(execution.getSummary()).isNotEmpty();
    }

    @Test
    void shouldProcessedJobCreateTaskLogs() {
        String[] pt = createProjectAndTask("wtl-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "id");
        String toolKey = TestJsonHelper.getString(runData.get("toolExecutions").get(0), "toolName");

        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pt[0]));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(job);

        toolExecutionWorkerService.process(job.getId());

        // Check task logs
        ResponseEntity<String> logsRes = get("/api/tasks/" + tid + "/logs");
        assertOk(logsRes);
        JsonNode logs = TestJsonHelper.parse(logsRes.getBody()).get("data");
        boolean hasRunning = false, hasCompleted = false;
        for (JsonNode log : logs) {
            String stage = TestJsonHelper.getString(log, "stage");
            if ("TOOL_JOB_RUNNING".equals(stage)) hasRunning = true;
            if ("TOOL_JOB_COMPLETED".equals(stage)) hasCompleted = true;
        }
        assertThat(hasRunning).isTrue();
        assertThat(hasCompleted).isTrue();
    }

    // ========================
    // 11-14: Job Message
    // ========================

    @Test
    void shouldBuildJobMessageWithAllFields() {
        ToolExecutionJobMessage msg = new ToolExecutionJobMessage(
                "1001", "2001", "3001", "4001",
                "5001", "6001", "PROJECT_CONTEXT_SCAN",
                "2026-05-21T10:00:00");

        assertThat(msg.getJobId()).isEqualTo("1001");
        assertThat(msg.getToolExecutionId()).isEqualTo("2001");
        assertThat(msg.getProjectId()).isEqualTo("3001");
        assertThat(msg.getTaskId()).isEqualTo("4001");
        assertThat(msg.getRunId()).isEqualTo("5001");
        assertThat(msg.getStepId()).isEqualTo("6001");
        assertThat(msg.getToolKey()).isEqualTo("PROJECT_CONTEXT_SCAN");
        assertThat(msg.getRequestedAt()).isEqualTo("2026-05-21T10:00:00");
    }

    @Test
    void shouldSerializeAndDeserializeJobMessage() throws Exception {
        ToolExecutionJobMessage original = new ToolExecutionJobMessage(
                "1001", "2001", "3001", "4001",
                "5001", "6001", "PROJECT_CONTEXT_SCAN",
                "2026-05-21T10:00:00");

        // Java serialization
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
        }
        byte[] bytes = bos.toByteArray();

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ToolExecutionJobMessage deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            deserialized = (ToolExecutionJobMessage) ois.readObject();
        }

        assertThat(deserialized.getJobId()).isEqualTo(original.getJobId());
        assertThat(deserialized.getToolExecutionId()).isEqualTo(original.getToolExecutionId());
        assertThat(deserialized.getProjectId()).isEqualTo(original.getProjectId());
        assertThat(deserialized.getTaskId()).isEqualTo(original.getTaskId());
        assertThat(deserialized.getRunId()).isEqualTo(original.getRunId());
        assertThat(deserialized.getStepId()).isEqualTo(original.getStepId());
        assertThat(deserialized.getToolKey()).isEqualTo(original.getToolKey());
        assertThat(deserialized.getRequestedAt()).isEqualTo(original.getRequestedAt());
    }

    @Test
    void shouldJobMessageJsonSerialize() throws Exception {
        ToolExecutionJobMessage msg = new ToolExecutionJobMessage(
                "1001", "2001", "3001", "4001",
                null, null, "READ_REPOSITORY_TREE",
                "2026-05-21T10:00:00");

        String json = objectMapper.writeValueAsString(msg);
        assertThat(json).contains("1001");
        assertThat(json).contains("READ_REPOSITORY_TREE");
        assertThat(json).contains("2001");

        ToolExecutionJobMessage parsed = objectMapper.readValue(json, ToolExecutionJobMessage.class);
        assertThat(parsed.getJobId()).isEqualTo("1001");
        assertThat(parsed.getToolKey()).isEqualTo("READ_REPOSITORY_TREE");
        assertThat(parsed.getRunId()).isNull();
        assertThat(parsed.getStepId()).isNull();
    }

    @Test
    void shouldPublisherNotFailWhenQueueDisabled() {
        // Publisher should skip when queue is disabled (test config has queue-enabled: false)
        // This test verifies the publisher doesn't throw when queue is disabled
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setId(999999L);
        job.setToolKey("TEST");

        // Should not throw
        toolExecutionJobPublisher.publish(job);
    }

    // ========================
    // 15-18: Job Execution Integration
    // ========================

    @Test
    void shouldCreateJobWithExecuteJobFlow() {
        String[] pt = createProjectAndTask("ejf-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        boolean hasJob = false;
        for (JsonNode te : toolExecs) {
            JsonNode job = te.get("job");
            if (job != null) {
                String status = TestJsonHelper.getString(job, "status");
                assertThat(status).isEqualTo("COMPLETED");
                hasJob = true;
                break;
            }
        }
        assertThat(hasJob).isTrue();
    }

    @Test
    void shouldApprovalFlowCreateJobViaWorkerService() {
        String[] pt = createProjectAndTask("awf-" + System.currentTimeMillis());
        String pid = pt[0], tid = pt[1];

        // Enable high-risk tool
        post("/api/projects/" + pid + "/tools/910006/enable",
                Map.of("parameters", Map.of(
                        "proposalScope", "MINIMAL",
                        "includeTests", false,
                        "maxChangedFiles", 1,
                        "targetArea", ""
                )));

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        // Find WAITING_APPROVAL execution
        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        String execId = null;
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        assertThat(execId).isNotNull();

        // Approve
        ResponseEntity<String> approveRes = post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "批准"));
        assertOk(approveRes);

        // After approval, job should be COMPLETED (SYNC_MOCK mode)
        JsonNode approvedExec = TestJsonHelper.parse(approveRes.getBody()).get("data");
        JsonNode job = approvedExec.get("job");
        assertThat(job).isNotNull();
        assertThat(TestJsonHelper.getString(job, "status")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getString(job, "resultPayload")).contains("mock");
    }

    @Test
    void shouldExecutionResponseContainJobFields() {
        String[] pt = createProjectAndTask("ejf2-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        boolean foundJobField = false;
        for (JsonNode te : toolExecs) {
            if (te.has("job") && te.get("job") != null && !te.get("job").isNull()) {
                foundJobField = true;
                JsonNode job = te.get("job");
                assertThat(job.has("status")).isTrue();
                assertThat(job.has("requestPayload")).isTrue();
                assertThat(job.has("resultPayload")).isTrue();
                break;
            }
        }
        assertThat(foundJobField).isTrue();
    }

    @Test
    void shouldWorkerServiceProcessDifferentToolTypes() {
        // Test that worker service handles MOCK_PATCH_PROPOSAL tool type
        String[] pt = createProjectAndTask("wdt-" + System.currentTimeMillis());
        String pid = pt[0], tid = pt[1];

        post("/api/projects/" + pid + "/tools/910006/enable",
                Map.of("parameters", Map.of(
                        "proposalScope", "MINIMAL",
                        "includeTests", false,
                        "maxChangedFiles", 1,
                        "targetArea", ""
                )));

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        String execId = null;
        for (JsonNode te : toolExecs) {
            if ("WAITING_APPROVAL".equals(TestJsonHelper.getString(te, "status"))) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        assertThat(execId).isNotNull();

        // Insert a PENDING job for the MOCK_PATCH_PROPOSAL execution
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(pid));
        job.setTaskId(Long.valueOf(tid));
        job.setToolExecutionId(Long.valueOf(execId));
        job.setToolKey("MOCK_PATCH_PROPOSAL");
        job.setStatus(ToolExecutionJobStatus.PENDING.name());
        job.setPriority("NORMAL");
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setRequestPayload("{\"test\":true,\"proposalScope\":\"MINIMAL\"}");
        toolExecutionJobMapper.insert(job);

        // Process via worker
        toolExecutionWorkerService.process(job.getId());

        // Job should be COMPLETED
        ToolExecutionJobEntity updated = toolExecutionJobMapper.selectById(job.getId());
        assertThat(updated.getStatus()).isEqualTo(ToolExecutionJobStatus.COMPLETED.name());

        // Verify execution was updated
        var execution = toolSandboxExecutionMapper.selectById(Long.valueOf(execId));
        assertThat(execution).isNotNull();
        assertThat(execution.getStatus()).isEqualTo(ToolExecutionStatus.COMPLETED.name());
    }

    // ========================
    // Helpers
    // ========================

    private String[] createProjectAndTask(String suffix) {
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-AW-" + suffix,
                "description", "Async worker test",
                "techStack", List.of("Java")
        ));
        assertOk(prjRes);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-AW-Task-" + suffix,
                "description", "Async worker test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
        return new String[]{pid, tid};
    }
}
