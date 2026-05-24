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

class ToolIncidentWorkflowIntegrationTest extends IntegrationTestBase {

    // ========================
    // Helpers
    // ========================

    private String createProject(String suffix) {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Incident-" + suffix,
                "description", "Incident test project",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String createManualIncident(String projectId, String title, String severity) {
        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", projectId,
                "sourceType", "MANUAL",
                "severity", severity,
                "title", title
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    private String createAlertRule(String projectId) {
        ResponseEntity<String> res = post("/api/orchestration/alert-rules", Map.of(
                "projectId", projectId,
                "name", "Test Rule - " + System.currentTimeMillis(),
                "sourceType", "MANUAL",
                "minSeverity", "LOW",
                "channel", "IN_APP"
        ));
        assertOk(res);
        return Objects.requireNonNull(TestJsonHelper.getString(
                TestJsonHelper.parse(res.getBody()), "data.id"));
    }

    // ========================
    // Create Incident
    // ========================

    @Test
    void shouldCreateIncident() {
        String pid = createProject("Create");

        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", pid,
                "sourceType", "MANUAL",
                "severity", "HIGH",
                "title", "测试事件"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "sourceType")).isEqualTo("MANUAL");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("HIGH");
        assertThat(TestJsonHelper.getString(data, "title")).isEqualTo("测试事件");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("OPEN");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
    }

    @Test
    void shouldCreateIncidentWithAllOptionalFields() {
        String pid = createProject("AllFields");

        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", pid,
                "sourceType", "MANUAL",
                "severity", "CRITICAL",
                "title", "完整字段测试",
                "summary", "这是一个完整的测试事件",
                "assigneeId", "100001"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("CRITICAL");
        assertThat(TestJsonHelper.getString(data, "summary")).isEqualTo("这是一个完整的测试事件");
        assertThat(TestJsonHelper.getString(data, "assigneeId")).isEqualTo("100001");
    }

