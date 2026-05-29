package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
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

class ModelCostAlertIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ModelCostAlertMapper modelCostAlertMapper;

    private String projectIdValue;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-CostAlert-" + suffix,
                "description", "Model cost alert integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Clean any existing alerts for this project
        Long projectId = Long.parseLong(projectIdValue);
        List<ModelCostAlertEntity> existing = modelCostAlertMapper.selectList(null);
        for (ModelCostAlertEntity e : existing) {
            if (projectId.equals(e.getProjectId())) {
                modelCostAlertMapper.deleteById(e);
            }
        }
    }

    @Test
    void shouldScanAlertsWithNoData() {
        ResponseEntity<String> res = post("/api/projects/" + projectIdValue + "/model-cost/alerts/scan", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
    }

    @Test
    void shouldListAlerts() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue + "/model-cost/alerts");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data).isNotNull();
    }

    @Test
    void shouldListAlertsWithPagination() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/alerts?page=1&size=10");
        assertOk(res);
    }

    @Test
    void shouldListAlertsWithStatusFilter() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/alerts?status=OPEN");
        assertOk(res);
    }

    @Test
    void shouldListAlertsWithSeverityFilter() {
        ResponseEntity<String> res = get("/api/projects/" + projectIdValue
                + "/model-cost/alerts?severity=HIGH");
        assertOk(res);
    }

    @Test
    void shouldUpdateAlertStatus() {
        // Create an alert directly
        ModelCostAlertEntity alert = new ModelCostAlertEntity();
        alert.setProjectId(Long.parseLong(projectIdValue));
        alert.setProvider("openai");
        alert.setModelName("gpt-4");
        alert.setAlertType("DAILY_COST_SPIKE");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setSummary("Test alert for status update");
        alert.setStatDate(LocalDate.now());
        alert.setThresholdValue(BigDecimal.valueOf(100));
        alert.setActualValue(BigDecimal.valueOf(200));
        modelCostAlertMapper.insert(alert);

        String alertId = alert.getId().toString();
        ResponseEntity<String> res = put("/api/model-cost/alerts/" + alertId + "/status?status=ACKNOWLEDGED", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAlert() {
        ResponseEntity<String> res = put("/api/model-cost/alerts/999999999/status?status=ACKNOWLEDGED", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldHandleInvalidAlertId() {
        ResponseEntity<String> res = put("/api/model-cost/alerts/invalid/status?status=ACKNOWLEDGED", Map.of());
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldHandleInvalidProjectIdForList() {
        ResponseEntity<String> res = get("/api/projects/invalid/model-cost/alerts");
        assertCode(res, "BAD_REQUEST");
    }
}
