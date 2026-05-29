package com.aicoding.platform.orchestration;

import com.aicoding.platform.github.domain.PrReviewJobEntity;
import com.aicoding.platform.github.infrastructure.PrReviewJobMapper;
import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.domain.ModelCostSummaryEntity;
import com.aicoding.platform.orchestration.domain.PrReviewQualityRecordEntity;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
import com.aicoding.platform.orchestration.infrastructure.ModelCostSummaryMapper;
import com.aicoding.platform.orchestration.infrastructure.PrReviewQualityRecordMapper;
import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCostAndQualityExportIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ModelCostSummaryMapper modelCostSummaryMapper;

    @Autowired
    private ModelCostAlertMapper modelCostAlertMapper;

    @Autowired
    private PrReviewQualityRecordMapper prReviewQualityRecordMapper;

    @Autowired
    private PrReviewJobMapper prReviewJobMapper;

    private String projectIdValue;

    @BeforeEach
    public void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Export-" + suffix,
                "description", "Export integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Seed model cost summary data
        Long projectId = Long.valueOf(projectIdValue);
        ModelCostSummaryEntity summary = new ModelCostSummaryEntity();
        summary.setProjectId(projectId);
        summary.setProvider("openai");
        summary.setModelName("gpt-4");
        summary.setRequestType("CHAT");
        summary.setStatDate(LocalDate.now().minusDays(1));
        summary.setRequestCount(100L);
        summary.setSuccessCount(95L);
        summary.setFailureCount(5L);
        summary.setFallbackCount(2L);
        summary.setPromptTokens(50000L);
        summary.setCompletionTokens(100000L);
        summary.setTotalTokens(150000L);
        summary.setEstimatedCost(BigDecimal.valueOf(2.5));
        summary.setAvgLatencyMs(800L);
        modelCostSummaryMapper.insert(summary);

        // Seed alert
        ModelCostAlertEntity alert = new ModelCostAlertEntity();
        alert.setProjectId(projectId);
        alert.setProvider("openai");
        alert.setModelName("gpt-4");
        alert.setAlertType("DAILY_COST_SPIKE");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setSummary("Cost spike detected");
        alert.setStatDate(LocalDate.now().minusDays(1));
        alert.setThresholdValue(BigDecimal.valueOf(1.0));
        alert.setActualValue(BigDecimal.valueOf(2.5));
        modelCostAlertMapper.insert(alert);

        // Seed quality record - use projectId as unique reviewJobId
        PrReviewQualityRecordEntity record = new PrReviewQualityRecordEntity();
        record.setProjectId(projectId);
        record.setReviewJobId(projectId);
        record.setRepositoryFullName("test/repo");
        record.setPullRequestNumber(42L);
        record.setFindingsTotal(5);
        record.setHighRiskFindings(1);
        record.setMediumRiskFindings(2);
        record.setLowRiskFindings(2);
        record.setReviewStatus("HIGH_VALUE");
        record.setHumanFeedbackStatus("PENDING");
        record.setAdoptionStatus("UNKNOWN");
        prReviewQualityRecordMapper.insert(record);
    }

    @Test
    void shouldExportModelCostReport() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/export/model-cost-report?startDate=2026-01-01&endDate=2026-12-31");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "content")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "fileName")).contains("model-cost-report");
    }

    @Test
    void shouldExportPrReviewQualityReport() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/export/pr-review-quality-report");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "content")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "fileName")).contains("pr-review-quality-report");
    }

    @Test
    void shouldRefreshAndScanModelCostForExistingProject() {
        ResponseEntity<String> refreshRes = post("/api/projects/" + projectIdValue + "/model-cost/refresh", Map.of());
        assertOk(refreshRes);

        ResponseEntity<String> scanRes = post("/api/projects/" + projectIdValue + "/model-cost/alerts/scan", Map.of());
        assertOk(scanRes);
    }

    @Test
    void shouldGetDashboardWithSeededData() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/model-cost/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("totalCostToday")).isTrue();
        assertThat(data.has("recentAlerts")).isTrue();
    }

    @Test
    void shouldGetQualityDashboardWithSeededData() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/pr-review-quality/dashboard");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.has("totalReviews")).isTrue();
    }

    @Test
    void shouldUpdateAlertStatusToResolved() {
        Long projectId = Long.valueOf(projectIdValue);
        ModelCostAlertEntity alert = new ModelCostAlertEntity();
        alert.setProjectId(projectId);
        alert.setProvider("anthropic");
        alert.setModelName("claude-opus-4");
        alert.setAlertType("HIGH_FALLBACK_RATE");
        alert.setSeverity("MEDIUM");
        alert.setStatus("OPEN");
        alert.setSummary("Fallback rate alert");
        alert.setStatDate(LocalDate.now());
        modelCostAlertMapper.insert(alert);

        ResponseEntity<String> res = put("/api/model-cost/alerts/" + alert.getId() + "/status?status=RESOLVED", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("RESOLVED");
    }

    @Test
    void shouldCreateAndQueryQualityRecordList() {
        Long projectId = Long.valueOf(projectIdValue);
        PrReviewJobEntity job = new PrReviewJobEntity();
        job.setProjectId(projectId);
        job.setPullRequestId(1L);
        job.setStatus("COMPLETED");
        job.setModelProvider("openai");
        job.setModelName("gpt-4");
        job.setCreatorId(1L);
        prReviewJobMapper.insert(job);

        ResponseEntity<String> createRes = post("/api/projects/" + projectIdValue + "/pr-review-quality/records",
                Map.of("reviewJobId", job.getId().toString(),
                        "usefulnessScore", 3,
                        "falsePositiveScore", 2));
        assertOk(createRes);

        ResponseEntity<String> listRes = get("/api/projects/" + projectIdValue + "/pr-review-quality/records");
        assertOk(listRes);
        JsonNode data = TestJsonHelper.parse(listRes.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
    }

    @Test
    void shouldTransitionAlertThroughAllStatuses() {
        Long projectId = Long.valueOf(projectIdValue);
        ModelCostAlertEntity alert = new ModelCostAlertEntity();
        alert.setProjectId(projectId);
        alert.setProvider("openai");
        alert.setModelName("gpt-4o");
        alert.setAlertType("LATENCY_COST_ANOMALY");
        alert.setSeverity("LOW");
        alert.setStatus("OPEN");
        alert.setSummary("Latency anomaly");
        alert.setStatDate(LocalDate.now());
        modelCostAlertMapper.insert(alert);

        String alertIdStr = alert.getId().toString();

        // OPEN -> ACKNOWLEDGED
        ResponseEntity<String> res1 = put("/api/model-cost/alerts/" + alertIdStr + "/status?status=ACKNOWLEDGED", Map.of());
        assertOk(res1);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res1.getBody()).get("data"), "status")).isEqualTo("ACKNOWLEDGED");

        // ACKNOWLEDGED -> RESOLVED
        ResponseEntity<String> res2 = put("/api/model-cost/alerts/" + alertIdStr + "/status?status=RESOLVED", Map.of());
        assertOk(res2);
        assertThat(TestJsonHelper.getString(TestJsonHelper.parse(res2.getBody()).get("data"), "status")).isEqualTo("RESOLVED");
    }
}
