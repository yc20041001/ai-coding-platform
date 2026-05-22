package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolExecutionJobIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ToolExecutionJobMapper toolExecutionJobMapper;

		private String[] createFreshProjectAndTask(String suffix) {
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-JOB-" + suffix,
                "description", "Job test",
                "techStack", List.of("Java")
        ));
        assertOk(prjRes);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-JOB-Task-" + suffix,
                "description", "Test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
        return new String[]{pid, tid};
    }

    // ========================
    // 1-7: Job Creation / Drain
    // ========================

    @Test
    @Order(1)
    void shouldCreateJobOnToolExecution() {
        String[] pt = createFreshProjectAndTask("jc-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        assertThat(toolExecs).isNotNull();

        boolean hasJobId = false;
        for (JsonNode te : toolExecs) {
            String jobId = TestJsonHelper.getString(te, "jobId");
            if (jobId != null && !jobId.isEmpty()) {
                hasJobId = true;
                break;
            }
        }
        assertThat(hasJobId).isTrue();
    }

    @Test
    @Order(2)
    void shouldJobStatusCompleted() {
        String[] pt = createFreshProjectAndTask("jsc-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            JsonNode job = te.get("job");
            if (job != null) {
                String status = TestJsonHelper.getString(job, "status");
                assertThat(status).isEqualTo("COMPLETED");
                return;
            }
        }
        // Should have found at least one job
        assertThat(false).isTrue();
    }

    @Test
    @Order(3)
    void shouldExecutionResponseContainJobId() {
        String[] pt = createFreshProjectAndTask("ejid-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        assertThat(toolExecs).isNotNull();

        boolean found = false;
        for (JsonNode te : toolExecs) {
            String jobId = TestJsonHelper.getString(te, "jobId");
            if (jobId != null && !jobId.isEmpty()) {
                assertThat(te.get("job")).isNotNull();
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @Order(4)
    void shouldAllCompletedExecutionsHaveJobs() {
        String[] pt = createFreshProjectAndTask("allj-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            String status = TestJsonHelper.getString(te, "status");
            if ("COMPLETED".equals(status)) {
                String jobId = TestJsonHelper.getString(te, "jobId");
                assertThat(jobId).isNotEmpty();
            }
        }
    }

    @Test
    @Order(5)
    void shouldJobRequestPayloadContainParameters() {
        String[] pt = createFreshProjectAndTask("jrp-" + System.currentTimeMillis());
        String pid = pt[0], tid = pt[1];

        post("/api/projects/" + pid + "/tools/910001/enable",
                Map.of("parameters", Map.of("scope", "TASK", "includeMetadata", true)));

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY", "instruction", "test job params"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            JsonNode job = te.get("job");
            if (job != null) {
                String requestPayload = TestJsonHelper.getString(job, "requestPayload");
                if (requestPayload != null && !requestPayload.isEmpty()) {
                    assertThat(requestPayload).contains("toolKey");
                    assertThat(requestPayload).contains("parameters");
                    return;
                }
            }
        }
    }

    @Test
    @Order(6)
    void shouldJobResultPayloadContainMockTrue() {
        String[] pt = createFreshProjectAndTask("jres-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            JsonNode job = te.get("job");
            if (job != null) {
                String resultPayload = TestJsonHelper.getString(job, "resultPayload");
                if (resultPayload != null && !resultPayload.isEmpty()) {
                    assertThat(resultPayload).contains("\"mock\":true");
                    return;
                }
            }
        }
    }

    @Test
    @Order(7)
    void shouldTaskLogsContainJobStages() {
        String[] pt = createFreshProjectAndTask("tlog-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        // Check task logs
        ResponseEntity<String> logsRes = get("/api/tasks/" + tid + "/logs");
        assertOk(logsRes);
        JsonNode logs = TestJsonHelper.parse(logsRes.getBody()).get("data");
        boolean hasCreated = false, hasCompleted = false;
        for (JsonNode log : logs) {
            String stage = TestJsonHelper.getString(log, "stage");
            if ("TOOL_JOB_CREATED".equals(stage)) hasCreated = true;
            if ("TOOL_JOB_COMPLETED".equals(stage)) hasCompleted = true;
        }
        assertThat(hasCreated).isTrue();
        assertThat(hasCompleted).isTrue();
    }

    // ========================
    // 8-11: Approval + Job
    // ========================

    @Test
    @Order(8)
    void shouldHighToolWaitingApprovalNotCreateCompletedJob() {
        String[] pt = createFreshProjectAndTask("waj-" + System.currentTimeMillis());
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
        for (JsonNode te : toolExecs) {
            String status = TestJsonHelper.getString(te, "status");
            String jobId = TestJsonHelper.getString(te, "jobId");
            if ("WAITING_APPROVAL".equals(status)) {
                // WAITING_APPROVAL should not have a completed job
                assertThat(jobId).isEmpty();
                return;
            }
        }
    }

    @Test
    @Order(9)
    void shouldCreateJobAfterApprove() {
        String[] pt = createFreshProjectAndTask("ajc-" + System.currentTimeMillis());
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

        // Find WAITING_APPROVAL execution and approve it
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

        ResponseEntity<String> approveRes = post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "批准"));
        assertOk(approveRes);

        JsonNode approvedExec = TestJsonHelper.parse(approveRes.getBody()).get("data");
        String jobId = TestJsonHelper.getString(approvedExec, "jobId");
        assertThat(jobId).isNotEmpty();
    }

    @Test
    @Order(10)
    void shouldJobCompletedAfterApprove() {
        String[] pt = createFreshProjectAndTask("ajc2-" + System.currentTimeMillis());
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

        ResponseEntity<String> approveRes = post("/api/tool-sandbox-executions/" + execId + "/approve",
                Map.of("comment", "批准"));
        assertOk(approveRes);

        JsonNode approvedExec = TestJsonHelper.parse(approveRes.getBody()).get("data");
        JsonNode job = approvedExec.get("job");
        assertThat(job).isNotNull();
        String jobStatus = TestJsonHelper.getString(job, "status");
        assertThat(jobStatus).isEqualTo("COMPLETED");
    }

    @Test
    @Order(11)
    void shouldPatchProposalArtifactCreatedAfterApprove() {
        String[] pt = createFreshProjectAndTask("art-" + System.currentTimeMillis());
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

        post("/api/tool-sandbox-executions/" + execId + "/approve", Map.of("comment", "批准"));

        ResponseEntity<String> artRes = get("/api/tasks/" + tid + "/artifacts");
        assertOk(artRes);
        JsonNode artifacts = TestJsonHelper.parse(artRes.getBody()).get("data");
        boolean found = false;
        for (JsonNode art : artifacts) {
            if ("PATCH_PROPOSAL".equals(TestJsonHelper.getString(art, "artifactType"))) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    // ========================
    // 12-16: Job API
    // ========================

    @Test
    @Order(12)
    void shouldGetJobDetail() {
        String[] pt = createFreshProjectAndTask("gjd-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        String jobId = null;
        for (JsonNode te : toolExecs) {
            jobId = TestJsonHelper.getString(te, "jobId");
            if (jobId != null && !jobId.isEmpty()) break;
        }
        assertThat(jobId).isNotNull();

        ResponseEntity<String> jobRes = get("/api/tool-execution-jobs/" + jobId);
        assertOk(jobRes);
        JsonNode jobData = TestJsonHelper.parse(jobRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(jobData, "status")).isEqualTo("COMPLETED");
    }

    @Test
    @Order(13)
    void shouldListJobsByExecution() {
        String[] pt = createFreshProjectAndTask("lje-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        JsonNode toolExecs = runData.get("toolExecutions");
        String execId = null;
        for (JsonNode te : toolExecs) {
            String status = TestJsonHelper.getString(te, "status");
            String jobId = TestJsonHelper.getString(te, "jobId");
            if ("COMPLETED".equals(status) && jobId != null && !jobId.isEmpty()) {
                execId = TestJsonHelper.getString(te, "id");
                break;
            }
        }
        assertThat(execId).isNotNull();

        ResponseEntity<String> jobsRes = get("/api/tool-sandbox-executions/" + execId + "/jobs");
        assertOk(jobsRes);
        JsonNode jobs = TestJsonHelper.parse(jobsRes.getBody()).get("data");
        assertThat(jobs.isArray()).isTrue();
        assertThat(jobs.size()).isGreaterThan(0);
    }

    @Test
    @Order(14)
    void shouldListJobsByRun() {
        String[] pt = createFreshProjectAndTask("ljr-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String runId = TestJsonHelper.getString(runData, "id");

        ResponseEntity<String> jobsRes = get("/api/multi-agent-runs/" + runId + "/tool-execution-jobs");
        assertOk(jobsRes);
        JsonNode jobs = TestJsonHelper.parse(jobsRes.getBody()).get("data");
        assertThat(jobs.isArray()).isTrue();
        assertThat(jobs.size()).isGreaterThan(0);
    }

    @Test
    @Order(15)
    void shouldUnauthorizedGetJobReturn401() {
        ResponseEntity<String> res = restTemplate.getForEntity(
                baseUrl() + "/api/tool-execution-jobs/99999999", String.class);
        // Should be 401 unauthorized
        assertThat(res.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @Order(16)
    void shouldInvalidJobIdReturnNotFound() {
        ResponseEntity<String> res = get("/api/tool-execution-jobs/999999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 17-21: Retry / Cancel
    // ========================

    @Test
    @Order(17)
    void shouldRetryFailedJob() {
        String[] pt = createFreshProjectAndTask("retry-" + System.currentTimeMillis());
        String tid = pt[1];

        // Start a run to get a real tool execution
        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = null;
        String toolKey = null;
        JsonNode toolExecs = runData.get("toolExecutions");
        for (JsonNode te : toolExecs) {
            execId = TestJsonHelper.getString(te, "id");
            toolKey = TestJsonHelper.getString(te, "toolName");
            if (execId != null && !execId.isEmpty()) break;
        }
        assertThat(execId).isNotNull();

        // Insert a FAILED job directly
        ToolExecutionJobEntity failedJob = new ToolExecutionJobEntity();
        failedJob.setProjectId(Long.valueOf(pt[0]));
        failedJob.setTaskId(Long.valueOf(tid));
        failedJob.setToolExecutionId(Long.valueOf(execId));
        failedJob.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        failedJob.setStatus(ToolExecutionJobStatus.FAILED.name());
        failedJob.setPriority("NORMAL");
        failedJob.setRetryCount(0);
        failedJob.setMaxRetryCount(2);
        failedJob.setRequestPayload("{\"test\":true}");
        failedJob.setLastError("模拟失败");
        toolExecutionJobMapper.insert(failedJob);

        // Retry the failed job
        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/" + failedJob.getId() + "/retry",
                Map.of("reason", "重试测试"));
        assertOk(retryRes);

        JsonNode retryData = TestJsonHelper.parse(retryRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(retryData, "status")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getInt(retryData, "retryCount")).isEqualTo(1);
    }

    @Test
    @Order(18)
    void shouldRetryOverMaxRetryCountReturnConflict() {
        String[] pt = createFreshProjectAndTask("mxrt-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = null;
        String toolKey = null;
        for (JsonNode te : runData.get("toolExecutions")) {
            execId = TestJsonHelper.getString(te, "id");
            toolKey = TestJsonHelper.getString(te, "toolName");
            break;
        }
        assertThat(execId).isNotNull();

        // Insert a FAILED job at max retry
        ToolExecutionJobEntity failedJob = new ToolExecutionJobEntity();
        failedJob.setProjectId(Long.valueOf(pt[0]));
        failedJob.setTaskId(Long.valueOf(tid));
        failedJob.setToolExecutionId(Long.valueOf(execId));
        failedJob.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        failedJob.setStatus(ToolExecutionJobStatus.FAILED.name());
        failedJob.setPriority("NORMAL");
        failedJob.setRetryCount(2);
        failedJob.setMaxRetryCount(2);
        failedJob.setRequestPayload("{\"test\":true}");
        failedJob.setLastError("已达最大重试");
        toolExecutionJobMapper.insert(failedJob);

        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/" + failedJob.getId() + "/retry",
                Map.of("reason", "超限重试"));
        assertCode(retryRes, "CONFLICT");
    }

    @Test
    @Order(19)
    void shouldCancelPendingJob() {
        String[] pt = createFreshProjectAndTask("cncl-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = null;
        String toolKey = null;
        for (JsonNode te : runData.get("toolExecutions")) {
            execId = TestJsonHelper.getString(te, "id");
            toolKey = TestJsonHelper.getString(te, "toolName");
            break;
        }
        assertThat(execId).isNotNull();

        // Insert a PENDING job directly
        ToolExecutionJobEntity pendingJob = new ToolExecutionJobEntity();
        pendingJob.setProjectId(Long.valueOf(pt[0]));
        pendingJob.setTaskId(Long.valueOf(tid));
        pendingJob.setToolExecutionId(Long.valueOf(execId));
        pendingJob.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        pendingJob.setStatus(ToolExecutionJobStatus.PENDING.name());
        pendingJob.setPriority("NORMAL");
        pendingJob.setRetryCount(0);
        pendingJob.setMaxRetryCount(2);
        pendingJob.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(pendingJob);

        ResponseEntity<String> cancelRes = post("/api/tool-execution-jobs/" + pendingJob.getId() + "/cancel",
                Map.of());
        assertOk(cancelRes);

        JsonNode cancelData = TestJsonHelper.parse(cancelRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(cancelData, "status")).isEqualTo("CANCELED");
    }

    @Test
    @Order(20)
    void shouldCancelCompletedJobReturnConflict() {
        String[] pt = createFreshProjectAndTask("cncc-" + System.currentTimeMillis());
        String tid = pt[1];

        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);

        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        String execId = null;
        String toolKey = null;
        for (JsonNode te : runData.get("toolExecutions")) {
            execId = TestJsonHelper.getString(te, "id");
            toolKey = TestJsonHelper.getString(te, "toolName");
            break;
        }
        assertThat(execId).isNotNull();

        // Insert a COMPLETED job directly
        ToolExecutionJobEntity completedJob = new ToolExecutionJobEntity();
        completedJob.setProjectId(Long.valueOf(pt[0]));
        completedJob.setTaskId(Long.valueOf(tid));
        completedJob.setToolExecutionId(Long.valueOf(execId));
        completedJob.setToolKey(toolKey != null ? toolKey : "PROJECT_CONTEXT_SCAN");
        completedJob.setStatus(ToolExecutionJobStatus.COMPLETED.name());
        completedJob.setPriority("NORMAL");
        completedJob.setRetryCount(0);
        completedJob.setMaxRetryCount(2);
        completedJob.setRequestPayload("{\"test\":true}");
        toolExecutionJobMapper.insert(completedJob);

        ResponseEntity<String> cancelRes = post("/api/tool-execution-jobs/" + completedJob.getId() + "/cancel",
                Map.of());
        assertCode(cancelRes, "CONFLICT");
    }
}
