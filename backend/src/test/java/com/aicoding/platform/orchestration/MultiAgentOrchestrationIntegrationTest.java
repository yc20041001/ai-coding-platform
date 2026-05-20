package com.aicoding.platform.orchestration;

import com.aicoding.platform.support.IntegrationTestBase;
import com.aicoding.platform.support.TestJsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MultiAgentOrchestrationIntegrationTest extends IntegrationTestBase {

    private String projectIdValue;
    private String taskIdValue;

    private void ensureTestData() {
        if (projectIdValue != null) return;

        String suffix = String.valueOf(System.currentTimeMillis());

        // Create project
        ResponseEntity<String> res = post("/api/projects", Map.of(
                "name", "IT-MultiAgent-" + suffix,
                "description", "Multi-agent integration test",
                "techStack", List.of("Java")
        ));
        assertOk(res);
        projectIdValue = TestJsonHelper.getString(TestJsonHelper.parse(res.getBody()), "data.id");

        // Create task
        ResponseEntity<String> taskRes = post("/api/projects/" + projectIdValue + "/tasks", Map.of(
                "title", "IT-MultiAgent-Task-" + suffix,
                "description", "Multi-agent test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        taskIdValue = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");
    }

    private @NonNull String taskId() {
        ensureTestData();
        return Objects.requireNonNull(taskIdValue);
    }

    private ResponseEntity<String> postNoAuth(@NonNull String path, Object body) {
        RequestEntity<Object> entity = RequestEntity.post(uri(path))
                .contentType(jsonMediaType())
                .body(Objects.requireNonNull(body));
        return restTemplate.exchange(entity, String.class);
    }

    // ========================
    // 1. Happy path
    // ========================

    @Test
    void shouldStartMultiAgentRunAndPauseAtApprovalGate() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs",
                Map.of("strategy", "DEFAULT_MOCK", "instruction", "测试多智能体协作"));

        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("WAITING_APPROVAL");
        assertThat(TestJsonHelper.getString(data, "strategy")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");

        // Pending approval gate should be present
        JsonNode pendingGate = data.get("pendingApprovalGate");
        assertThat(pendingGate).isNotNull();
        assertThat(TestJsonHelper.getString(pendingGate, "status")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(pendingGate, "gateKey")).isEqualTo("IMPLEMENTATION_PLAN_APPROVAL");

        JsonNode steps = data.get("steps");
        assertThat(steps.isArray()).isTrue();
        assertThat(steps.size()).isEqualTo(4); // Phase 1 (1 step) + Phase 2 (3 steps), paused before Phase 3
        assertThat(TestJsonHelper.getInt(steps.get(0), "stepOrder")).isEqualTo(1);
    }

    @Test
    void shouldListMultiAgentRuns() {
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());

        ResponseEntity<String> res = get("/api/tasks/" + taskId() + "/multi-agent-runs");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        assertThat(TestJsonHelper.getString(data.get(0), "status")).isEqualTo("WAITING_APPROVAL");
    }

    @Test
    void shouldGetMultiAgentRunDetail() {
        ResponseEntity<String> startRes = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());
        String runId = TestJsonHelper.getString(
                TestJsonHelper.parse(startRes.getBody()), "data.id");

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "id")).isEqualTo(runId);
        assertThat(data.get("steps").size()).isEqualTo(4); // paused at gate after Phase 2
    }

    @Test
    void shouldReturnStepsInOrder() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());

        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");
        for (int i = 0; i < steps.size(); i++) {
            assertThat(TestJsonHelper.getInt(steps.get(i), "stepOrder")).isEqualTo(i + 1);
        }
    }

    @Test
    void shouldTaskStayRunningWhenPausedAtApprovalGate() {
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());

        ResponseEntity<String> res = get("/api/tasks/" + taskId());
        assertOk(res);
        String status = TestJsonHelper.getString(
                TestJsonHelper.parse(res.getBody()), "data.status");
        assertThat(status).isEqualTo("RUNNING");
    }

    @Test
    void shouldWriteTaskLogsWithApprovalRequested() {
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());

        ResponseEntity<String> res = get("/api/tasks/" + taskId() + "/logs");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        boolean hasStartLog = false;
        boolean hasApprovalLog = false;
        for (JsonNode log : data) {
            String stage = TestJsonHelper.getString(log, "stage");
            if ("MULTI_AGENT_PHASED_START".equals(stage)) hasStartLog = true;
            if ("MULTI_AGENT_APPROVAL_REQUESTED".equals(stage)) hasApprovalLog = true;
        }
        assertThat(hasStartLog).isTrue();
        assertThat(hasApprovalLog).isTrue();
    }

    @Test
    void shouldNotWriteArtifactWhenPausedAtGate() {
        post("/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());

        ResponseEntity<String> res = get("/api/tasks/" + taskId() + "/artifacts");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        boolean hasSummary = false;
        for (JsonNode artifact : data) {
            String name = TestJsonHelper.getString(artifact, "name");
            if ("Multi-Agent Mock Orchestration Summary".equals(name)) hasSummary = true;
        }
        assertThat(hasSummary).isFalse(); // artifact not created until run completes after approval
    }

    // ========================
    // 2. Permission tests
    // ========================

    @Test
    void shouldRejectUnauthenticated() {
        try {
            ResponseEntity<String> res = postNoAuth(
                    "/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401 with JDK HTTP client
        }
    }

    @Test
    void shouldReturnNotFoundForInvalidTask() {
        ResponseEntity<String> res = post(
                "/api/tasks/99999999/multi-agent-runs", Map.of());
        assertCode(res, "NOT_FOUND");
    }

    @Test
    void shouldReturnNotFoundForInvalidRunId() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/99999999");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 3. Status validation
    // ========================

    @Test
    void shouldRejectCompletedTask() {
        // First, complete the task using REVIEW_ONLY strategy (no approval gate)
        String suffix = String.valueOf(System.currentTimeMillis()) + "-CT";
        ResponseEntity<String> prjRes = post("/api/projects", Map.of(
                "name", "IT-CT-" + suffix,
                "description", "Completed task test",
                "techStack", List.of("Java")
        ));
        assertOk(prjRes);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prjRes.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-CT-Task-" + suffix,
                "description", "Completed task test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        // Complete the task with REVIEW_ONLY strategy (no gate)
        ResponseEntity<String> runRes = post("/api/tasks/" + tid + "/multi-agent-runs",
                Map.of("strategy", "REVIEW_ONLY"));
        assertOk(runRes);
        assertThat(TestJsonHelper.getString(
                TestJsonHelper.parse(runRes.getBody()), "data.status")).isEqualTo("COMPLETED");

        // Second run on the completed task should fail with CONFLICT
        ResponseEntity<String> res = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of());
        assertCode(res, "CONFLICT");
    }

    // ========================
    // 4. Message passing tests
    // ========================

    private String freshRunId() {
        // Create a dedicated task for message tests so we don't clash with
        // shouldRejectCompletedTask which completes the shared cached task.
        String suffix = String.valueOf(System.currentTimeMillis()) + "-MSG";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-Msg-" + suffix,
                "description", "Message passing test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        // Enable the multi-agent agents for this project
        for (long agentId : new long[]{300001L, 300002L, 300004L, 300005L}) {
            ResponseEntity<String> enableRes = post(
                    "/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
            assertOk(enableRes);
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-Msg-Task-" + suffix,
                "description", "Message passing test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of());
        assertOk(startRes);
        return TestJsonHelper.getString(TestJsonHelper.parse(startRes.getBody()), "data.id");
    }

    @Test
    void shouldGenerateTaskContextMessage() {
        String runId = freshRunId();

        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        boolean hasTaskContext = false;
        for (JsonNode m : messages) {
            if ("TASK_CONTEXT".equals(TestJsonHelper.getString(m, "messageType"))) {
                hasTaskContext = true;
                assertThat(TestJsonHelper.getString(m, "content")).contains("任务上下文");
            }
        }
        assertThat(hasTaskContext).isTrue();
    }

    @Test
    void shouldGenerateStepOutputMessages() {
        String runId = freshRunId();

        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        int stepOutputCount = 0;
        for (JsonNode m : messages) {
            if ("STEP_OUTPUT".equals(TestJsonHelper.getString(m, "messageType"))) {
                stepOutputCount++;
            }
        }
        assertThat(stepOutputCount).isEqualTo(3); // only Phase 1 + Phase 2 agents that are enabled
    }

    @Test
    void shouldGenerateHandoffMessages() {
        String runId = freshRunId();

        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        int handoffCount = 0;
        for (JsonNode m : messages) {
            if ("HANDOFF".equals(TestJsonHelper.getString(m, "messageType"))) {
                handoffCount++;
                assertThat(TestJsonHelper.getString(m, "content")).contains("Phase 交接");
            }
        }
        assertThat(handoffCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldGenerateApprovalRequestMessage() {
        String runId = freshRunId();

        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        boolean hasApprovalRequest = false;
        for (JsonNode m : messages) {
            if ("APPROVAL_REQUEST".equals(TestJsonHelper.getString(m, "messageType"))) {
                hasApprovalRequest = true;
                assertThat(TestJsonHelper.getString(m, "summary")).contains("审批闸门已创建");
            }
        }
        assertThat(hasApprovalRequest).isTrue();
    }

    @Test
    void shouldReturnMessagesInRunDetail() {
        String runId = freshRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode messages = data.get("messages");
        assertThat(messages.isArray()).isTrue();
        assertThat(messages.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldGetMessagesByRunId() {
        String runId = freshRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(res);
        JsonNode messages = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(messages.isArray()).isTrue();
        assertThat(messages.size()).isGreaterThanOrEqualTo(1);

        JsonNode firstMsg = messages.get(0);
        assertThat(TestJsonHelper.getString(firstMsg, "id")).isNotEmpty();
        assertThat(TestJsonHelper.getString(firstMsg, "runId")).isEqualTo(runId);
        assertThat(TestJsonHelper.getString(firstMsg, "messageType")).isNotEmpty();
    }

    @Test
    void shouldReturnMessagesSortedByCreateTime() {
        String runId = freshRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(res);
        JsonNode messages = TestJsonHelper.parse(res.getBody()).get("data");
        String prevTime = "";
        for (JsonNode m : messages) {
            String currTime = TestJsonHelper.getString(m, "createTime");
            if (!prevTime.isEmpty()) {
                assertThat(currTime.compareTo(prevTime)).isGreaterThanOrEqualTo(0);
            }
            prevTime = currTime;
        }
    }

    @Test
    void shouldIncludePriorStepSummaryInInputContext() {
        String runId = freshRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");

        for (int i = 1; i < steps.size(); i++) {
            JsonNode step = steps.get(i);
            String inputContext = TestJsonHelper.getString(step, "inputContext");
            String status = TestJsonHelper.getString(step, "status");
            if ("COMPLETED".equals(status) && inputContext != null) {
                assertThat(inputContext).contains("前序 Phase 输出摘要");
            }
        }
    }

    @Test
    void shouldFinalSummaryBeNullWhenPausedAtGate() {
        String runId = freshRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String finalSummary = TestJsonHelper.getString(data, "finalSummary");
        assertThat(finalSummary).isNullOrEmpty(); // final summary not generated until after approval
    }

    @Test
    void shouldRejectUnauthenticatedForMessages() {
        String runId = freshRunId();

        try {
            ResponseEntity<String> res = getNoAuth("/api/multi-agent-runs/" + runId + "/messages");
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401 with JDK HTTP client
        }
    }

    @Test
    void shouldReturnNotFoundForInvalidRunMessages() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/99999999/messages");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 5. Phase tests (35C)
    // ========================

    private String freshPhaseRunId() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-PHASE";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-Phase-" + suffix,
                "description", "Phase test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        // Enable all 5 multi-agent agents including frontend-agent
        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            ResponseEntity<String> enableRes = post(
                    "/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
            assertOk(enableRes);
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-Phase-Task-" + suffix,
                "description", "Phase test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "PHASED_PARALLEL_MOCK"));
        assertOk(startRes);
        return TestJsonHelper.getString(TestJsonHelper.parse(startRes.getBody()), "data.id");
    }

    @Test
    void shouldCreateFourPhasesOnStartRun() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode phases = data.get("phases");
        assertThat(phases.isArray()).isTrue();
        assertThat(phases.size()).isEqualTo(4);
    }

    @Test
    void shouldReturnPhasesOrderedByPhaseOrderAsc() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data").get("phases");
        for (int i = 0; i < phases.size(); i++) {
            assertThat(TestJsonHelper.getInt(phases.get(i), "phaseOrder")).isEqualTo(i + 1);
        }
    }

    @Test
    void shouldPhase2ContainThreeSteps() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data").get("phases");
        JsonNode phase2 = phases.get(1);
        assertThat(TestJsonHelper.getString(phase2, "phaseKey")).isEqualTo("IMPLEMENTATION");
        JsonNode steps = phase2.get("steps");
        assertThat(steps.size()).isEqualTo(3);

        boolean hasBackend = false, hasFrontend = false, hasTest = false;
        for (JsonNode step : steps) {
            String stepType = TestJsonHelper.getString(step, "stepType");
            if ("BACKEND_IMPLEMENTATION_PLAN".equals(stepType)) hasBackend = true;
            if ("FRONTEND_IMPLEMENTATION_PLAN".equals(stepType)) hasFrontend = true;
            if ("TEST_PLAN".equals(stepType)) hasTest = true;
        }
        assertThat(hasBackend).isTrue();
        assertThat(hasFrontend).isTrue();
        assertThat(hasTest).isTrue();
    }

    @Test
    void shouldPhase2StepsHaveSamePhaseIdAndPhaseOrder() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data").get("phases");
        JsonNode phase2 = phases.get(1);
        String phaseId = TestJsonHelper.getString(phase2, "id");
        int phaseOrder = TestJsonHelper.getInt(phase2, "phaseOrder");

        JsonNode steps = phase2.get("steps");
        for (JsonNode step : steps) {
            assertThat(TestJsonHelper.getString(step, "phaseId")).isEqualTo(phaseId);
            assertThat(TestJsonHelper.getInt(step, "phaseOrder")).isEqualTo(phaseOrder);
        }
    }

    @Test
    void shouldEachStepHaveLaneKey() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");
        for (JsonNode step : steps) {
            String status = TestJsonHelper.getString(step, "status");
            if ("COMPLETED".equals(status)) {
                assertThat(TestJsonHelper.getString(step, "laneKey")).isNotEmpty();
            }
        }
    }

    @Test
    void shouldRunDetailReturnPhases() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode phases = data.get("phases");
        assertThat(phases.isArray()).isTrue();
        assertThat(phases.size()).isGreaterThanOrEqualTo(1);

        JsonNode firstPhase = phases.get(0);
        assertThat(TestJsonHelper.getString(firstPhase, "phaseKey")).isNotEmpty();
        assertThat(TestJsonHelper.getString(firstPhase, "title")).isNotEmpty();
        assertThat(TestJsonHelper.getString(firstPhase, "status")).isNotEmpty();
        assertThat(firstPhase.get("steps").isArray()).isTrue();
    }

    @Test
    void shouldGetPhasesByRunId() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/phases");
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(phases.isArray()).isTrue();
        assertThat(phases.size()).isEqualTo(4);

        for (JsonNode phase : phases) {
            assertThat(TestJsonHelper.getString(phase, "id")).isNotEmpty();
            assertThat(TestJsonHelper.getString(phase, "runId")).isEqualTo(runId);
            assertThat(TestJsonHelper.getString(phase, "phaseKey")).isNotEmpty();
            assertThat(phase.get("steps").isArray()).isTrue();
        }
    }

    @Test
    void shouldPhase1OutputSummaryBeReferencedInPhase2InputSummary() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/phases");
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data");

        JsonNode phase1 = phases.get(0);
        JsonNode phase2 = phases.get(1);

        String phase1Output = TestJsonHelper.getString(phase1, "outputSummary");
        assertThat(phase1Output).isNotEmpty();

        String phase2Input = TestJsonHelper.getString(phase2, "inputSummary");
        assertThat(phase2Input).isNotEmpty();
        assertThat(phase2Input).contains("Phase 1");
    }

    @Test
    void shouldPhase2OutputSummaryAggregateBackendFrontendTest() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/phases");
        assertOk(res);
        JsonNode phases = TestJsonHelper.parse(res.getBody()).get("data");

        JsonNode phase2 = phases.get(1);
        String outputSummary = TestJsonHelper.getString(phase2, "outputSummary");
        assertThat(outputSummary).isNotEmpty();
        assertThat(outputSummary).contains("IMPLEMENTATION");
    }

    @Test
    void shouldCodeReviewStepNotExistWhenPausedAtGate() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");
        for (JsonNode step : steps) {
            String stepType = TestJsonHelper.getString(step, "stepType");
            assertThat(stepType).isNotEqualTo("CODE_REVIEW"); // Phase 3 not executed before gate
        }
    }

    @Test
    void shouldFinalSummaryBeNullOrEmptyWhenPausedAtGateForPhases() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        String finalSummary = TestJsonHelper.getString(data, "finalSummary");
        assertThat(finalSummary).isNullOrEmpty(); // not generated until after approval
    }

    @Test
    void shouldRejectUnauthenticatedForPhases() {
        String runId = freshPhaseRunId();

        try {
            ResponseEntity<String> res = getNoAuth("/api/multi-agent-runs/" + runId + "/phases");
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401 with JDK HTTP client
        }
    }

    @Test
    void shouldReturnNotFoundForInvalidRunPhases() {
        ResponseEntity<String> res = get("/api/multi-agent-runs/99999999/phases");
        assertCode(res, "NOT_FOUND");
    }

    // ========================
    // 6. Strategy tests (35D)
    // ========================

    @Test
    void shouldListStrategiesReturnFourStrategies() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(4);
    }

    @Test
    void shouldStrategiesHaveRequiredFields() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : data) {
            assertThat(TestJsonHelper.getString(s, "strategyKey")).isNotEmpty();
            assertThat(TestJsonHelper.getString(s, "name")).isNotEmpty();
            assertThat(TestJsonHelper.getString(s, "description")).isNotEmpty();
            assertThat(TestJsonHelper.getInt(s, "phaseCount")).isGreaterThanOrEqualTo(1);
            assertThat(TestJsonHelper.getInt(s, "stepCount")).isGreaterThanOrEqualTo(1);
            assertThat(s.get("phases").isArray()).isTrue();
        }
    }

    @Test
    void shouldStandardDeliveryHaveFourPhases() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode strategies = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : strategies) {
            if ("STANDARD_DELIVERY".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                assertThat(TestJsonHelper.getInt(s, "phaseCount")).isEqualTo(4);
                assertThat(s.get("phases").size()).isEqualTo(4);
                return;
            }
        }
        throw new AssertionError("STANDARD_DELIVERY strategy not found");
    }

    @Test
    void shouldStandardDeliveryHaveSixSteps() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode strategies = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : strategies) {
            if ("STANDARD_DELIVERY".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                assertThat(TestJsonHelper.getInt(s, "stepCount")).isEqualTo(6);
                return;
            }
        }
        throw new AssertionError("STANDARD_DELIVERY strategy not found");
    }

    @Test
    void shouldBackendFocusedHaveFiveSteps() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode strategies = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : strategies) {
            if ("BACKEND_FOCUSED".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                assertThat(TestJsonHelper.getInt(s, "stepCount")).isEqualTo(5);
                // Should NOT have frontend lane
                boolean hasFrontend = false;
                for (JsonNode phase : s.get("phases")) {
                    for (JsonNode step : phase.get("steps")) {
                        if ("frontend".equals(TestJsonHelper.getString(step, "laneKey"))) {
                            hasFrontend = true;
                        }
                    }
                }
                assertThat(hasFrontend).isFalse();
                return;
            }
        }
        throw new AssertionError("BACKEND_FOCUSED strategy not found");
    }

    @Test
    void shouldFrontendFocusedHaveFiveSteps() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode strategies = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : strategies) {
            if ("FRONTEND_FOCUSED".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                assertThat(TestJsonHelper.getInt(s, "stepCount")).isEqualTo(5);
                // Should NOT have backend lane
                boolean hasBackend = false;
                for (JsonNode phase : s.get("phases")) {
                    for (JsonNode step : phase.get("steps")) {
                        if ("backend".equals(TestJsonHelper.getString(step, "laneKey"))) {
                            hasBackend = true;
                        }
                    }
                }
                assertThat(hasBackend).isFalse();
                return;
            }
        }
        throw new AssertionError("FRONTEND_FOCUSED strategy not found");
    }

    @Test
    void shouldReviewOnlyHaveTwoPhases() {
        ResponseEntity<String> res = get("/api/multi-agent-strategies");
        assertOk(res);
        JsonNode strategies = TestJsonHelper.parse(res.getBody()).get("data");
        for (JsonNode s : strategies) {
            if ("REVIEW_ONLY".equals(TestJsonHelper.getString(s, "strategyKey"))) {
                assertThat(TestJsonHelper.getInt(s, "phaseCount")).isEqualTo(2);
                assertThat(TestJsonHelper.getInt(s, "stepCount")).isEqualTo(2);
                return;
            }
        }
        throw new AssertionError("REVIEW_ONLY strategy not found");
    }

    @Test
    void shouldLegacyDefaultMockMapToStandardDelivery() {
        // Create a new run with DEFAULT_MOCK
        String suffix = String.valueOf(System.currentTimeMillis()) + "-DM";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-DM-" + suffix,
                "description", "Default mock legacy test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-DM-Task-" + suffix,
                "description", "Default mock test",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "DEFAULT_MOCK"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");
        // strategy field stores the normalized key
        assertThat(TestJsonHelper.getString(data, "strategy")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyName")).isNotEmpty();
        assertThat(data.get("steps").size()).isEqualTo(4); // paused at gate after Phase 2
    }

    @Test
    void shouldLegacyPhasedParallelMockMapToStandardDelivery() {
        String runId = freshPhaseRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        // strategy field stores the normalized key
        assertThat(TestJsonHelper.getString(data, "strategy")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyName")).isEqualTo("标准交付流程");
    }

    @Test
    void shouldRejectInvalidStrategy() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs",
                Map.of("strategy", "INVALID_STRATEGY_XYZ"));
        assertCode(res, "BAD_REQUEST");
    }

    @Test
    void shouldReturnStrategyMetadataInRunResponse() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs",
                Map.of("strategy", "STANDARD_DELIVERY"));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
        assertThat(TestJsonHelper.getString(data, "strategyName")).isEqualTo("标准交付流程");
        assertThat(TestJsonHelper.getString(data, "strategyDescription")).isNotEmpty();
    }

    private String freshBackendFocusedRunId() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-BF";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-BF-" + suffix,
                "description", "Backend focused test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-BF-Task-" + suffix,
                "description", "Backend focused test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "BACKEND_FOCUSED"));
        assertOk(startRes);
        return TestJsonHelper.getString(TestJsonHelper.parse(startRes.getBody()), "data.id");
    }

    @Test
    void shouldBackendFocusedStrategySkipFrontendLane() {
        String runId = freshBackendFocusedRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");

        boolean hasFrontend = false;
        boolean hasBackend = false;
        for (JsonNode step : steps) {
            String stepType = TestJsonHelper.getString(step, "stepType");
            if ("FRONTEND_IMPLEMENTATION_PLAN".equals(stepType)) hasFrontend = true;
            if ("BACKEND_IMPLEMENTATION_PLAN".equals(stepType)) hasBackend = true;
        }
        assertThat(hasBackend).isTrue();
        assertThat(hasFrontend).isFalse();
    }

    private String freshFrontendFocusedRunId() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-FF";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-FF-" + suffix,
                "description", "Frontend focused test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-FF-Task-" + suffix,
                "description", "Frontend focused test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "FRONTEND_FOCUSED"));
        assertOk(startRes);
        return TestJsonHelper.getString(TestJsonHelper.parse(startRes.getBody()), "data.id");
    }

    @Test
    void shouldFrontendFocusedStrategySkipBackendLane() {
        String runId = freshFrontendFocusedRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode steps = TestJsonHelper.parse(res.getBody()).get("data").get("steps");

        boolean hasFrontend = false;
        boolean hasBackend = false;
        for (JsonNode step : steps) {
            String stepType = TestJsonHelper.getString(step, "stepType");
            if ("FRONTEND_IMPLEMENTATION_PLAN".equals(stepType)) hasFrontend = true;
            if ("BACKEND_IMPLEMENTATION_PLAN".equals(stepType)) hasBackend = true;
        }
        assertThat(hasFrontend).isTrue();
        assertThat(hasBackend).isFalse();
    }

    private String freshReviewOnlyRunId() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-RO";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-RO-" + suffix,
                "description", "Review only test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-RO-Task-" + suffix,
                "description", "Review only test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"));
        assertOk(startRes);
        return TestJsonHelper.getString(TestJsonHelper.parse(startRes.getBody()), "data.id");
    }

    @Test
    void shouldReviewOnlyStrategyHaveOnlyTwoPhases() {
        String runId = freshReviewOnlyRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        JsonNode phases = data.get("phases");
        assertThat(phases.size()).isEqualTo(2);

        boolean hasReview = false, hasSummary = false;
        for (JsonNode phase : phases) {
            String key = TestJsonHelper.getString(phase, "phaseKey");
            if ("REVIEW".equals(key)) hasReview = true;
            if ("SUMMARY".equals(key)) hasSummary = true;
        }
        assertThat(hasReview).isTrue();
        assertThat(hasSummary).isTrue();
    }

    @Test
    void shouldNullStrategyDefaultToStandardDelivery() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs", Map.of());
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
    }

    @Test
    void shouldBlankStrategyDefaultToStandardDelivery() {
        ResponseEntity<String> res = post(
                "/api/tasks/" + taskId() + "/multi-agent-runs",
                Map.of("strategy", ""));
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "strategyKey")).isEqualTo("STANDARD_DELIVERY");
    }

    // ========================
    // 7. Approval gate tests (35E)
    // ========================

    private String freshApprovalRunId() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-AG";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-AG-" + suffix,
                "description", "Approval gate test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300002L, 300003L, 300004L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-AG-Task-" + suffix,
                "description", "Approval gate test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "STANDARD_DELIVERY"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("WAITING_APPROVAL");
        return TestJsonHelper.getString(data, "id");
    }

    @Test
    void shouldRunContainPendingApprovalGate() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId);
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");

        JsonNode pendingGate = data.get("pendingApprovalGate");
        assertThat(pendingGate).isNotNull();
        assertThat(TestJsonHelper.getString(pendingGate, "status")).isEqualTo("PENDING");
        assertThat(TestJsonHelper.getString(pendingGate, "gateKey")).isEqualTo("IMPLEMENTATION_PLAN_APPROVAL");

        JsonNode gates = data.get("approvalGates");
        assertThat(gates.isArray()).isTrue();
        assertThat(gates.size()).isEqualTo(1);
    }

    @Test
    void shouldGetApprovalGatesEndpoint() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> res = get("/api/multi-agent-runs/" + runId + "/approval-gates");
        assertOk(res);
        JsonNode data = TestJsonHelper.parse(res.getBody()).get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(1);
        assertThat(TestJsonHelper.getString(data.get(0), "status")).isEqualTo("PENDING");
    }

    @Test
    void shouldApproveGateAndContinueToCompleted() {
        String runId = freshApprovalRunId();

        // Get run to find gate ID
        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");
        String taskId = TestJsonHelper.getString(detailData, "taskId");

        // Approve
        ResponseEntity<String> approveRes = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                Map.of("comment", "方案可进入下一阶段。"));
        assertOk(approveRes);
        JsonNode runData = TestJsonHelper.parse(approveRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(runData, "status")).isEqualTo("COMPLETED");
        assertThat(TestJsonHelper.getString(runData, "finalSummary")).isNotEmpty();
        assertThat(runData.get("steps").size()).isEqualTo(6);

        // Gate should be APPROVED
        JsonNode gates = runData.get("approvalGates");
        assertThat(gates.size()).isEqualTo(1);
        assertThat(TestJsonHelper.getString(gates.get(0), "status")).isEqualTo("APPROVED");

        // Task should be COMPLETED
        ResponseEntity<String> taskRes = get("/api/tasks/" + taskId);
        assertOk(taskRes);
        assertThat(TestJsonHelper.getString(
                TestJsonHelper.parse(taskRes.getBody()), "data.status")).isEqualTo("COMPLETED");
    }

    @Test
    void shouldRejectGateAndCancelRun() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");
        String taskId = TestJsonHelper.getString(detailData, "taskId");

        // Reject
        ResponseEntity<String> rejectRes = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/reject",
                Map.of("comment", "方案需要重新调整。"));
        assertOk(rejectRes);
        JsonNode runData = TestJsonHelper.parse(rejectRes.getBody()).get("data");
        assertThat(TestJsonHelper.getString(runData, "status")).isEqualTo("CANCELED");

        // Gate should be REJECTED
        JsonNode gates = runData.get("approvalGates");
        assertThat(gates.size()).isEqualTo(1);
        assertThat(TestJsonHelper.getString(gates.get(0), "status")).isEqualTo("REJECTED");

        // Remaining phases should be SKIPPED
        JsonNode phases = runData.get("phases");
        for (int i = 2; i < phases.size(); i++) {
            assertThat(TestJsonHelper.getString(phases.get(i), "status")).isEqualTo("SKIPPED");
        }

        // Task should be CANCELED
        ResponseEntity<String> taskRes = get("/api/tasks/" + taskId);
        assertOk(taskRes);
        assertThat(TestJsonHelper.getString(
                TestJsonHelper.parse(taskRes.getBody()), "data.status")).isEqualTo("CANCELED");
    }

    @Test
    void shouldRejectDuplicateApprove() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");

        // First approve
        ResponseEntity<String> firstRes = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                Map.of("comment", "批准"));
        assertOk(firstRes);

        // Second approve should fail with CONFLICT
        ResponseEntity<String> secondRes = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                Map.of("comment", "再次批准"));
        assertCode(secondRes, "CONFLICT");
    }

    @Test
    void shouldRejectApproveOnNonWaitingRun() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");

        // Approve first
        post("/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve", Map.of());

        // Try to approve again — should fail
        ResponseEntity<String> res = post(
                "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve", Map.of());
        assertCode(res, "CONFLICT");
    }

    @Test
    void shouldReviewOnlyStrategyCompleteWithoutGate() {
        String suffix = String.valueOf(System.currentTimeMillis()) + "-ROG";
        ResponseEntity<String> prj = post("/api/projects", Map.of(
                "name", "IT-ROG-" + suffix,
                "description", "Review only gate test",
                "techStack", List.of("Java")
        ));
        assertOk(prj);
        String pid = TestJsonHelper.getString(TestJsonHelper.parse(prj.getBody()), "data.id");

        for (long agentId : new long[]{300001L, 300005L}) {
            post("/api/projects/" + pid + "/agents/" + agentId + "/enable", Map.of());
        }

        ResponseEntity<String> taskRes = post("/api/projects/" + pid + "/tasks", Map.of(
                "title", "IT-ROG-Task-" + suffix,
                "description", "Review only gate test task",
                "taskType", "FEATURE",
                "priority", "MEDIUM",
                "agentId", AGENT_ID
        ));
        assertOk(taskRes);
        String tid = TestJsonHelper.getString(TestJsonHelper.parse(taskRes.getBody()), "data.id");

        ResponseEntity<String> startRes = post("/api/tasks/" + tid + "/multi-agent-runs", Map.of(
                "strategy", "REVIEW_ONLY"));
        assertOk(startRes);
        JsonNode data = TestJsonHelper.parse(startRes.getBody()).get("data");

        // REVIEW_ONLY has no gate, should complete directly
        assertThat(TestJsonHelper.getString(data, "status")).isEqualTo("COMPLETED");
        JsonNode pendingGate = data.get("pendingApprovalGate");
        JsonNode gates = data.get("approvalGates");
        assertThat(gates.isArray()).isTrue();
        assertThat(gates.size()).isEqualTo(0);
        assertThat(pendingGate == null || pendingGate.isNull()).isTrue();
    }

    @Test
    void shouldApprovalDecisionWriteMessagesAndLogs() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");
        String taskId = TestJsonHelper.getString(detailData, "taskId");

        // Approve
        post("/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                Map.of("comment", "同意"));

        // Check messages for APPROVAL_DECISION
        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        boolean hasDecision = false;
        for (JsonNode m : messages) {
            if ("APPROVAL_DECISION".equals(TestJsonHelper.getString(m, "messageType"))) {
                hasDecision = true;
                assertThat(TestJsonHelper.getString(m, "content")).contains("批准");
            }
        }
        assertThat(hasDecision).isTrue();

        // Check task logs for approval events
        ResponseEntity<String> logRes = get("/api/tasks/" + taskId + "/logs");
        assertOk(logRes);
        JsonNode logs = TestJsonHelper.parse(logRes.getBody()).get("data");
        boolean hasApproved = false;
        boolean hasResumed = false;
        for (JsonNode log : logs) {
            String stage = TestJsonHelper.getString(log, "stage");
            if ("MULTI_AGENT_APPROVAL_APPROVED".equals(stage)) hasApproved = true;
            if ("MULTI_AGENT_RESUMED_AFTER_APPROVAL".equals(stage)) hasResumed = true;
        }
        assertThat(hasApproved).isTrue();
        assertThat(hasResumed).isTrue();
    }

    @Test
    void shouldRejectGateWriteMessagesAndLogs() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");
        String taskId = TestJsonHelper.getString(detailData, "taskId");

        // Reject
        post("/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/reject",
                Map.of("comment", "驳回"));

        // Check messages for APPROVAL_DECISION
        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        boolean hasDecision = false;
        for (JsonNode m : messages) {
            if ("APPROVAL_DECISION".equals(TestJsonHelper.getString(m, "messageType"))) {
                hasDecision = true;
                assertThat(TestJsonHelper.getString(m, "content")).contains("驳回");
            }
        }
        assertThat(hasDecision).isTrue();

        // Check task logs
        ResponseEntity<String> logRes = get("/api/tasks/" + taskId + "/logs");
        assertOk(logRes);
        JsonNode logs = TestJsonHelper.parse(logRes.getBody()).get("data");
        boolean hasRejected = false;
        for (JsonNode log : logs) {
            if ("MULTI_AGENT_APPROVAL_REJECTED".equals(TestJsonHelper.getString(log, "stage"))) {
                hasRejected = true;
            }
        }
        assertThat(hasRejected).isTrue();
    }

    @Test
    void shouldUnauthenticatedRejectApprove() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> detailRes = get("/api/multi-agent-runs/" + runId);
        JsonNode detailData = TestJsonHelper.parse(detailRes.getBody()).get("data");
        String gateId = TestJsonHelper.getString(detailData.get("pendingApprovalGate"), "id");

        try {
            ResponseEntity<String> res = postNoAuth(
                    "/api/multi-agent-runs/" + runId + "/approval-gates/" + gateId + "/approve",
                    Map.of());
            assertCode(res, "UNAUTHORIZED");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Expected on 401
        }
    }

    @Test
    void shouldApprovalGateContainApprovalRequestMessage() {
        String runId = freshApprovalRunId();

        ResponseEntity<String> msgRes = get("/api/multi-agent-runs/" + runId + "/messages");
        assertOk(msgRes);
        JsonNode messages = TestJsonHelper.parse(msgRes.getBody()).get("data");
        boolean hasRequest = false;
        for (JsonNode m : messages) {
            if ("APPROVAL_REQUEST".equals(TestJsonHelper.getString(m, "messageType"))) {
                hasRequest = true;
                assertThat(TestJsonHelper.getString(m, "summary")).contains("审批闸门已创建");
            }
        }
        assertThat(hasRequest).isTrue();
    }
}
