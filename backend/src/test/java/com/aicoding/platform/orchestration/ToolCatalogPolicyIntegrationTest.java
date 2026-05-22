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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolCatalogPolicyIntegrationTest extends IntegrationTestBase {

    private String projectId;
    private String taskId;
    private String runId;
    private String scanToolId; // PROJECT_CONTEXT_SCAN

    // ========================
    // Tool Catalog / Project Config (1-10)
    // ========================

    @Test
    @Order(1)
    void shouldReturnFiveToolsAfterSeed() {
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isEqualTo(13);

        boolean hasScan = false, hasAnalysis = false, hasInspection = false,
                hasTestPlan = false, hasSecurity = false, hasPatch = false;
        for (JsonNode t : dataArray) {
            String key = TestJsonHelper.getString(t, "toolKey");
            if ("PROJECT_CONTEXT_SCAN".equals(key)) {
                hasScan = true;
                scanToolId = TestJsonHelper.getString(t, "id");
            }
            if ("TASK_REQUIREMENT_ANALYSIS".equals(key)) hasAnalysis = true;
            if ("MOCK_FILE_INSPECTION".equals(key)) hasInspection = true;
            if ("MOCK_TEST_PLAN_SCAN".equals(key)) hasTestPlan = true;
            if ("MOCK_SECURITY_REVIEW".equals(key)) hasSecurity = true;
            if ("MOCK_PATCH_PROPOSAL".equals(key)) hasPatch = true;
        }
        assertThat(hasScan && hasAnalysis && hasInspection && hasTestPlan && hasSecurity && hasPatch).isTrue();
        assertThat(scanToolId).isNotNull();
    }

    @Test
    @Order(2)
    void shouldAllowAuthenticatedAccessToCatalog() {
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
    }

    @Test
    @Order(3)
    void shouldRejectUnauthenticatedCatalogAccess() {
        ResponseEntity<String> res = getNoAuth("/api/tool-catalog");
        assertCode(res, "UNAUTHORIZED");
    }

    @Test
    @Order(4)
    void shouldCreateProjectAndTaskForPolicyTests() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> projRes = post("/api/projects", Map.of(
                "name", "IT-TCP-" + suffix,
                "description", "Tool catalog policy integration test",
                "techStack", List.of("Java")
        ));
        assertOk(projRes);
        projectId = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");
        assertThat(projectId).isNotNull();

        // Enable agents for this project (required for multi-agent runs)
        enableAgent(projectId, "300001");
        enableAgent(projectId, "300002");
        enableAgent(projectId, "300003");
        enableAgent(projectId, "300004");
        enableAgent(projectId, "300005");

        ResponseEntity<String> taskRes = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-TCP-Task-" + suffix,
                "description", "Test task for tool policy",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        taskId = TestJsonHelper.getString(
                TestJsonHelper.parse(taskRes.getBody()), "data.id");
        assertThat(taskId).isNotNull();
    }

    @Test
    @Order(5)
    void shouldListProjectToolsForViewer() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/tools");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isEqualTo(11);

        for (JsonNode pt : dataArray) {
            String risk = TestJsonHelper.getString(pt, "riskLevel");
            boolean projEnabled = pt.get("projectEnabled").asBoolean();
            switch (risk) {
                case "LOW" ->
                    assertThat(projEnabled).as("LOW risk tool should be enabled by default").isTrue();
                case "MEDIUM" ->
                    assertThat(projEnabled).as("MEDIUM risk tool should be disabled by default").isFalse();
                case "HIGH" ->
                    assertThat(projEnabled).as("HIGH risk tool should be disabled by default").isFalse();
            }
        }
    }

    @Test
    @Order(6)
    void shouldEnableProjectToolByOwner() {
        // Find a MEDIUM tool ID
        ResponseEntity<String> catRes = get("/api/tool-catalog");
        assertOk(catRes);
        JsonNode catArray = TestJsonHelper.parse(catRes.getBody()).get("data");
        String mediumToolId = null;
        for (JsonNode t : catArray) {
            if ("MEDIUM".equals(TestJsonHelper.getString(t, "riskLevel"))) {
                mediumToolId = TestJsonHelper.getString(t, "id");
                break;
            }
        }
        assertThat(mediumToolId).isNotNull();

        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tools/" + mediumToolId + "/enable",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("projectEnabled").asBoolean()).isTrue();
    }

    @Test
    @Order(7)
    void shouldDisableProjectToolByOwner() {
        // Disable PROJECT_CONTEXT_SCAN
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tools/" + scanToolId + "/disable",
                Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.get("projectEnabled").asBoolean()).isFalse();
    }

    @Test
    @Order(8)
    void shouldReturnNotFoundForInvalidTool() {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tools/99999999/enable", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    @Order(9)
    void shouldReturnNotFoundForDisableInvalidTool() {
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tools/99999999/disable", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    @Order(10)
    void shouldRejectUnauthenticatedProjectToolsAccess() {
        ResponseEntity<String> res = getNoAuth("/api/projects/" + projectId + "/tools");
        assertCode(res, "UNAUTHORIZED");
    }

    // ========================
    // Policy / Sandbox (11-18)
    // ========================

    @Test
    @Order(11)
    void shouldStartMultiAgentRunForPolicyTest() {
        ResponseEntity<String> res = post("/api/tasks/" + taskId + "/multi-agent-runs",
                Map.of("strategy", "STANDARD_DELIVERY", "instruction", "test tool policy"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        runId = TestJsonHelper.getString(data, "id");
        assertThat(runId).isNotNull();
    }

    @Test
    @Order(12)
    void shouldContainToolExecutions() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isGreaterThan(0);
    }

    @Test
    @Order(13)
    void shouldGenerateBlockedExecutionsForDisabledTools() {
        // PROJECT_CONTEXT_SCAN was disabled in test 7, so ARCHITECTURE_ANALYSIS step should be blocked
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        boolean hasBlocked = false;
        for (JsonNode te : dataArray) {
            if ("BLOCKED".equals(TestJsonHelper.getString(te, "status"))) {
                hasBlocked = true;
                String output = TestJsonHelper.getString(te, "outputPayload");
                assertThat(output).contains("\"blocked\":true");
                assertThat(output).contains("\"filesTouched\":[]");
                assertThat(output).contains("\"gitOperations\":[]");
                assertThat(output).contains("\"mock\":true");
                assertThat(output).contains("\"readOnly\":true");
            }
        }
        assertThat(hasBlocked).isTrue();
    }

    @Test
    @Order(14)
    void shouldReEnableLowToolAndVerifyCompleted() {
        // Re-enable PROJECT_CONTEXT_SCAN
        ResponseEntity<String> enableRes = post("/api/projects/" + projectId + "/tools/" + scanToolId + "/enable",
                Map.of());
        assertOk(enableRes);
        assertThat(TestJsonHelper.parse(enableRes.getBody()).get("data").get("projectEnabled").asBoolean()).isTrue();

        // Start a new run — this time PROJECT_CONTEXT_SCAN should be allowed
        ResponseEntity<String> runRes = post("/api/tasks/" + taskId + "/multi-agent-runs",
                Map.of("strategy", "STANDARD_DELIVERY", "instruction", "test re-enable policy"));
        assertOk(runRes);
        String newRunId = TestJsonHelper.getString(
                TestJsonHelper.parse(runRes.getBody()).get("data"), "id");

        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + newRunId + "/tool-executions");
        assertOk(execRes);
        JsonNode execArray = TestJsonHelper.parse(execRes.getBody()).get("data");

        boolean hasScanCompleted = false;
        for (JsonNode te : execArray) {
            if ("PROJECT_CONTEXT_SCAN".equals(TestJsonHelper.getString(te, "toolName"))
                    && "COMPLETED".equals(TestJsonHelper.getString(te, "status"))) {
                hasScanCompleted = true;
            }
        }
        assertThat(hasScanCompleted).isTrue();
    }

    @Test
    @Order(15)
    void shouldVerifyBlockedSummary() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        for (JsonNode te : dataArray) {
            if ("BLOCKED".equals(TestJsonHelper.getString(te, "status"))) {
                String summary = TestJsonHelper.getString(te, "summary");
                assertThat(summary).contains("被策略阻止");
                String errMsg = TestJsonHelper.getString(te, "errorMessage");
                assertThat(errMsg).isNotNull().isNotEmpty();
            }
        }
    }

    @Test
    @Order(16)
    void shouldContainCompletedExecutionsForAllowedTools() {
        // After re-enabling in test 14, the new run should have completed executions
        // Check the first run for any COMPLETED executions too
        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        boolean hasCompleted = false;
        for (JsonNode te : dataArray) {
            if ("COMPLETED".equals(TestJsonHelper.getString(te, "status"))) {
                hasCompleted = true;
            }
        }
        // Even with some tools blocked, other LOW tools should still complete
        assertThat(hasCompleted).isTrue();
    }

    @Test
    @Order(17)
    void shouldFilterToolsByType() {
        ResponseEntity<String> res = get("/api/tool-catalog?toolType=READ_ONLY");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode t : dataArray) {
            assertThat(TestJsonHelper.getString(t, "toolType")).isEqualTo("READ_ONLY");
        }
    }

    @Test
    @Order(18)
    void shouldFilterToolsByRiskLevel() {
        ResponseEntity<String> res = get("/api/tool-catalog?riskLevel=LOW");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(dataArray.size()).isGreaterThan(0);
        for (JsonNode t : dataArray) {
            assertThat(TestJsonHelper.getString(t, "riskLevel")).isEqualTo("LOW");
        }
    }

    // ========================
    // Helper
    // ========================

    private void enableAgent(String projectId, String agentId) {
        post("/api/projects/" + projectId + "/agents/" + agentId + "/enable", Map.of());
    }
}
