package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowTemplateIntegrationTest extends IntegrationTestBase {

    private volatile String cachedTemplateId;

    private @NonNull String templateId() {
        if (cachedTemplateId == null) {
            ResponseEntity<String> listRes = get("/api/workflow-templates");
            assertOk(listRes);
            JsonNode dataArray = TestJsonHelper.parse(listRes.getBody()).get("data");
            assertThat(dataArray).isNotNull();
            assertThat(dataArray.isArray()).isTrue();
            for (JsonNode node : dataArray) {
                if ("STANDARD_DELIVERY".equals(TestJsonHelper.getString(node, "templateKey"))) {
                    cachedTemplateId = TestJsonHelper.getString(node, "id");
                    break;
                }
            }
            assertThat(cachedTemplateId).isNotNull();
        }
        return Objects.requireNonNull(cachedTemplateId);
    }

    // ========================
    // 1. Seed data
    // ========================

    @Test
    @Order(1)
    void shouldHaveFourBuiltInTemplatesAfterSeed() {
        ResponseEntity<String> res = get("/api/workflow-templates");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isEqualTo(4);

        boolean hasStandard = false, hasBackend = false, hasFrontend = false, hasReview = false;
        for (JsonNode tpl : dataArray) {
            String key = TestJsonHelper.getString(tpl, "templateKey");
            if ("STANDARD_DELIVERY".equals(key)) hasStandard = true;
            if ("BACKEND_FOCUSED".equals(key)) hasBackend = true;
            if ("FRONTEND_FOCUSED".equals(key)) hasFrontend = true;
            if ("REVIEW_ONLY".equals(key)) hasReview = true;
            assertThat(TestJsonHelper.getString(tpl, "status")).isEqualTo("ENABLED");
            assertThat(tpl.get("builtIn").asBoolean()).isTrue();
        }
        assertThat(hasStandard && hasBackend && hasFrontend && hasReview).isTrue();
    }

    // ========================
    // 2. listStrategies from DB
    // ========================

    @Test
    @Order(2)
    void shouldListMultiAgentStrategiesFromDatabase() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isEqualTo(4);

        boolean hasStandard = false;
        for (JsonNode s : dataArray) {
            if ("STANDARD_DELIVERY".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                hasStandard = true;
                assertThat(TestJsonHelper.getInt(s, "phaseCount")).isGreaterThan(0);
                assertThat(TestJsonHelper.getInt(s, "stepCount")).isGreaterThan(0);
            }
        }
        assertThat(hasStandard).isTrue();
    }

    // ========================
    // 3. Admin template list
    // ========================

    @Test
    @Order(3)
    void shouldFilterTemplatesByStatus() {
        ResponseEntity<String> res = get("/api/workflow-templates?status=ENABLED");
        assertOk(res);
        JsonNode enabledArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(enabledArray.size()).isEqualTo(4);

        ResponseEntity<String> res2 = get("/api/workflow-templates?status=DISABLED");
        assertOk(res2);
        JsonNode disabledArray = TestJsonHelper.parse(res2.getBody()).get("data");
        assertThat(disabledArray.size()).isEqualTo(0);
    }

    // ========================
    // 4. Admin template detail
    // ========================

    @Test
    @Order(4)
    void shouldGetTemplateDetail() {
        ResponseEntity<String> res = get("/api/workflow-templates/" + templateId());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        assertThat(TestJsonHelper.getString(data, "templateKey")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "name")).isEqualTo("标准交付流程");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("ENABLED");
        assertThat(data.get("builtIn").asBoolean()).isTrue();
        assertThat(TestJsonHelper.getString(data, "templateJson")).isNotEmpty();

        JsonNode strategy = data.get("strategy");
        assertThat(strategy).isNotNull();
        assertThat(TestJsonHelper.getString(strategy, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getInt(strategy, "phaseCount")).isEqualTo(4);
        assertThat(TestJsonHelper.getInt(data, "phaseCount")).isEqualTo(4);
        assertThat(TestJsonHelper.getInt(data, "stepCount")).isGreaterThan(0);
    }

    // ========================
    // 8. Access control
    // ========================

    @Test
    @Order(5)
    void shouldRejectUnauthenticatedAccess() {
        ResponseEntity<String> res = getNoAuth("/api/workflow-templates");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    @Order(6)
    void shouldRejectUnauthenticatedDetailAccess() {
        ResponseEntity<String> res = getNoAuth("/api/workflow-templates/" + templateId());
        assertCode(res, "UNAUTHORIZED");
    }

    // ========================
    // 9. Legacy DEFAULT_MOCK
    // ========================

    @Test
    @Order(7)
    void shouldResolveLegacyDefaultMockToEnabledTemplate() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> projRes = post("/api/projects", Map.of(
                "name", "IT-Legacy-" + suffix,
                "description", "Legacy test",
                "techStack", List.of()));
        assertOk(projRes);
        String pid = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-Legacy-Task-" + suffix,
                "description", "Test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID));
        assertOk(taskRes);
        String taskId = TestJsonHelper.getString(
                TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> runRes = post("/api/tasks/" + taskId + "/multi-agent-runs",
                Map.of("strategy", "DEFAULT_MOCK", "instruction", "test"));
        assertOk(runRes);
        JsonNode data = TestJsonHelper.parse(runRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "strategy")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
    }

    // ========================
    // 10. Strategy list valid data
    // ========================

    @Test
    @Order(8)
    void shouldReturnValidPhasesAndStepCountsInStrategies() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        for (JsonNode strategy : dataArray) {
            String key = TestJsonHelper.getString(strategy, "strategyKey");
            assertThat(key).isNotEmpty();
            assertThat(TestJsonHelper.getString(strategy, "name")).isNotEmpty();
            assertThat(TestJsonHelper.getInt(strategy, "phaseCount")).isGreaterThan(0);
            assertThat(TestJsonHelper.getInt(strategy, "stepCount")).isGreaterThan(0);
            assertThat(strategy.get("phases")).isNotNull();
            assertThat(strategy.get("phases").isArray()).isTrue();
            assertThat(strategy.get("phases").size()).isGreaterThan(0);
        }
    }

    // ========================
    // 6. Reject invalid status
    // ========================

    @Test
    @Order(9)
    void shouldRejectInvalidStatusValue() {
        ResponseEntity<String> res = put("/api/workflow-templates/" + templateId() + "/status",
                Map.of("status", "ARCHIVED"));
        assertCode(res, "BAD_REQUEST");
    }

    // ========================
    // 7. Not found
    // ========================

    @Test
    @Order(10)
    void shouldReturnNotFoundForMissingTemplate() {
        ResponseEntity<String> res = get("/api/workflow-templates/999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 11. Not found for status update
    // ========================

    @Test
    @Order(11)
    void shouldReturnNotFoundForTemplateStatusUpdateWithBadId() {
        ResponseEntity<String> res = put("/api/workflow-templates/999999/status",
                Map.of("status", "DISABLED"));
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 5. Disable / re-enable (runs last — mutates shared state)
    // ========================

    @Test
    @Order(12)
    void shouldDisableTemplateAndHideFromStrategies() {
        String tid = templateId();

        // Disable STANDARD_DELIVERY
        ResponseEntity<String> res = put("/api/workflow-templates/" + tid + "/status",
                Map.of("status", "DISABLED"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("DISABLED");

        // Verify it no longer appears in strategies
        ResponseEntity<String> strategiesRes = get("/api/multi-agent-strategies");
        assertOk(strategiesRes);
        JsonNode stratArray = TestJsonHelper.parse(strategiesRes.getBody()).get("data");
        for (JsonNode s : stratArray) {
            assertThat(TestJsonHelper.getString(s, "strategyKey")).isNotEqualTo("STANDARD_DELIVERY");
        }

        // Starting run with disabled strategy should fail
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> projRes = post("/api/projects", Map.of(
                "name", "IT-WFT-" + suffix,
                "description", "WFT integration test",
                "techStack", List.of()));
        assertOk(projRes);
        String pid = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-WFT-Task-" + suffix,
                "description", "Test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID));
        assertOk(taskRes);
        String taskId = TestJsonHelper.getString(
                TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> runRes = post("/api/tasks/" + taskId + "/multi-agent-runs",
                Map.of("strategy", "STANDARD_DELIVERY", "instruction", "test"));
        assertCode(runRes, "BAD_REQUEST");

        // Re-enable
        ResponseEntity<String> enableRes = put("/api/workflow-templates/" + tid + "/status",
                Map.of("status", "ENABLED"));
        assertOk(enableRes);
        assertThat(TestJsonHelper.getString(
                TestJsonHelper.parse(enableRes.getBody()), "data.status")).isEqualTo("ENABLED");

        // Now it should appear in strategies again
        ResponseEntity<String> strategiesRes2 = get("/api/multi-agent-strategies");
        assertOk(strategiesRes2);
        JsonNode stratArray2 = TestJsonHelper.parse(strategiesRes2.getBody()).get("data");
        boolean found = false;
        for (JsonNode s : stratArray2) {
            if ("STANDARD_DELIVERY".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
