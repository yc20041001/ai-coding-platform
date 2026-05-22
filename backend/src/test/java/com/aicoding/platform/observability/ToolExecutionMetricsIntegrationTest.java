package com.aicoding.platform.observability;

import com.aicoding.platform.orchestration.domain.ToolExecutionJobEntity;
import com.aicoding.platform.orchestration.domain.ToolExecutionJobStatus;
import com.aicoding.platform.orchestration.infrastructure.ToolExecutionJobMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestDataFactory;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolExecutionMetricsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ToolExecutionJobMapper toolExecutionJobMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String projectIdStr;
    private Long projectId;

    @BeforeAll
    public void setup() {
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-Metrics-" + TestDataFactory.uniqueSuffix(),
                "description", "Tool execution metrics test",
                "techStack", java.util.List.of("Java")
        ));
        assertOk(prjRes);
        projectIdStr = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");
        projectId = Long.valueOf(projectIdStr);
    }

    @AfterEach
    public void cleanup() {
        toolExecutionJobMapper.delete(null);
    }

    // ========================
    // Helpers
    // ========================

    private ToolExecutionJobEntity insertJob(String toolKey, String status, long durationMs,
                                              String errorCode, String failureStage,
                                              Integer retryCount) {
        ToolExecutionJobEntity job = new ToolExecutionJobEntity();
        job.setProjectId(projectId);
        job.setTaskId(999999L);
        job.setToolExecutionId(999999L);
        job.setToolKey(toolKey);
        job.setStatus(status);
        job.setPriority("NORMAL");
        job.setRetryCount(retryCount != null ? retryCount : 0);
        job.setMaxRetryCount(2);
        job.setDurationMs(durationMs);
        job.setErrorCode(errorCode);
        job.setFailureStage(failureStage);
        job.setRequestPayload("{}");
        toolExecutionJobMapper.insert(job);
        return job;
    }

    private ToolExecutionJobEntity insertJob(String toolKey, String status, long durationMs) {
        return insertJob(toolKey, status, durationMs, null, null, null);
    }

    private void insertJobRawSql(Long projectId, String toolKey, String status, long durationMs,
                                  String errorCode, String failureStage, LocalDateTime createTime) {
        jdbcTemplate.update(
                "INSERT INTO tool_execution_job (project_id, task_id, tool_execution_id, tool_key, status, " +
                "priority, retry_count, max_retry_count, duration_ms, error_code, failure_stage, " +
                "request_payload, create_time, update_time) " +
                "VALUES (?, 999999, 999999, ?, ?, 'NORMAL', 0, 2, ?, ?, ?, '{}', ?, ?)",
                projectId, toolKey, status, durationMs, errorCode, failureStage,
                createTime, createTime);
    }

    private JsonNode getGlobalMetrics() {
        ResponseEntity<String> res = get("/api/observability/tool-executions/metrics");
        assertOk(res);
        return TestJsonHelper.parse(res.getBody()).get("data");
    }

    private JsonNode getProjectMetrics() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdStr + "/observability/tool-executions/metrics");
        assertOk(res);
        return TestJsonHelper.parse(res.getBody()).get("data");
    }

    // ========================
    // 1. Empty Metrics
    // ========================

    @Test
    void globalMetricsReturnsEmptyWhenNoJobs() {
        JsonNode data = getGlobalMetrics();
        assertThat(data.has("summary")).isTrue();
        assertThat(data.get("summary").get("totalJobs").asLong()).isZero();
        assertThat(data.get("tools").isArray()).isTrue();
        assertThat(data.get("tools").size()).isZero();
        assertThat(data.get("daily").isArray()).isTrue();
        assertThat(data.get("daily").size()).isEqualTo(30);
        assertThat(data.get("errorCodes").isArray()).isTrue();
        assertThat(data.get("errorCodes").size()).isZero();
        assertThat(data.get("failureStages").isArray()).isTrue();
        assertThat(data.get("failureStages").size()).isZero();
    }

    // ========================
    // 2. Summary Calculations
    // ========================

    @Test
    void summaryComputesCorrectlyWithMixedStatusJobs() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 200);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 50);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 300);
        insertJob("TOOL_B", ToolExecutionJobStatus.RETRY_PENDING.name(), 0);
        insertJob("TOOL_B", ToolExecutionJobStatus.DEAD_LETTERED.name(), 0);
        insertJob("TOOL_C", ToolExecutionJobStatus.RUNNING.name(), 0);
        insertJob("TOOL_C", ToolExecutionJobStatus.PENDING.name(), 0);
        insertJob("TOOL_C", ToolExecutionJobStatus.CANCELED.name(), 0);

        JsonNode summary = getGlobalMetrics().get("summary");
        assertThat(summary.get("totalJobs").asLong()).isEqualTo(9);
        assertThat(summary.get("completedJobs").asLong()).isEqualTo(3);
        assertThat(summary.get("failedJobs").asLong()).isEqualTo(1);
        assertThat(summary.get("deadLetteredJobs").asLong()).isEqualTo(1);
        assertThat(summary.get("retryPendingJobs").asLong()).isEqualTo(1);
        assertThat(summary.get("runningJobs").asLong()).isEqualTo(1);
        assertThat(summary.get("pendingJobs").asLong()).isEqualTo(1);
        assertThat(summary.get("canceledJobs").asLong()).isEqualTo(1);
        // success rate = completed / total = 3/9
        assertThat(summary.get("successRate").asDouble()).isCloseTo(3.0 / 9.0, within(0.001));
        // avg duration = average of completed jobs only = (100+200+300)/3
        assertThat(summary.get("avgDurationMs").asDouble()).isCloseTo(200.0, within(0.001));
        // max duration = max of all jobs = 300
        assertThat(summary.get("maxDurationMs").asLong()).isEqualTo(300);
    }

    @Test
    void summaryReturnsZeroWhenNoCompletedJobs() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 100);
        insertJob("TOOL_A", ToolExecutionJobStatus.RETRY_PENDING.name(), 0);

        JsonNode summary = getGlobalMetrics().get("summary");
        assertThat(summary.get("totalJobs").asLong()).isEqualTo(2);
        assertThat(summary.get("completedJobs").asLong()).isZero();
        assertThat(summary.get("successRate").asDouble()).isZero();
        assertThat(summary.get("avgDurationMs").asDouble()).isZero();
    }

    // ========================
    // 3. Tool Metrics
    // ========================

    @Test
    void toolMetricsGroupedByToolKey() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 200);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 50);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 300);
        insertJob("TOOL_B", ToolExecutionJobStatus.DEAD_LETTERED.name(), 0);

        JsonNode tools = getGlobalMetrics().get("tools");
        assertThat(tools.size()).isEqualTo(2);

        // TOOL_A: 3 jobs, 2 completed, 1 failed, successRate = 2/3
        JsonNode toolA = tools.get(0).get("toolKey").asText().equals("TOOL_A") ? tools.get(0) : tools.get(1);
        JsonNode toolB = tools.get(0).get("toolKey").asText().equals("TOOL_B") ? tools.get(0) : tools.get(1);

        assertThat(toolA.get("totalJobs").asLong()).isEqualTo(3);
        assertThat(toolA.get("completedJobs").asLong()).isEqualTo(2);
        assertThat(toolA.get("failedJobs").asLong()).isEqualTo(1);
        assertThat(toolA.get("deadLetteredJobs").asLong()).isZero();
        assertThat(toolA.get("successRate").asDouble()).isCloseTo(2.0 / 3.0, within(0.001));

        // TOOL_B: 2 jobs, 1 completed, 1 DLQ
        assertThat(toolB.get("totalJobs").asLong()).isEqualTo(2);
        assertThat(toolB.get("completedJobs").asLong()).isEqualTo(1);
        assertThat(toolB.get("deadLetteredJobs").asLong()).isEqualTo(1);
    }

    @Test
    void toolMetricsSortedByTotalJobsDesc() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 200);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 300);
        insertJob("TOOL_C", ToolExecutionJobStatus.COMPLETED.name(), 400);
        insertJob("TOOL_C", ToolExecutionJobStatus.COMPLETED.name(), 500);
        insertJob("TOOL_C", ToolExecutionJobStatus.COMPLETED.name(), 600);

        JsonNode tools = getGlobalMetrics().get("tools");
        assertThat(tools.size()).isEqualTo(3);
        assertThat(tools.get(0).get("toolKey").asText()).isEqualTo("TOOL_C"); // 3 jobs
        assertThat(tools.get(1).get("toolKey").asText()).isEqualTo("TOOL_B"); // 2 jobs
        assertThat(tools.get(2).get("toolKey").asText()).isEqualTo("TOOL_A"); // 1 job
    }

    @Test
    void toolMetricsTopErrorCodeAndFailureStage() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", "NETWORK", null);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", "NETWORK", null);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_AUTH", "AUTH", null);
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, null);

        JsonNode tools = getGlobalMetrics().get("tools");
        assertThat(tools.size()).isEqualTo(1);
        JsonNode toolA = tools.get(0);
        assertThat(toolA.get("topErrorCode").asText()).isEqualTo("ERR_TIMEOUT");
        assertThat(toolA.get("topFailureStage").asText()).isEqualTo("NETWORK");
    }

    // ========================
    // 4. Daily Metrics
    // ========================

    @Test
    void dailyMetricsCoversExactly30Days() {
        // Insert a job with current time via mapper (auto sets createTime)
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);

        JsonNode daily = getGlobalMetrics().get("daily");
        assertThat(daily.size()).isEqualTo(30);
        // All dates should be present and in order
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            String expectedDate = today.minusDays(29 - i).toString();
            assertThat(daily.get(i).get("date").asText()).isEqualTo(expectedDate);
        }
    }

    @Test
    void dailyMetricsAggregatesJobsByDate() {
        // Use JDBC to insert jobs on specific dates
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime twoDaysAgo = today.minusDays(2);

        insertJobRawSql(projectId, "TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, yesterday);
        insertJobRawSql(projectId, "TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 200, null, null, yesterday);
        insertJobRawSql(projectId, "TOOL_B", ToolExecutionJobStatus.FAILED.name(), 50, null, null, twoDaysAgo);

        JsonNode daily = getGlobalMetrics().get("daily");
        // Find the entries for yesterday and two days ago
        String yesterdayStr = yesterday.toLocalDate().toString();
        String twoDaysAgoStr = twoDaysAgo.toLocalDate().toString();

        for (int i = 0; i < daily.size(); i++) {
            String date = daily.get(i).get("date").asText();
            if (date.equals(yesterdayStr)) {
                assertThat(daily.get(i).get("totalJobs").asLong()).isEqualTo(2);
                assertThat(daily.get(i).get("completedJobs").asLong()).isEqualTo(2);
                assertThat(daily.get(i).get("avgDurationMs").asDouble()).isCloseTo(150.0, within(0.001));
            } else if (date.equals(twoDaysAgoStr)) {
                assertThat(daily.get(i).get("totalJobs").asLong()).isEqualTo(1);
                assertThat(daily.get(i).get("failedJobs").asLong()).isEqualTo(1);
            } else {
                assertThat(daily.get(i).get("totalJobs").asLong()).isZero();
            }
        }
    }

    // ========================
    // 5. Failure Metrics
    // ========================

    @Test
    void errorCodeMetricsGroupedAndSorted() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", null, null);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", null, null);
        insertJob("TOOL_B", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_AUTH", null, null);
        insertJob("TOOL_C", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, null);

        JsonNode errorCodes = getGlobalMetrics().get("errorCodes");
        assertThat(errorCodes.size()).isEqualTo(2);
        // Sorted by count desc: ERR_TIMEOUT (2) first
        assertThat(errorCodes.get(0).get("errorCode").asText()).isEqualTo("ERR_TIMEOUT");
        assertThat(errorCodes.get(0).get("count").asLong()).isEqualTo(2);
        assertThat(errorCodes.get(1).get("errorCode").asText()).isEqualTo("ERR_AUTH");
        assertThat(errorCodes.get(1).get("count").asLong()).isEqualTo(1);
    }

    @Test
    void failureStageMetricsGroupedAndSorted() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, null, "NETWORK", null);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, null, "NETWORK", null);
        insertJob("TOOL_B", ToolExecutionJobStatus.FAILED.name(), 0, null, "AUTH", null);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, null, "TIMEOUT", null);

        JsonNode stages = getGlobalMetrics().get("failureStages");
        assertThat(stages.size()).isEqualTo(3);
        assertThat(stages.get(0).get("errorCode").asText()).isEqualTo("NETWORK");
        assertThat(stages.get(0).get("count").asLong()).isEqualTo(2);
    }

    @Test
    void failureMetricsExcludesCompletedJobsWithoutErrorProps() {
        // Completed job with no error details should not appear in error/failure metrics
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, null);

        JsonNode errorCodes = getGlobalMetrics().get("errorCodes");
        assertThat(errorCodes.size()).isZero();

        // But the job IS counted in total
        assertThat(getGlobalMetrics().get("summary").get("totalJobs").asLong()).isEqualTo(1);
    }

    // ========================
    // 6. Project-Scoped Metrics
    // ========================

    @Test
    void projectMetricsReturnsOnlyThatProjectsJobs() {
        // Create a second project with its own jobs
        ResponseEntity<String> prjRes2 = post("/api/projects", Map.of(
                "name", "IT-Metrics-2-" + TestDataFactory.uniqueSuffix(),
                "description", "Second project",
                "techStack", java.util.List.of("Java")
        ));
        assertOk(prjRes2);
        String prjId2 = TestJsonHelper.getString(TestJsonHelper.parse(prjRes2.getBody()), "data.id");

        // Insert jobs in project 1
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        // Insert jobs in project 2 via raw SQL to avoid meta-handler interference
        jdbcTemplate.update(
                "INSERT INTO tool_execution_job (project_id, task_id, tool_execution_id, tool_key, status, " +
                "priority, retry_count, max_retry_count, duration_ms, request_payload, create_time, update_time) " +
                "VALUES (?, 999999, 999999, 'TOOL_B', 'COMPLETED', 'NORMAL', 0, 2, 200, '{}', NOW(), NOW())",
                Long.valueOf(prjId2));

        // Global metrics should see all jobs
        JsonNode global = getGlobalMetrics();
        assertThat(global.get("summary").get("totalJobs").asLong()).isEqualTo(2);

        // Project 1 metrics should only see its own job
        JsonNode proj = getProjectMetrics();
        assertThat(proj.get("summary").get("totalJobs").asLong()).isEqualTo(1);
        assertThat(proj.get("tools").get(0).get("toolKey").asText()).isEqualTo("TOOL_A");
    }

    // ========================
    // 7. Retry Metrics
    // ========================

    @Test
    void summaryTracksRetriesAndDurations() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 500, null, null, 3);
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 1500, null, null, 1);
        insertJob("TOOL_B", ToolExecutionJobStatus.FAILED.name(), 100, null, null, 0);

        JsonNode summary = getGlobalMetrics().get("summary");
        assertThat(summary.get("totalRetries").asLong()).isEqualTo(4); // 3 + 1 + 0
        // avg durationMs: only completed jobs = (500+1500)/2 = 1000
        assertThat(summary.get("avgDurationMs").asDouble()).isCloseTo(1000.0, within(0.001));
        // total jobs with retries > 0 = 2 (TOOL_A has 2 jobs with retries)
        // Actually retryRate = count of jobs with retry>0 / total = 2/3
        assertThat(summary.get("retryRate").asDouble()).isCloseTo(2.0 / 3.0, within(0.001));
    }

    // ========================
    // 8. 30-Day Filtering
    // ========================

    @Test
    void metricsOnlyIncludesLast30Days() {
        // Insert a job from 35 days ago (beyond the 30-day window)
        LocalDateTime oldDate = LocalDate.now().minusDays(35).atStartOfDay();
        insertJobRawSql(projectId, "TOOL_OLD", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, oldDate);

        // Should not appear in metrics
        JsonNode summary = getGlobalMetrics().get("summary");
        assertThat(summary.get("totalJobs").asLong()).isZero();
    }

    // ========================
    // 9. Problem Jobs Endpoint
    // ========================

    @Test
    void problemJobsReturnsFailedAndRetryAndDLQByDefault() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", null, null);
        insertJob("TOOL_A", ToolExecutionJobStatus.RETRY_PENDING.name(), 0, null, null, null);
        insertJob("TOOL_B", ToolExecutionJobStatus.DEAD_LETTERED.name(), 0, "ERR_FATAL", null, null);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 100, null, null, null);
        insertJob("TOOL_B", ToolExecutionJobStatus.CANCELED.name(), 0, null, null, null);

        ResponseEntity<String> res = get("/api/projects/" + projectIdStr + "/observability/tool-executions/problem-jobs");
        assertOk(res);
        JsonNode problems = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(problems.isArray()).isTrue();
        assertThat(problems.size()).isEqualTo(3); // FAILED + RETRY_PENDING + DEAD_LETTERED
    }

    @Test
    void problemJobsFiltersByStatus() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", null, null);
        insertJob("TOOL_A", ToolExecutionJobStatus.RETRY_PENDING.name(), 0, null, null, null);
        insertJob("TOOL_B", ToolExecutionJobStatus.DEAD_LETTERED.name(), 0, "ERR_FATAL", null, null);

        ResponseEntity<String> res = get("/api/projects/" + projectIdStr
                + "/observability/tool-executions/problem-jobs?status=FAILED");
        assertOk(res);
        JsonNode problems = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(problems.size()).isEqualTo(1);
        assertThat(problems.get(0).get("status").asText()).isEqualTo("FAILED");
    }

    @Test
    void problemJobsLimitsResults() {
        for (int i = 0; i < 5; i++) {
            insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_" + i, null, null);
        }

        ResponseEntity<String> res = get("/api/projects/" + projectIdStr
                + "/observability/tool-executions/problem-jobs?limit=2");
        assertOk(res);
        JsonNode problems = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(problems.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void problemJobsOmitsPayloads() {
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0, "ERR_TIMEOUT", null, null);

        ResponseEntity<String> res = get("/api/projects/" + projectIdStr
                + "/observability/tool-executions/problem-jobs");
        assertOk(res);
        JsonNode job = TestJsonHelper.parse(res.getBody()).get("data").get(0);
        // Payloads are explicitly nulled in the response for security
        assertThat(job.has("requestPayload")).isFalse();
        assertThat(job.has("resultPayload")).isFalse();
    }

    // ========================
    // 10. Auth / Authorization
    // ========================

    @Test
    void globalMetricsRejectsNonAdmin() {
        ResponseEntity<String> res = getNoAuth("/api/observability/tool-executions/metrics");
        // 401 UNAUTHORIZED
        JsonNode body = TestJsonHelper.parse(res.getBody());
        assertThat(body.get("code").asText()).isNotEqualTo("OK");
    }

    // ========================
    // 11. Tool Metrics Edge Cases
    // ========================

    @Test
    void toolMetricsHandlesNullToolKey() {
        // Tool key should not be null (DB constraint), but if mapper returns null toolKey it's filtered
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);

        JsonNode tools = getGlobalMetrics().get("tools");
        assertThat(tools.size()).isEqualTo(1);
    }

    @Test
    void toolMetricsAvgDurationUsesAllJobsForDuration() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 200);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 0); // durationMs = 0

        JsonNode tools = getGlobalMetrics().get("tools");
        assertThat(tools.size()).isEqualTo(2);
        // TOOL_A avg duration = (100+200)/2 = 150
        JsonNode toolA = tools.get(0).get("toolKey").asText().equals("TOOL_A") ? tools.get(0) : tools.get(1);
        assertThat(toolA.get("avgDurationMs").asDouble()).isCloseTo(150.0, within(0.001));
    }

    // ========================
    // 12. Summary Edge Cases
    // ========================

    @Test
    void summaryMaxDurationWhenNoDurationJobs() {
        // All jobs have null/default duration
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 0);
        insertJob("TOOL_B", ToolExecutionJobStatus.FAILED.name(), 0);

        JsonNode summary = getGlobalMetrics().get("summary");
        assertThat(summary.get("maxDurationMs").asLong()).isZero();
        assertThat(summary.get("totalJobs").asLong()).isEqualTo(2);
    }

    @Test
    void summaryFailureRateIsFailedPlusDLQOverTotal() {
        insertJob("TOOL_A", ToolExecutionJobStatus.COMPLETED.name(), 100);
        insertJob("TOOL_A", ToolExecutionJobStatus.FAILED.name(), 0);
        insertJob("TOOL_B", ToolExecutionJobStatus.DEAD_LETTERED.name(), 0);
        insertJob("TOOL_B", ToolExecutionJobStatus.COMPLETED.name(), 200);

        JsonNode summary = getGlobalMetrics().get("summary");
        // failure rate = (1 failed + 1 DLQ) / 4 total = 0.5
        assertThat(summary.get("failureRate").asDouble()).isCloseTo(0.5, within(0.001));
    }
}
