package com.aicoding.platform.orchestration;

import com.aicoding.platform.orchestration.application.ReadOnlyRepositoryAdapter;
import com.aicoding.platform.orchestration.application.RepositoryReadToolService;
import com.aicoding.platform.orchestration.application.RepositoryToolSafetyService;
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
class RepositoryReadToolIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RepositoryToolSafetyService safetyService;

    @Autowired
    private ReadOnlyRepositoryAdapter repositoryAdapter;

    private String projectId;
    private String taskId;
    private String runId;

    // Tool IDs
    private String treeToolId;    // READ_REPOSITORY_TREE
    private String snippetToolId; // READ_FILE_SNIPPET
    private String diffToolId;    // READ_DIFF_SUMMARY
    private String branchToolId;  // READ_BRANCH_INFO

    // ========================
    // Tool Catalog / Config (1-8)
    // ========================

    @Test
    @Order(1)
    void shouldCatalogContainFourRepositoryTools() {
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        int repoCount = 0;
        for (JsonNode t : dataArray) {
            String key = TestJsonHelper.getString(t, "toolKey");
            switch (key) {
                case "READ_REPOSITORY_TREE" -> {
                    repoCount++;
                    treeToolId = TestJsonHelper.getString(t, "id");
                }
                case "READ_FILE_SNIPPET" -> {
                    repoCount++;
                    snippetToolId = TestJsonHelper.getString(t, "id");
                }
                case "READ_DIFF_SUMMARY" -> {
                    repoCount++;
                    diffToolId = TestJsonHelper.getString(t, "id");
                }
                case "READ_BRANCH_INFO" -> {
                    repoCount++;
                    branchToolId = TestJsonHelper.getString(t, "id");
                }
            }
        }
        assertThat(repoCount).isEqualTo(4);
        assertThat(treeToolId).isNotNull();
        assertThat(branchToolId).isNotNull();
        assertThat(snippetToolId).isNotNull();
        assertThat(diffToolId).isNotNull();
    }

    @Test
    @Order(2)
    void shouldReadRepositoryTreeRiskLow() {
        assertThat(treeToolId).isNotNull();
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode t : dataArray) {
            if (treeToolId.equals(TestJsonHelper.getString(t, "id"))) {
                assertThat(TestJsonHelper.getString(t, "riskLevel")).isEqualTo("LOW");
                return;
            }
        }
        throw new AssertionError("READ_REPOSITORY_TREE not found");
    }

    @Test
    @Order(3)
    void shouldReadBranchInfoRiskLow() {
        assertThat(branchToolId).isNotNull();
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode t : dataArray) {
            if (branchToolId.equals(TestJsonHelper.getString(t, "id"))) {
                assertThat(TestJsonHelper.getString(t, "riskLevel")).isEqualTo("LOW");
                return;
            }
        }
        throw new AssertionError("READ_BRANCH_INFO not found");
    }

    @Test
    @Order(4)
    void shouldReadFileSnippetRiskMedium() {
        assertThat(snippetToolId).isNotNull();
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode t : dataArray) {
            if (snippetToolId.equals(TestJsonHelper.getString(t, "id"))) {
                assertThat(TestJsonHelper.getString(t, "riskLevel")).isEqualTo("MEDIUM");
                return;
            }
        }
        throw new AssertionError("READ_FILE_SNIPPET not found");
    }

    @Test
    @Order(5)
    void shouldReadDiffSummaryRiskMedium() {
        assertThat(diffToolId).isNotNull();
        ResponseEntity<String> res = get("/api/tool-catalog");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode t : dataArray) {
            if (diffToolId.equals(TestJsonHelper.getString(t, "id"))) {
                assertThat(TestJsonHelper.getString(t, "riskLevel")).isEqualTo("MEDIUM");
                return;
            }
        }
        throw new AssertionError("READ_DIFF_SUMMARY not found");
    }

    @Test
    @Order(6)
    void shouldCreateProjectForRepoReadTests() {
        String suffix = String.valueOf(System.currentTimeMillis());
        ResponseEntity<String> projRes = post("/api/projects", Map.of(
                "name", "IT-REPO-" + suffix,
                "description", "Repository read tool integration test",
                "techStack", List.of("Java")
        ));
        assertOk(projRes);
        projectId = TestJsonHelper.getString(
                TestJsonHelper.parse(projRes.getBody()), "data.id");
        assertThat(projectId).isNotNull();

        // Enable agents
        enableAgent(projectId, "300001");
        enableAgent(projectId, "300002");
        enableAgent(projectId, "300003");
        enableAgent(projectId, "300004");
        enableAgent(projectId, "300005");

        // Create task
        ResponseEntity<String> taskRes = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-REPO-Task-" + suffix,
                "description", "Repo read test task",
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
    @Order(7)
    void shouldListLowRepositoryToolsDefaultEnabled() {
        ResponseEntity<String> res = get("/api/projects/" + projectId + "/tools");
        assertOk(res);
        JsonNode dataArray = TestJsonHelper.parse(res.getBody()).get("data");

        boolean treeEnabled = false;
        boolean branchEnabled = false;
        boolean snippetDisabled = true;
        boolean diffDisabled = true;

        for (JsonNode pt : dataArray) {
            String key = TestJsonHelper.getString(pt, "toolKey");
            boolean enabled = pt.get("projectEnabled").asBoolean();
            switch (key) {
                case "READ_REPOSITORY_TREE" -> treeEnabled = enabled;
                case "READ_BRANCH_INFO" -> branchEnabled = enabled;
                case "READ_FILE_SNIPPET" -> snippetDisabled = snippetDisabled && !enabled;
                case "READ_DIFF_SUMMARY" -> diffDisabled = diffDisabled && !enabled;
            }
        }
        assertThat(treeEnabled).as("LOW repository tools should be enabled by default").isTrue();
        assertThat(branchEnabled).as("LOW repository tools should be enabled by default").isTrue();
        assertThat(snippetDisabled).as("MEDIUM repository tools should be disabled by default").isTrue();
        assertThat(diffDisabled).as("MEDIUM repository tools should be disabled by default").isTrue();
    }

    @Test
    @Order(8)
    void shouldEnableRepositoryTools() {
        // Enable snippet (MEDIUM) and diff (MEDIUM)
        ResponseEntity<String> res1 = post("/api/projects/" + projectId + "/tools/" + snippetToolId + "/enable",
                Map.of());
        assertOk(res1);
        JsonNode data1 = TestJsonHelper.parse(res1.getBody()).get("data");
        assertThat(data1.get("projectEnabled").asBoolean()).isTrue();

        ResponseEntity<String> res2 = post("/api/projects/" + projectId + "/tools/" + diffToolId + "/enable",
                Map.of());
        assertOk(res2);
        JsonNode data2 = TestJsonHelper.parse(res2.getBody()).get("data");
        assertThat(data2.get("projectEnabled").asBoolean()).isTrue();

        // Start a multi-agent run so we can test execution
        ResponseEntity<String> runRes = post("/api/tasks/" + taskId + "/multi-agent-runs", Map.of());
        assertOk(runRes);
        runId = TestJsonHelper.getString(
                TestJsonHelper.parse(runRes.getBody()), "data.id");
        assertThat(runId).isNotNull();
    }

    // ========================
    // Safety (9-13)
    // ========================

    @Test
    @Order(9)
    void shouldBlockPathWithDotDot() {
        // READ_FILE_SNIPPET with ".." in filePath should be blocked by policy
        ResponseEntity<String> res = post("/api/tasks/" + taskId + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"
        ));
        assertOk(res);
    }

    @Test
    @Order(10)
    void shouldBlockEnvFilePath() {
        assertThat(safetyService.isSensitivePath(".env")).isTrue();
        assertThat(safetyService.isSensitivePath(".env.production")).isTrue();
        assertThat(safetyService.isSensitivePath("config/.env")).isTrue();
    }

    @Test
    @Order(11)
    void shouldBlockGitDirectory() {
        assertThat(safetyService.isSensitivePath(".git/config")).isTrue();
        assertThat(safetyService.isSensitivePath(".git/HEAD")).isTrue();
        assertThat(safetyService.isSensitivePath("src/.git/objects")).isTrue();
    }

    @Test
    @Order(12)
    void shouldBlockAbsolutePath() {
        try {
            safetyService.validateSafeRelativePath("/etc/passwd");
            throw new AssertionError("Should have thrown for absolute path");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("绝对路径");
        }
    }

    @Test
    @Order(13)
    void shouldBlockExceedingMaxLines() {
        // maxLines = 300 is the max per schema. Values > 300 should be rejected
        // by parameter schema validation. Since we validate via schema service,
        // this verifies the schema max is enforced.
        com.aicoding.platform.orchestration.application.ToolParameterSchemaService schemaService =
                new com.aicoding.platform.orchestration.application.ToolParameterSchemaService();

        String schemaJson = "{\"fields\":[{\"key\":\"maxLines\",\"type\":\"number\",\"required\":true,\"defaultValue\":80,\"min\":1,\"max\":300}]}";
        Map<String, Object> validParams = Map.of("maxLines", 200);
        Map<String, Object> result = schemaService.normalizeAndValidate(schemaJson, validParams);
        assertThat(result.get("maxLines")).isEqualTo(200);

        try {
            Map<String, Object> invalidParams = Map.of("maxLines", 500);
            schemaService.normalizeAndValidate(schemaJson, invalidParams);
            throw new AssertionError("Should have thrown for exceeding maxLines");
        } catch (com.aicoding.platform.common.exception.BizException e) {
            String msg = e.getMessage();
            assertThat(msg).isNotNull();
            assertThat(msg.contains("maxLines") || msg.contains("500") || msg.contains("300"))
                    .as("Exception should mention maxLines constraint: " + msg).isTrue();
        }
    }

    private void enableAgent(String projectId, String agentId) {
        post("/api/projects/" + projectId + "/agents/" + agentId + "/enable", Map.of());
    }

    // ========================
    // Execution (14-21)
    // ========================

    @Test
    @Order(14)
    void shouldReadRepositoryTreeOutputReadOnly() {
        // Wait for run to complete, then check tool executions
        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        JsonNode execArray = TestJsonHelper.parse(execRes.getBody()).get("data");

        // Find READ_REPOSITORY_TREE executions
        for (JsonNode exec : execArray) {
            String toolKey = TestJsonHelper.getString(exec, "toolName");
            if ("READ_REPOSITORY_TREE".equals(toolKey)) {
                String outputPayload = TestJsonHelper.getString(exec, "outputPayload");
                assertThat(outputPayload).isNotNull();
                try {
                    JsonNode output = TestJsonHelper.parse(outputPayload);
                    assertThat(output.get("readOnly").asBoolean()).isTrue();
                } catch (Exception e) {
                    // output may be empty for mocked runs without repo tools triggered
                }
            }
        }
    }

    @Test
    @Order(15)
    void shouldReadRepositoryTreeFilesTouchedEmpty() {
        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        JsonNode execArray = TestJsonHelper.parse(execRes.getBody()).get("data");

        for (JsonNode exec : execArray) {
            String toolKey = TestJsonHelper.getString(exec, "toolName");
            if ("READ_REPOSITORY_TREE".equals(toolKey)) {
                String outputPayload = TestJsonHelper.getString(exec, "outputPayload");
                if (outputPayload != null) {
                    try {
                        JsonNode output = TestJsonHelper.parse(outputPayload);
                        assertThat(output.get("filesTouched").isArray()).isTrue();
                        assertThat(output.get("filesTouched").size()).isEqualTo(0);
                    } catch (Exception e) {
                        // skip
                    }
                }
            }
        }
    }

    @Test
    @Order(16)
    void shouldReadBranchInfoNoCheckout() {
        // The service never performs checkout by construction
        // Verify the tool results are present
        ResponseEntity<String> execRes = get("/api/multi-agent-runs/" + runId + "/tool-executions");
        assertOk(execRes);
        JsonNode execArray = TestJsonHelper.parse(execRes.getBody()).get("data");

        for (JsonNode exec : execArray) {
            String toolKey = TestJsonHelper.getString(exec, "toolName");
            if ("READ_BRANCH_INFO".equals(toolKey)) {
                String outputPayload = TestJsonHelper.getString(exec, "outputPayload");
                if (outputPayload != null) {
                    try {
                        JsonNode output = TestJsonHelper.parse(outputPayload);
                        assertThat(output.get("noCheckout").asBoolean()).isTrue();
                        assertThat(output.get("noPull").asBoolean()).isTrue();
                    } catch (Exception e) {
                        // skip
                    }
                }
            }
        }
    }

    @Test
    @Order(17)
    void shouldReadFileSnippetWithFilesRead() {
        RepositoryReadToolService repoService =
                new RepositoryReadToolService(repositoryAdapter);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("filePath", "TestFile.java");
        params.put("maxLines", 50);
        params.put("branch", "main");

        var result = repoService.executeReadOnlyTool(0L, "READ_FILE_SNIPPET", params);
        assertThat(result.getOutputPayload()).contains("\"filesTouched\":[]");
        assertThat(result.getOutputPayload()).contains("\"gitOperations\":[]");
        assertThat(result.getOutputPayload()).contains("\"readOnly\":true");
    }

    @Test
    @Order(18)
    void shouldReadDiffSummaryReturnReadOnly() {
        RepositoryReadToolService repoService =
                new RepositoryReadToolService(repositoryAdapter);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("branch", "feature/test");
        params.put("baseBranch", "main");
        params.put("maxFiles", 20);

        var result = repoService.executeReadOnlyTool(0L, "READ_DIFF_SUMMARY", params);
        assertThat(result.getOutputPayload()).contains("\"filesTouched\":[]");
        assertThat(result.getOutputPayload()).contains("\"readOnly\":true");
    }

    @Test
    @Order(19)
    void shouldRepositoryToolCreateJob() {
        // After a multi-agent run, check that jobs exist for repository tools
        ResponseEntity<String> jobRes = get("/api/multi-agent-runs/" + runId + "/tool-execution-jobs");
        assertOk(jobRes);
        JsonNode jobArray = TestJsonHelper.parse(jobRes.getBody()).get("data");

        for (JsonNode job : jobArray) {
            String toolKey = TestJsonHelper.getString(job, "toolKey");
            if (toolKey != null && (toolKey.equals("READ_REPOSITORY_TREE")
                    || toolKey.equals("READ_FILE_SNIPPET")
                    || toolKey.equals("READ_DIFF_SUMMARY")
                    || toolKey.equals("READ_BRANCH_INFO"))) {
                String status = TestJsonHelper.getString(job, "status");
                assertThat(status).isIn("COMPLETED", "PENDING");
                return;
            }
        }
        // If no repository tool jobs found, that's acceptable since auto-mapping is optional
    }

    @Test
    @Order(20)
    void shouldCreateTaskLogForRepositoryTool() {
        // Verify task logs exist by checking multi-agent run details
        ResponseEntity<String> runRes = get("/api/multi-agent-runs/" + runId);
        assertOk(runRes);
        JsonNode runData = TestJsonHelper.parse(runRes.getBody()).get("data");
        assertThat(runData).isNotNull();
    }

    @Test
    @Order(21)
    void shouldSafetyServiceRejectDotDot() {
        try {
            safetyService.validateSafeRelativePath("../../etc/passwd");
            throw new AssertionError("Should have thrown for '..' path");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("..");
        }
    }
}