    @Test
    void shouldCreateIncidentAllSeverities() {
        String pid = createProject("Sev");
        String[] severities = {"INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL"};

        for (String sev : severities) {
            ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                    "projectId", pid,
                    "sourceType", "MANUAL",
                    "severity", sev,
                    "title", "测试级别: " + sev
            ));
            assertOk(res);
        }
    }

    @Test
    void shouldCreateIncidentAllSourceTypes() {
        String pid = createProject("ST");
        String[] sourceTypes = {"TOOL_EXECUTION_FAILED", "TOOL_JOB_FAILED", "TOOL_JOB_RETRY_PENDING",
                "TOOL_JOB_DEAD_LETTERED", "READ_ONLY_CONTRACT_WARNING", "TRACE_OUTPUT_PARSE_WARNING",
                "HIGH_RISK_REVIEW", "OPERATOR_REVIEW", "MANUAL"};

        for (String st : sourceTypes) {
            ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                    "projectId", pid,
                    "sourceType", st,
                    "severity", "LOW",
                    "title", "来源: " + st
            ));
            assertOk(res);
        }
    }

    // ========================
    // Create Incident - Validation
    // ========================

    @Test
    void shouldCreateIncidentValidationErrorMissingTitle() {
        String pid = createProject("NoTitle");

        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", pid,
                "sourceType", "MANUAL",
                "severity", "MEDIUM",
                "title", ""
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateIncidentValidationErrorInvalidSeverity() {
        String pid = createProject("BadSev");

        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", pid,
                "sourceType", "MANUAL",
                "severity", "INVALID_SEV",
                "title", "test"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateIncidentValidationErrorInvalidSourceType() {
        String pid = createProject("BadST");

        ResponseEntity<String> res = post("/api/orchestration/incidents", Map.of(
                "projectId", pid,
                "sourceType", "INVALID_SOURCE",
                "severity", "MEDIUM",
                "title", "test"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    // ========================
    // Get Incident
    // ========================

    @Test
    void shouldGetIncident() {
        String pid = createProject("Get");
        String incidentId = createManualIncident(pid, "获取测试", "MEDIUM");

        ResponseEntity<String> res = get("/api/orchestration/incidents/" + incidentId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(incidentId);
        assertThat(TestJsonHelper.getString(data, "title")).isEqualTo("获取测试");
    }

    @Test
    void shouldGetIncidentNotFound() {
        ResponseEntity<String> res = get("/api/orchestration/incidents/99999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // Update Incident - Status Transitions
    // ========================

    @Test
    void shouldUpdateIncidentAcknowledge() {
        String pid = createProject("Ack");
        String incidentId = createManualIncident(pid, "确认测试", "MEDIUM");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "ACKNOWLEDGED"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("ACKNOWLEDGED");
        assertThat(TestJsonHelper.getString(data, "acknowledgedAt")).isNotEmpty();
    }

    @Test
    void shouldUpdateIncidentResolve() {
        String pid = createProject("Resolve");
        String incidentId = createManualIncident(pid, "解决测试", "MEDIUM");

        put("/api/orchestration/incidents/" + incidentId, Map.of("status", "ACKNOWLEDGED"));

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "RESOLVED",
                "resolution", "已修复"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("RESOLVED");
        assertThat(TestJsonHelper.getString(data, "resolution")).isEqualTo("已修复");
    }

    @Test
    void shouldUpdateIncidentReopen() {
        String pid = createProject("Reopen");
        String incidentId = createManualIncident(pid, "重新打开测试", "MEDIUM");

        put("/api/orchestration/incidents/" + incidentId, Map.of("status", "ACKNOWLEDGED"));
        put("/api/orchestration/incidents/" + incidentId, Map.of("status", "RESOLVED"));

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "OPEN"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("OPEN");
        // Re-open clears resolved info
        assertThat(TestJsonHelper.getString(data, "resolvedBy")).isEmpty();
        assertThat(TestJsonHelper.getString(data, "resolvedAt")).isEmpty();
        assertThat(TestJsonHelper.getString(data, "acknowledgedBy")).isEmpty();
        assertThat(TestJsonHelper.getString(data, "acknowledgedAt")).isEmpty();
    }

    @Test
    void shouldUpdateIncidentWontFix() {
        String pid = createProject("WontFix");
        String incidentId = createManualIncident(pid, "不修复测试", "LOW");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "WONT_FIX"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("WONT_FIX");
        assertThat(TestJsonHelper.getString(data, "resolvedAt")).isNotEmpty();
    }

    @Test
    void shouldUpdateIncidentFalsePositive() {
        String pid = createProject("FP");
        String incidentId = createManualIncident(pid, "误报测试", "LOW");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "FALSE_POSITIVE"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("FALSE_POSITIVE");
    }

    // ========================
    // Update Incident - Validation
    // ========================

    @Test
    void shouldUpdateIncidentNotFound() {
        ResponseEntity<String> res = put("/api/orchestration/incidents/99999999", Map.of(
                "status", "RESOLVED"
        ));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldUpdateIncidentInvalidStatus() {
        String pid = createProject("BadStatus");
        String incidentId = createManualIncident(pid, "无效状态测试", "MEDIUM");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "INVALID_STATUS"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldUpdateIncidentInvalidTransition() {
        String pid = createProject("BadTrans");
        String incidentId = createManualIncident(pid, "无效转换测试", "MEDIUM");

        // OPEN -> RESOLVED is valid (OPEN can go to any status)
        put("/api/orchestration/incidents/" + incidentId, Map.of("status", "RESOLVED"));

        // RESOLVED -> ACKNOWLEDGED is invalid (terminal -> only OPEN)
        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "status", "ACKNOWLEDGED"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldUpdateIncidentSeverity() {
        String pid = createProject("UpdSev");
        String incidentId = createManualIncident(pid, "级别更新测试", "LOW");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "severity", "CRITICAL"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "severity")).isEqualTo("CRITICAL");
    }

    @Test
    void shouldUpdateIncidentTitleAndSummary() {
        String pid = createProject("UpdTitle");
        String incidentId = createManualIncident(pid, "原标题", "MEDIUM");

        ResponseEntity<String> res = put("/api/orchestration/incidents/" + incidentId, Map.of(
                "title", "新标题",
                "summary", "新摘要"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "title")).isEqualTo("新标题");
        assertThat(TestJsonHelper.getString(data, "summary")).isEqualTo("新摘要");
    }

    // ========================
    // List Project Incidents
    // ========================

    @Test
    void shouldListProjectIncidents() {
        String pid = createProject("List");
        createManualIncident(pid, "列表测试", "MEDIUM");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incidents?page=1&pageSize=20");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("records")).isTrue();
        assertThat(data.has("total")).isTrue();
    }

    @Test
    void shouldListProjectIncidentsFilterByStatus() {
        String pid = createProject("ListStatus");
        createManualIncident(pid, "状态过滤", "HIGH");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incidents?status=OPEN&page=1&pageSize=20");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListProjectIncidentsFilterBySeverity() {
        String pid = createProject("ListSev");
        createManualIncident(pid, "级别过滤", "HIGH");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incidents?severity=HIGH&page=1&pageSize=20");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListProjectIncidentsEmpty() {
        String pid = createProject("NoIncident");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incidents?page=1&pageSize=20");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "total")).isEqualTo(0);
    }

    // ========================
    // Incident Summary
    // ========================

    @Test
    void shouldGetIncidentSummary() {
        String pid = createProject("Summary");
        createManualIncident(pid, "摘要1", "HIGH");
        createManualIncident(pid, "摘要2", "CRITICAL");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/incidents/summary");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getLong(data, "openCount")).isGreaterThanOrEqualTo(2);
        assertThat(TestJsonHelper.getLong(data, "criticalCount")).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getLong(data, "highCount")).isGreaterThanOrEqualTo(1);
    }

    // ========================
    // Alert Rule CRUD
    // ========================

    @Test
    void shouldCreateAlertRule() {
        String pid = createProject("Rule");

        ResponseEntity<String> res = post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "测试规则",
                "sourceType", "MANUAL",
                "minSeverity", "MEDIUM",
                "channel", "IN_APP"
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "name")).isEqualTo("测试规则");
        assertThat(TestJsonHelper.getString(data, "enabled")).isEqualTo("true");
        assertThat(TestJsonHelper.getString(data, "channel")).isEqualTo("IN_APP");
    }

    @Test
    void shouldCreateAlertRuleValidationError() {
        String pid = createProject("RuleBad");

        ResponseEntity<String> res = post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "",
                "sourceType", "MANUAL",
                "minSeverity", "MEDIUM",
                "channel", "IN_APP"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateAlertRuleInvalidChannel() {
        String pid = createProject("RuleChan");

        ResponseEntity<String> res = post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "坏通道规则",
                "sourceType", "MANUAL",
                "minSeverity", "MEDIUM",
                "channel", "INVALID_CHANNEL"
        ));
        assertCode(res, "VALIDATION_ERROR");
    }

    @Test
    void shouldCreateAlertRuleAllChannels() {
        String pid = createProject("Chan");
        String[] channels = {"IN_APP", "MOCK_WEBHOOK", "MOCK_SLACK", "MOCK_EMAIL"};

        for (String ch : channels) {
            ResponseEntity<String> res = post("/api/orchestration/alert-rules", Map.of(
                    "projectId", pid,
                    "name", "通道: " + ch,
                    "sourceType", "MANUAL",
                    "minSeverity", "LOW",
                    "channel", ch
            ));
            assertOk(res);
        }
    }

    @Test
    void shouldUpdateAlertRule() {
        String pid = createProject("UpdRule");
        String ruleId = createAlertRule(pid);

        ResponseEntity<String> res = put("/api/orchestration/alert-rules/" + ruleId, Map.of(
                "name", "更新后的规则",
                "enabled", false
        ));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "name")).isEqualTo("更新后的规则");
        assertThat(TestJsonHelper.getString(data, "enabled")).isEqualTo("false");
    }

    @Test
    void shouldUpdateAlertRuleNotFound() {
        ResponseEntity<String> res = put("/api/orchestration/alert-rules/99999999", Map.of(
                "name", "不存在的规则"
        ));
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldListProjectAlertRules() {
        String pid = createProject("ListRule");
        createAlertRule(pid);

        ResponseEntity<String> res = get("/api/projects/" + pid + "/alert-rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldListProjectAlertRulesEmpty() {
        String pid = createProject("NoRules");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/alert-rules");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data).isEmpty();
    }

    // ========================
    // Alert Delivery + Routing
    // ========================

    @Test
    void shouldCreateIncidentWithMatchingRuleTriggersDelivery() {
        String pid = createProject("Route");
        // Create matching alert rule
        post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "匹配规则",
                "sourceType", "MANUAL",
                "minSeverity", "LOW",
                "channel", "IN_APP"
        ));

        // Create incident - should trigger delivery
        String incidentId = createManualIncident(pid, "路由测试", "HIGH");

        // Verify delivery was created
        ResponseEntity<String> delRes = get("/api/orchestration/incidents/" + incidentId + "/alert-deliveries");
        assertOk(delRes);
        JsonNode data = TestJsonHelper.parse(delRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getString(data.get(0), "status")).isEqualTo("DELIVERED");
        assertThat(TestJsonHelper.getString(data.get(0), "channel")).isEqualTo("IN_APP");
    }

    @Test
    void shouldCreateIncidentWithoutMatchingRuleNoDelivery() {
        String pid = createProject("NoRoute");

        // Create incident with CRITICAL severity but no alert rule exists
        String incidentId = createManualIncident(pid, "无路由", "CRITICAL");

        ResponseEntity<String> delRes = get("/api/orchestration/incidents/" + incidentId + "/alert-deliveries");
        assertOk(delRes);
        JsonNode data = TestJsonHelper.parse(delRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data).isEmpty();
    }

    @Test
    void shouldIncidentDeliveryWithMismatchedSeverityNoDelivery() {
        String pid = createProject("MisSev");
        // Rule requires CRITICAL severity
        post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "仅严重规则",
                "sourceType", "MANUAL",
                "minSeverity", "CRITICAL",
                "channel", "IN_APP"
        ));

        // Incident with LOW severity - should NOT match
        String incidentId = createManualIncident(pid, "级别不匹配", "LOW");

        ResponseEntity<String> delRes = get("/api/orchestration/incidents/" + incidentId + "/alert-deliveries");
        assertOk(delRes);
        JsonNode data = TestJsonHelper.parse(delRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data).isEmpty();
    }

    @Test
    void shouldListProjectAlertDeliveries() {
        String pid = createProject("ListDel");
        post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "投递规则",
                "sourceType", "MANUAL",
                "minSeverity", "LOW",
                "channel", "IN_APP"
        ));
        createManualIncident(pid, "投递测试", "MEDIUM");

        ResponseEntity<String> res = get("/api/projects/" + pid + "/alert-deliveries");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRetryAlertDelivery() {
        String pid = createProject("RetryDel");
        post("/api/orchestration/alert-rules", Map.of(
                "projectId", pid,
                "name", "重试规则",
                "sourceType", "MANUAL",
                "minSeverity", "LOW",
                "channel", "MOCK_WEBHOOK"
        ));
        String incidentId = createManualIncident(pid, "重试测试", "MEDIUM");

        ResponseEntity<String> delRes = get("/api/orchestration/incidents/" + incidentId + "/alert-deliveries");
        assertOk(delRes);
        JsonNode deliveries = TestJsonHelper.parse(delRes.getBody()).get("data");
        assertThat(deliveries.isArray()).isTrue();

        if (deliveries.size() > 0) {
            String deliveryId = Objects.requireNonNull(TestJsonHelper.getString(deliveries.get(0), "id"));

            ResponseEntity<String> retryRes = post("/api/orchestration/alert-deliveries/" + deliveryId + "/retry", Map.of());
            assertOk(retryRes);
            JsonNode retryData = TestJsonHelper.parse(retryRes.getBody()).get("data");
            assertThat(TestJsonHelper.getString(retryData, "status")).isEqualTo("DELIVERED");
        }
    }

    // ========================
    // Sync Problem Jobs
    // ========================

    @Test
    void shouldSyncProblemJobsEmpty() {
        String pid = createProject("SyncEmpty");

        ResponseEntity<String> res = post("/api/projects/" + pid + "/incidents/sync-problem-jobs", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getInt(data, "created")).isEqualTo(0);
        assertThat(TestJsonHelper.getInt(data, "updated")).isEqualTo(0);
        assertThat(TestJsonHelper.getInt(data, "skipped")).isEqualTo(0);
    }
}
