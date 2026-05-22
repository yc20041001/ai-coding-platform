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

class ToolOperatorReviewIntegrationTest extends IntegrationTestBase {

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Review-" + suffix,
                "description", "Operator review test",
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
                "title", "IT-Review-Task-" + suffix,
                "description", "Review test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String[] freshCompletedRun() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-CR";
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
    // Create Review
    // ========================

    @Test
    void shouldCreateReview() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "需要审查此工具执行"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "reviewTargetType")).isEqualTo("TOOL_EXECUTION");
        assertThat(TestJsonHelper.getString(data, "reviewTargetId")).isEqualTo(executionId);
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("MEDIUM");
        assertThat(TestJsonHelper.getString(data, "title")).isEqualTo("需要审查此工具执行");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("OPEN");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
    }

    @Test
    void shouldCreateReviewWithAssignee() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "HIGH",
                "title", "紧急审查",
                "summary", "此执行存在异常行为",
                "assigneeId", "100001"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("HIGH");
        assertThat(TestJsonHelper.getString(data, "summary")).isEqualTo("此执行存在异常行为");
        assertThat(TestJsonHelper.getString(data, "assigneeId")).isEqualTo("100001");
    }

    @Test
    void shouldCreateReviewForRunTarget() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "MULTI_AGENT_RUN",
                "reviewTargetId", runInfo[0],
                "severity", "LOW",
                "title", "审查整个 Run"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "reviewTargetType")).isEqualTo("MULTI_AGENT_RUN");
        assertThat(TestJsonHelper.getString(data, "reviewTargetId")).isEqualTo(runInfo[0]);
    }

    @Test
    void shouldCreateReviewForTaskTarget() {
        String[] runInfo = freshCompletedRun();

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TASK",
                "reviewTargetId", runInfo[1],
                "severity", "CRITICAL",
                "title", "审查整个 Task"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "reviewTargetType")).isEqualTo("TASK");
        assertThat(TestJsonHelper.getString(data, "reviewTargetId")).isEqualTo(runInfo[1]);
    }

    @Test
    void shouldCreateReviewAllSeverities() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);
        String[] severities = {"INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL"};

        for (String sev : severities) {
            ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                    "reviewTargetType", "TOOL_EXECUTION",
                    "reviewTargetId", executionId,
                    "severity", sev,
                    "title", "测试严重级别: " + sev
            ));
            assertOk(res);
        }
    }

    // ========================
    // Create Review - Validation
    // ========================

    @Test
    void shouldCreateReviewValidationErrorMissingTitle() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", ""
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateReviewValidationErrorInvalidSeverity() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "INVALID_SEVERITY",
                "title", "test"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateReviewValidationErrorInvalidTargetType() {
        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "INVALID_TYPE",
                "reviewTargetId", "1",
                "severity", "MEDIUM",
                "title", "test"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateReviewTargetNotFound() {
        ResponseEntity<String> res = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", "99999999",
                "severity", "MEDIUM",
                "title", "test"
        ));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Get Review
    // ========================

    @Test
    void shouldGetReview() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "获取测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> getRes = get("/api/orchestration/operator-reviews/" + reviewId);
        assertOk(getRes);
        JsonNode data = TestJsonHelper.parse(getRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(reviewId);
        assertThat(TestJsonHelper.getString(data, "title")).isEqualTo("获取测试");
    }

    @Test
    void shouldGetReviewNotFound() {
        ResponseEntity<String> res = get("/api/orchestration/operator-reviews/99999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Update Review
    // ========================

    @Test
    void shouldUpdateReviewStatus() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "状态更新测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "status", "IN_PROGRESS"
        ));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldUpdateReviewResolutionAndStatus() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "解决测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "status", "RESOLVED",
                "resolution", "已确认无问题"
        ));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("RESOLVED");
        assertThat(TestJsonHelper.getString(data, "resolution")).isEqualTo("已确认无问题");
    }

    @Test
    void shouldUpdateReviewAutoSetResolvedAt() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "自动时间测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "status", "RESOLVED"
        ));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("RESOLVED");
        // resolvedAt should be set (not empty)
        assertThat(TestJsonHelper.getString(data, "resolvedAt")).isNotEmpty();
        // resolvedBy should be set
        assertThat(TestJsonHelper.getString(data, "resolvedBy")).isNotEmpty();
    }

    @Test
    void shouldUpdateReviewNotFound() {
        ResponseEntity<String> res = put("/api/orchestration/operator-reviews/99999999", Map.of(
                "status", "RESOLVED"
        ));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldUpdateReviewInvalidStatus() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "无效状态测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "status", "INVALID_STATUS"
        ));
        assertCode(updateRes, "VALIDATION_ERROR");
    }

    @Test
    void shouldUpdateReviewSeverity() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "LOW",
                "title", "严重级别更新测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "severity", "HIGH"
        ));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("HIGH");
    }

    @Test
    void shouldUpdateReviewWontFixSetsResolvedAt() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        ResponseEntity<String> createRes = post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "LOW",
                "title", "WontFix 测试"
        ));
        assertOk(createRes);
        String reviewId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id"));

        ResponseEntity<String> updateRes = put("/api/orchestration/operator-reviews/" + reviewId, Map.of(
                "status", "WONT_FIX"
        ));
        assertOk(updateRes);
        JsonNode data = TestJsonHelper.parse(updateRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("WONT_FIX");
        assertThat(TestJsonHelper.getString(data, "resolvedAt")).isNotEmpty();
    }

    // ========================
    // List Project Reviews
    // ========================

    @Test
    void shouldListProjectReviews() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        // Create a review first
        post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "列表测试"
        ));

        // Need projectId from the execution
        ResponseEntity<String> execRes = get("/api/tool-sandbox-executions/" + executionId);
        assertOk(execRes);
        String projectId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(execRes.getBody()), "data.projectId"));

        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/operator-reviews?page=1&pageSize=20");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(data.has("records")).isTrue();
        assertThat(data.has("total")).isTrue();
    }

    @Test
    void shouldListProjectReviewsFilterByStatus() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "状态过滤测试"
        ));

        ResponseEntity<String> execRes = get("/api/tool-sandbox-executions/" + executionId);
        assertOk(execRes);
        String projectId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(execRes.getBody()), "data.projectId"));

        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/operator-reviews?status=OPEN&page=1&pageSize=20");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListProjectReviewsFilterBySeverity() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "HIGH",
                "title", "严重级别过滤测试"
        ));

        ResponseEntity<String> execRes = get("/api/tool-sandbox-executions/" + executionId);
        assertOk(execRes);
        String projectId = Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(execRes.getBody()), "data.projectId"));

        ResponseEntity<String> listRes = get("/api/projects/" + projectId + "/operator-reviews?severity=HIGH&page=1&pageSize=20");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListProjectReviewsEmpty() {
        // Use a fresh project with no reviews
        String pid = createProject("NoReview-" + System.currentTimeMillis());

        ResponseEntity<String> listRes = get("/api/projects/" + pid + "/operator-reviews?page=1&pageSize=20");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isEqualTo(0);
    }

    // ========================
    // List Target Reviews
    // ========================

    @Test
    void shouldListTargetReviews() {
        String[] runInfo = freshCompletedRun();
        String executionId = getFirstExecutionId(runInfo[0]);

        // Create a review
        post("/api/orchestration/operator-reviews", Map.of(
                "reviewTargetType", "TOOL_EXECUTION",
                "reviewTargetId", executionId,
                "severity", "MEDIUM",
                "title", "目标查询测试"
        ));

        ResponseEntity<String> listRes = get(
                "/api/orchestration/operator-reviews/by-target?targetType=TOOL_EXECUTION&targetId=" + executionId);
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListTargetReviewsEmpty() {
        ResponseEntity<String> listRes = get(
                "/api/orchestration/operator-reviews/by-target?targetType=TOOL_EXECUTION&targetId=1");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data).isEmpty();
    }

    @Test
    void shouldListTargetReviewsInvalidTargetType() {
        ResponseEntity<String> res = get(
                "/api/orchestration/operator-reviews/by-target?targetType=INVALID&targetId=1");
        assertCode(res, "VALIDATION_ERROR");
    }
}
