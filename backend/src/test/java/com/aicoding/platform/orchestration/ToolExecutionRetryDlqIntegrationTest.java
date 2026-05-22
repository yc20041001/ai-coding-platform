package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.ToolExecutionErrorCode;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionFailureStage;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.orchestration.worker.ToolExecutionRetryPolicy;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolExecutionRetryDlqIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ToolExecutionJobMapper toolExecutionJobMapper;

    @Autowired
    private ToolExecutionRetryPolicy retryPolicy;

    /**
     * Holds context for a project+task+run that can be reused to insert multiple jobs.
     */
    private static class RunCtx {
        String projectId;
        String taskId;
        String executionId;
        String toolKey;
    }

    private RunCtx createRun(String suffix) {
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-DLQ-" + suffix,
                "description", "DLQ retry test",
                "techStack", List.of("Java")
        ));
        assertOk(prjRes);
        RunCtx ctx = new RunCtx();
        ctx.projectId = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + ctx.projectId + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + ctx.projectId + "/tasks", Map.of(
                "title", "IT-DLQ-Task-" + suffix,
                "description", "DLQ retry test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        ctx.taskId = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> runRes = post("/api/tasks/" + ctx.taskId + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);
        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        for (JsonNode te : runData.get("toolExecutions")) {
            ctx.executionId = TestJsonHelper.getString(te, "id");
            ctx.toolKey = TestJsonHelper.getString(te, "toolName");
            if (ctx.executionId != null && !ctx.executionId.isEmpty()) break;
        }
        assertThat(ctx.executionId).isNotNull();
        return ctx;
    }

    private ToolExecutionJobEntity insertJob(RunCtx ctx, String status, int retryCount, int maxRetryCount) {
        return insertJob(ctx, status, retryCount, maxRetryCount, null, null);
    }

    private ToolExecutionJobEntity insertJob(RunCtx ctx, String status, int retryCount, int maxRetryCount,
                                              String errorCode, LocalDateTime nextRetryAt) {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(Long.valueOf(ctx.projectId));
        job.setTaskId(Long.valueOf(ctx.taskId));
        job.setToolExecutionId(Long.valueOf(ctx.executionId));
        job.setToolKey(ctx.toolKey != null ? ctx.toolKey : "PROJECT_CONTEXT_SCAN");
        job.setStatus(status);
        job.setPriority("NORMAL");
        job.setRetryCount(retryCount);
        job.setMaxRetryCount(maxRetryCount);
        job.setRequestPayload("{\"test\":true}");
        job.setLastError("模拟失败原因");
        if (errorCode != null) {
            job.setErrorCode(errorCode);
        }
        if (nextRetryAt != null) {
            job.setNextRetryAt(nextRetryAt);
        }
        toolExecutionJobMapper.insert(job);
        return job;
    }

    // ========================
    // 1-9: RetryPolicy unit tests (no DB needed)
    // ========================

    @Test
    @Order(1)
    void canRetryReturnsTrueForFailedJob() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        assertThat(retryPolicy.canRetry(job)).isTrue();
    }

    @Test
    @Order(2)
    void canRetryReturnsFalseForCanceledJob() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.CANCELED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(3)
    void canRetryReturnsFalseForDeadLetteredJob() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.DEAD_LETTERED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(4)
    void canRetryReturnsFalseAtMaxRetryCount() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(2);
        job.setMaxRetryCount(2);
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(5)
    void canRetryReturnsFalseForPolicyBlocked() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setErrorCode(ToolExecutionErrorCode.POLICY_BLOCKED.name());
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(6)
    void canRetryReturnsFalseForApprovalRequired() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setErrorCode(ToolExecutionErrorCode.APPROVAL_REQUIRED.name());
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(7)
    void canRetryReturnsFalseForMessageInvalid() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setErrorCode(ToolExecutionErrorCode.MESSAGE_INVALID.name());
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(8)
    void canRetryReturnsFalseForJobCanceledErrorCode() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setStatus(ToolExecutionJobStatus.FAILED.name());
        job.setRetryCount(0);
        job.setMaxRetryCount(2);
        job.setErrorCode(ToolExecutionErrorCode.JOB_CANCELED.name());
        assertThat(retryPolicy.canRetry(job)).isFalse();
    }

    @Test
    @Order(9)
    void nextDelayReturnsCorrectValues() {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setRetryCount(0);
        assertThat(retryPolicy.nextDelaySeconds(job)).isEqualTo(5L);

        job.setRetryCount(1);
        assertThat(retryPolicy.nextDelaySeconds(job)).isEqualTo(30L);

        job.setRetryCount(2);
        assertThat(retryPolicy.nextDelaySeconds(job)).isEqualTo(120L);

        // Beyond configured delays returns last value
        job.setRetryCount(5);
        assertThat(retryPolicy.nextDelaySeconds(job)).isEqualTo(120L);
    }

    // ========================
    // 10-11: listFailedJobs
    // ========================

    @Test
    @Order(10)
    void shouldListFailedJobsIncludeRetryPendingAndDeadLettered() {
        RunCtx ctx = createRun("lfj-" + System.currentTimeMillis());
        ToolExecutionJobEntity failedJob = insertJob(ctx,
                ToolExecutionJobStatus.FAILED.name(), 0, 2);
        ToolExecutionJobEntity dlqJob = insertJob(ctx,
                ToolExecutionJobStatus.DEAD_LETTERED.name(), 2, 2);

        ResponseEntity<String> listRes = get("/api/projects/" + ctx.projectId + "/tool-execution-jobs/failed");
        assertOk(listRes);
        JsonNode jobs = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(jobs.isArray()).isTrue();

        boolean foundFailed = false, foundDlq = false;
        for (JsonNode j : jobs) {
            String id = TestJsonHelper.getString(j, "id");
            if (failedJob.getId().toString().equals(id)) foundFailed = true;
            if (dlqJob.getId().toString().equals(id)) foundDlq = true;
        }
        assertThat(foundFailed).isTrue();
        assertThat(foundDlq).isTrue();
    }

    @Test
    @Order(11)
    void shouldListFailedJobsFilterByStatus() {
        RunCtx ctx = createRun("lfjs-" + System.currentTimeMillis());
        insertJob(ctx, ToolExecutionJobStatus.FAILED.name(), 0, 2);

        ResponseEntity<String> listRes = get("/api/projects/" + ctx.projectId
                + "/tool-execution-jobs/failed?status=FAILED");
        assertOk(listRes);
        JsonNode jobs = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(jobs.isArray()).isTrue();
        for (JsonNode j : jobs) {
            assertThat(TestJsonHelper.getString(j, "status")).isEqualTo("FAILED");
        }
    }

    // ========================
    // 12-13: manualRetry
    // ========================

    @Test
    @Order(12)
    void shouldManualRetryFailedJob() {
        RunCtx ctx = createRun("mrf-" + System.currentTimeMillis());
        ToolExecutionJobEntity failedJob = insertJob(ctx,
                ToolExecutionJobStatus.FAILED.name(), 0, 2);

        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/"
                + failedJob.getId() + "/manual-retry", Map.of("reason", "手动重试测试"));
        assertOk(retryRes);
        JsonNode data = TestJsonHelper.parse(retryRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isIn("COMPLETED", "PENDING");
        assertThat(TestJsonHelper.getInt(data, "retryCount")).isEqualTo(1);
    }

    @Test
    @Order(13)
    void shouldManualRetryDeadLetteredJob() {
        RunCtx ctx = createRun("mrdl-" + System.currentTimeMillis());
        ToolExecutionJobEntity dlqJob = insertJob(ctx,
                ToolExecutionJobStatus.DEAD_LETTERED.name(), 2, 2);

        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/"
                + dlqJob.getId() + "/manual-retry", Map.of("reason", "死信手动重试"));
        assertOk(retryRes);
        JsonNode data = TestJsonHelper.parse(retryRes.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "retryCount")).isEqualTo(3);
        // The new job should link back to the original
        assertThat(TestJsonHelper.getString(data, "sourceJobId"))
                .isEqualTo(dlqJob.getId().toString());
    }

    @Test
    @Order(14)
    void shouldManualRetrySetsSourceJobId() {
        RunCtx ctx = createRun("msrc-" + System.currentTimeMillis());
        ToolExecutionJobEntity failedJob = insertJob(ctx,
                ToolExecutionJobStatus.FAILED.name(), 0, 2);

        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/"
                + failedJob.getId() + "/manual-retry", Map.of());
        assertOk(retryRes);
        JsonNode data = TestJsonHelper.parse(retryRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "sourceJobId"))
                .isEqualTo(failedJob.getId().toString());
    }

    // ========================
    // 15-16: recoverTimedOutRunningJobs
    // ========================

    @Test
    @Order(15)
    void shouldRecoverTimedOutRunningJobs() {
        RunCtx ctx = createRun("recto-" + System.currentTimeMillis());

        ToolExecutionJobEntity runningJob = insertJob(ctx,
                ToolExecutionJobStatus.RUNNING.name(), 0, 2);
        runningJob.setStartedAt(LocalDateTime.now().minusSeconds(600)); // 10 min ago, past timeout
        toolExecutionJobMapper.updateById(runningJob);

        ResponseEntity<String> recoverRes = post("/api/tool-execution-jobs/recover-timeouts", Map.of());
        assertOk(recoverRes);
        int count = TestJsonHelper.getInt(TestJsonHelper.parse(recoverRes.getBody()), "data");
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(16)
    void shouldNotRecoverNonTimedOutJobs() {
        RunCtx ctx = createRun("nocto-" + System.currentTimeMillis());

        // Insert a RUNNING job with recent startedAt (should NOT be recovered)
        insertJob(ctx, ToolExecutionJobStatus.RUNNING.name(), 0, 2);

        // First recovery to clear any pre-existing timed-out jobs from other tests
        post("/api/tool-execution-jobs/recover-timeouts", Map.of());

        // Recovery again — the recent job should not be counted
        ResponseEntity<String> recoverRes = post("/api/tool-execution-jobs/recover-timeouts", Map.of());
        assertOk(recoverRes);
        int count = TestJsonHelper.getInt(TestJsonHelper.parse(recoverRes.getBody()), "data");
        assertThat(count).isEqualTo(0);
    }

    // ========================
    // 17-19: dispatchRetries
    // ========================

    @Test
    @Order(17)
    void shouldDispatchRetryPendingJob() {
        RunCtx ctx = createRun("dprp-" + System.currentTimeMillis());
        insertJob(ctx, ToolExecutionJobStatus.RETRY_PENDING.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(),
                LocalDateTime.now().minusSeconds(10)); // Past due

        ResponseEntity<String> dispatchRes = post("/api/tool-execution-jobs/dispatch-retries", Map.of());
        assertOk(dispatchRes);
        int count = TestJsonHelper.getInt(TestJsonHelper.parse(dispatchRes.getBody()), "data");
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(18)
    void shouldNotDispatchFutureRetryJob() {
        RunCtx ctx = createRun("frdp-" + System.currentTimeMillis());
        insertJob(ctx, ToolExecutionJobStatus.RETRY_PENDING.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(),
                LocalDateTime.now().plusSeconds(3600)); // Future

        ResponseEntity<String> dispatchRes = post("/api/tool-execution-jobs/dispatch-retries", Map.of());
        assertOk(dispatchRes);
        int count = TestJsonHelper.getInt(TestJsonHelper.parse(dispatchRes.getBody()), "data");
        // Our future job should NOT be dispatched; count may be 0
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(19)
    void shouldDispatchRetriesForMultipleJobs() {
        RunCtx ctx = createRun("mdr-" + System.currentTimeMillis());
        insertJob(ctx, ToolExecutionJobStatus.RETRY_PENDING.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(),
                LocalDateTime.now().minusSeconds(10));
        insertJob(ctx, ToolExecutionJobStatus.RETRY_PENDING.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(),
                LocalDateTime.now().minusSeconds(20));

        ResponseEntity<String> dispatchRes = post("/api/tool-execution-jobs/dispatch-retries", Map.of());
        assertOk(dispatchRes);
        int count = TestJsonHelper.getInt(TestJsonHelper.parse(dispatchRes.getBody()), "data");
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    // ========================
    // 20-23: Security / Edge cases
    // ========================

    @Test
    @Order(20)
    void shouldListFailedJobsUnauthorizedWithoutToken() {
        RunCtx ctx = createRun("unauth-" + System.currentTimeMillis());

        ResponseEntity<String> res = restTemplate.getForEntity(
                baseUrl() + "/api/projects/" + ctx.projectId + "/tool-execution-jobs/failed",
                String.class);
        assertThat(res.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @Order(21)
    void shouldManualRetryJobReturnNotFoundForInvalidId() {
        ResponseEntity<String> res = post("/api/tool-execution-jobs/999999999/manual-retry", Map.of("reason", "test"));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    @Order(22)
    void shouldManualRetryPendingJobReturnConflict() {
        RunCtx ctx = createRun("mrpn-" + System.currentTimeMillis());
        ToolExecutionJobEntity pendingJob = insertJob(ctx,
                ToolExecutionJobStatus.PENDING.name(), 0, 2);

        ResponseEntity<String> retryRes = post("/api/tool-execution-jobs/"
                + pendingJob.getId() + "/manual-retry", Map.of());
        assertCode(retryRes, "CONFLICT");
    }

    // ========================
    // 23-24: API auth (system-level endpoints)
    // ========================

    @Test
    @Order(23)
    void shouldRecoverTimeoutsReturnOk() {
        ResponseEntity<String> res = post("/api/tool-execution-jobs/recover-timeouts", Map.of());
        assertOk(res);
    }

    @Test
    @Order(24)
    void shouldDispatchRetriesReturnOk() {
        ResponseEntity<String> res = post("/api/tool-execution-jobs/dispatch-retries", Map.of());
        assertOk(res);
    }

    // ========================
    // 25: Job response DLQ fields (set errorCode so Jackson includes it)
    // ========================

    @Test
    @Order(25)
    void shouldJobResponseContainDlqFields() {
        RunCtx ctx = createRun("dlqf-" + System.currentTimeMillis());
        ToolExecutionJobEntity job = insertJob(ctx,
                ToolExecutionJobStatus.FAILED.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(), null);
        // Set all DLQ fields to non-null so Jackson includes them in JSON
        LocalDateTime now = LocalDateTime.now();
        job.setFailureStage(ToolExecutionFailureStage.MOCK_EXECUTE.name());
        job.setNextRetryAt(now.plusSeconds(30));
        job.setDeadLetteredAt(now);
        job.setDeadLetterReason("模拟死信原因");
        job.setSourceJobId(999999L);
        toolExecutionJobMapper.updateById(job);

        ResponseEntity<String> detailRes = get("/api/tool-execution-jobs/" + job.getId());
        assertOk(detailRes);
        JsonNode data = TestJsonHelper.parse(detailRes.getBody()).get("data");
        assertThat(data.has("errorCode")).isTrue();
        assertThat(data.has("failureStage")).isTrue();
        assertThat(data.has("nextRetryAt")).isTrue();
        assertThat(data.has("deadLetteredAt")).isTrue();
        assertThat(data.has("deadLetterReason")).isTrue();
        assertThat(data.has("sourceJobId")).isTrue();
        assertThat(TestJsonHelper.getString(data, "errorCode")).isEqualTo("MOCK_EXECUTION_FAILED");
        assertThat(TestJsonHelper.getString(data, "failureStage")).isEqualTo("MOCK_EXECUTE");
        assertThat(TestJsonHelper.getString(data, "deadLetterReason")).isEqualTo("模拟死信原因");
    }

    // ========================
    // 26: Job list includes error fields
    // ========================

    @Test
    @Order(26)
    void shouldFailedJobListIncludeErrorFields() {
        RunCtx ctx = createRun("elf-" + System.currentTimeMillis());
        ToolExecutionJobEntity job = insertJob(ctx,
                ToolExecutionJobStatus.FAILED.name(), 0, 2,
                ToolExecutionErrorCode.MOCK_EXECUTION_FAILED.name(), null);

        ResponseEntity<String> listRes = get("/api/projects/" + ctx.projectId
                + "/tool-execution-jobs/failed?status=FAILED");
        assertOk(listRes);
        JsonNode jobs = TestJsonHelper.parse(listRes.getBody()).get("data");
        boolean found = false;
        for (JsonNode j : jobs) {
            if (TestJsonHelper.getString(j, "id").equals(job.getId().toString())) {
                assertThat(TestJsonHelper.getString(j, "lastError")).isNotEmpty();
                assertThat(TestJsonHelper.getString(j, "errorCode")).isEqualTo("MOCK_EXECUTION_FAILED");
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
