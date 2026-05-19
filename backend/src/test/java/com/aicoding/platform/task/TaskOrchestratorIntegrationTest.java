package com.aicoding.platform.task;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskOrchestratorIntegrationTest extends IntegrationTestBase {

    private String createProject() {
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-Task-Project-" + System.currentTimeMillis(),
                "description", "Task integration test",
                "techStack", List.of("Java")
        ));
        JsonNode root = TestJsonHelper.parse(res.getBody());
        return TestJsonHelper.getString(root, "data.id");
    }

    @Test
    void shouldCreateTaskWithPendingStatus() {
        String projectId = createProject();
        ResponseEntity<String> res = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-Task-" + System.currentTimeMillis(),
                "description", "Test task description",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));

        assertOk(res);
        JsonNode root = TestJsonHelper.parse(res.getBody());
        String taskId = TestJsonHelper.getString(root, "data.id");
        assertThat(taskId).isNotEmpty();
        assertThat(TestJsonHelper.getString(root, "data.status")).isEqualTo("PENDING");
    }

    @Test
    void shouldExecuteTaskAndTransitionToCompleted() {
        String projectId = createProject();

        // Create task
        ResponseEntity<String> createRes = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-Exec-Task-" + System.currentTimeMillis(),
                "description", "Execute test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        String taskId = TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id");

        // Execute task
        ResponseEntity<String> execRes = post("/api/tasks/" + taskId + "/execute", Map.of(
                "instruction", "请完成这个集成测试任务",
                "agentId", AGENT_ID,
                "useRag", false,
                "ragLimit", 5
        ));
        assertOk(execRes);
        JsonNode execRoot = TestJsonHelper.parse(execRes.getBody());
        assertThat(TestJsonHelper.getString(execRoot, "data.status")).isEqualTo("COMPLETED");
        String executionId = TestJsonHelper.getString(execRoot, "data.id");
        assertThat(executionId).isNotEmpty();
        assertThat(TestJsonHelper.getString(execRoot, "data.agentVersionId")).isNotEmpty();

        // Verify the orchestrator used the Agent version runtime config in the model prompt.
        ResponseEntity<String> executionDetailRes = get("/api/agent-executions/" + executionId);
        assertOk(executionDetailRes);
        JsonNode executionDetailRoot = TestJsonHelper.parse(executionDetailRes.getBody());
        String inputPrompt = TestJsonHelper.getString(executionDetailRoot, "data.inputPrompt");
        assertThat(inputPrompt).contains("You are a Backend Agent");
        assertThat(inputPrompt).contains("Tool Policy:");
        assertThat(inputPrompt).contains("Execution Policy:");

        // Verify task detail shows completed
        ResponseEntity<String> detailRes = get("/api/tasks/" + taskId);
        assertOk(detailRes);
        JsonNode detailRoot = TestJsonHelper.parse(detailRes.getBody());
        assertThat(TestJsonHelper.getString(detailRoot, "data.status")).isEqualTo("COMPLETED");

        // Verify task logs exist
        ResponseEntity<String> logsRes = get("/api/tasks/" + taskId + "/logs");
        assertOk(logsRes);
        String logsJson = TestJsonHelper.parse(logsRes.getBody()).get("data").toString();
        assertThat(logsJson).contains("ORCHESTRATOR");

        // Verify task artifacts exist
        ResponseEntity<String> artifactsRes = get("/api/tasks/" + taskId + "/artifacts");
        assertOk(artifactsRes);

        // Verify executions exist
        ResponseEntity<String> execsRes = get("/api/tasks/" + taskId + "/executions?page=1&pageSize=10");
        assertOk(execsRes);

        // Verify model logs exist
        ResponseEntity<String> modelLogsRes = get("/api/agent-executions/" + executionId + "/model-logs");
        assertOk(modelLogsRes);
    }

    @Test
    void shouldRejectRepeatExecutionOfCompletedTask() {
        String projectId = createProject();

        ResponseEntity<String> createRes = post("/api/projects/" + projectId + "/tasks", Map.of(
                "title", "IT-Repeat-" + System.currentTimeMillis(),
                "description", "Repeat execution test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        String taskId = TestJsonHelper.getString(
                TestJsonHelper.parse(createRes.getBody()), "data.id");

        // First execution
        post("/api/tasks/" + taskId + "/execute", Map.of(
                "instruction", "First execution",
                "agentId", AGENT_ID,
                "useRag", false,
                "ragLimit", 5
        ));

        // Second execution should fail
        ResponseEntity<String> repeatRes = post("/api/tasks/" + taskId + "/execute", Map.of(
                "instruction", "Repeat execution",
                "agentId", AGENT_ID,
                "useRag", false,
                "ragLimit", 5
        ));
        assertCode(repeatRes, "CONFLICT");
    }

    @Test
    void shouldRejectWithoutToken() {
        ResponseEntity<String> res = getNoAuth("/api/tasks/99999");
        assertCode(res, "UNAUTHORIZED");
    }
}
