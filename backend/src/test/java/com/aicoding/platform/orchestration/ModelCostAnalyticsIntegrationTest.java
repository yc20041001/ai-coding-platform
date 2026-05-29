package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestrator.domain.ModelRequestLogEntity;
import com.aicoding.platform.orchestrator.infrastructure.ModelRequestLogMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCostAnalyticsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ModelRequestLogMapper modelRequestLogMapper;

    private String projectIdValue;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-CostAnalytics-" + suffix,
                "description", "Model cost analytics integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");
    }

    @Test
    void shouldRefreshDailySummariesWithNoData() {
        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/model-cost/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldRefreshDailySummariesWithData() {
        // Insert test model_request_log records directly
        Long projectId = Long.valueOf(projectIdValue);
        for (int i = 0; i < 5; i++) {
            ModelRequestLogEntity log = new ModelRequestLogEntity();
            log.setProjectId(projectId);
            log.setProvider("openai");
            log.setModelName("gpt-4");
            log.setRequestType("CHAT");
            log.setPromptTokens(100L + i);
            log.setCompletionTokens(200L + i);
            log.setTotalTokens(300L + i);
            log.setLatencyMs(500L + i);
            log.setSuccess(true);
            log.setFallbackUsed(false);
            log.setEstimatedCost(BigDecimal.valueOf(0.01 * (i + 1)));
            log.setCreateTime(LocalDateTime.now().minusDays(1));
            modelRequestLogMapper.insert(log);
        }

        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/model-cost/refresh", Map.of());
        assertOk(res);
    }

    @Test
    void shouldListCostSummaries() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/model-cost/summaries");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
    }

    @Test
    void shouldGetCostTrend() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/model-cost/trend");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
    }

    @Test
    void shouldGetCostDashboard() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/model-cost/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
        assertThat(data.has("totalCostToday")).isTrue();
        assertThat(data.has("totalCostThisWeek")).isTrue();
        assertThat(data.has("totalCostThisMonth")).isTrue();
        assertThat(data.has("totalRequestsToday")).isTrue();
        assertThat(data.has("topModelsByCost")).isTrue();
        assertThat(data.has("recentAlerts")).isTrue();
    }

    @Test
    void shouldGetCostTrendWithDateRange() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/trend?startDate=2026-01-01&endDate=2026-12-31");
        assertOk(res);
    }

    @Test
    void shouldListSummariesWithPagination() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/summaries?page=1&size=10");
        assertOk(res);
    }

    @Test
    void shouldListSummariesWithFilters() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/summaries?provider=openai&modelName=gpt-4");
        assertOk(res);
    }

    @Test
    void shouldHandleInvalidProjectId() {
        ResponseEntity<String> res = get("/api/projects/invalid/model-cost/dashboard");
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldHandleNonExistentProject() {
        ResponseEntity<String> res = get("/api/projects/999999999/model-cost/dashboard");
        assertCode(res, "PROJECT_ACCESS_DENIED");
    }
}
