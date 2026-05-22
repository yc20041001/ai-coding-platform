package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.MultiAgentOrchestrationService;
import com.aicoding.platform.orchestration.application.ToolSandboxExecutionService;
import com.aicoding.platform.orchestration.dto.MultiAgentApprovalDecisionRequest;
import com.aicoding.platform.orchestration.dto.MultiAgentApprovalGateResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentMessageResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentPhaseResponse;
import com.aicoding.platform.orchestration.dto.MultiAgentRunResponse;
import com.aicoding.platform.orchestration.dto.StartMultiAgentRunRequest;
import com.aicoding.platform.orchestration.dto.ToolApprovalDecisionRequest;
import com.aicoding.platform.orchestration.dto.ToolExecutionApprovalResponse;
import com.aicoding.platform.orchestration.dto.ToolSandboxExecutionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MultiAgentOrchestrationController {

    private final MultiAgentOrchestrationService multiAgentOrchestrationService;
    private final ToolSandboxExecutionService toolSandboxExecutionService;

    public MultiAgentOrchestrationController(MultiAgentOrchestrationService multiAgentOrchestrationService,
                                              ToolSandboxExecutionService toolSandboxExecutionService) {
        this.multiAgentOrchestrationService = multiAgentOrchestrationService;
        this.toolSandboxExecutionService = toolSandboxExecutionService;
    }

    @PostMapping("/api/tasks/{taskId}/multi-agent-runs")
    public ApiResponse<MultiAgentRunResponse> startMultiAgentRun(@PathVariable Long taskId,
                                                                  @RequestBody(required = false) StartMultiAgentRunRequest request) {
        StartMultiAgentRunRequest req = request != null ? request : new StartMultiAgentRunRequest();
        return ApiResponse.ok(multiAgentOrchestrationService.startRun(taskId, req));
    }

    @GetMapping("/api/tasks/{taskId}/multi-agent-runs")
    public ApiResponse<List<MultiAgentRunResponse>> listMultiAgentRuns(@PathVariable Long taskId,
                                                                         @RequestParam(defaultValue = "1") Integer page,
                                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(multiAgentOrchestrationService.listRuns(taskId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}")
    public ApiResponse<MultiAgentRunResponse> getMultiAgentRun(@PathVariable Long runId) {
        return ApiResponse.ok(multiAgentOrchestrationService.getRun(runId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}/phases")
    public ApiResponse<List<MultiAgentPhaseResponse>> getMultiAgentRunPhases(@PathVariable Long runId) {
        return ApiResponse.ok(multiAgentOrchestrationService.getPhases(runId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}/messages")
    public ApiResponse<List<MultiAgentMessageResponse>> getMultiAgentRunMessages(@PathVariable Long runId) {
        return ApiResponse.ok(multiAgentOrchestrationService.getMessages(runId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}/approval-gates")
    public ApiResponse<List<MultiAgentApprovalGateResponse>> getApprovalGates(@PathVariable Long runId) {
        return ApiResponse.ok(multiAgentOrchestrationService.getApprovalGates(runId));
    }

    @PostMapping("/api/multi-agent-runs/{runId}/approval-gates/{gateId}/approve")
    public ApiResponse<MultiAgentRunResponse> approveGate(@PathVariable Long runId,
                                                           @PathVariable Long gateId,
                                                           @RequestBody(required = false) MultiAgentApprovalDecisionRequest request) {
        MultiAgentApprovalDecisionRequest req = request != null ? request : new MultiAgentApprovalDecisionRequest();
        return ApiResponse.ok(multiAgentOrchestrationService.approveGate(runId, gateId, req));
    }

    @PostMapping("/api/multi-agent-runs/{runId}/approval-gates/{gateId}/reject")
    public ApiResponse<MultiAgentRunResponse> rejectGate(@PathVariable Long runId,
                                                          @PathVariable Long gateId,
                                                          @RequestBody(required = false) MultiAgentApprovalDecisionRequest request) {
        MultiAgentApprovalDecisionRequest req = request != null ? request : new MultiAgentApprovalDecisionRequest();
        return ApiResponse.ok(multiAgentOrchestrationService.rejectGate(runId, gateId, req));
    }

    // ========================
    // Tool Sandbox Execution APIs
    // ========================

    @GetMapping("/api/multi-agent-runs/{runId}/tool-executions")
    public ApiResponse<List<ToolSandboxExecutionResponse>> getRunToolExecutions(@PathVariable Long runId) {
        return ApiResponse.ok(toolSandboxExecutionService.listByRun(runId));
    }

    @GetMapping("/api/multi-agent-steps/{stepId}/tool-executions")
    public ApiResponse<List<ToolSandboxExecutionResponse>> getStepToolExecutions(@PathVariable Long stepId) {
        return ApiResponse.ok(toolSandboxExecutionService.listByStep(stepId));
    }

    @GetMapping("/api/tool-sandbox-executions/{executionId}")
    public ApiResponse<ToolSandboxExecutionResponse> getToolSandboxExecution(@PathVariable Long executionId) {
        return ApiResponse.ok(toolSandboxExecutionService.getExecution(executionId));
    }

    // ========================
    // Tool Execution Approval APIs
    // ========================

    @GetMapping("/api/tool-sandbox-executions/{executionId}/approval")
    public ApiResponse<ToolExecutionApprovalResponse> getToolExecutionApproval(@PathVariable Long executionId) {
        return ApiResponse.ok(toolSandboxExecutionService.getApproval(executionId));
    }

    @PostMapping("/api/tool-sandbox-executions/{executionId}/approve")
    public ApiResponse<ToolSandboxExecutionResponse> approveToolExecution(
            @PathVariable Long executionId,
            @RequestBody(required = false) ToolApprovalDecisionRequest request) {
        ToolApprovalDecisionRequest req = request != null ? request : new ToolApprovalDecisionRequest();
        return ApiResponse.ok(toolSandboxExecutionService.approveAndExecute(executionId, req.getComment()));
    }

    @PostMapping("/api/tool-sandbox-executions/{executionId}/reject")
    public ApiResponse<ToolSandboxExecutionResponse> rejectToolExecution(
            @PathVariable Long executionId,
            @RequestBody(required = false) ToolApprovalDecisionRequest request) {
        ToolApprovalDecisionRequest req = request != null ? request : new ToolApprovalDecisionRequest();
        return ApiResponse.ok(toolSandboxExecutionService.rejectExecution(executionId, req.getComment()));
    }

    @GetMapping("/api/projects/{projectId}/tool-approvals")
    public ApiResponse<List<ToolExecutionApprovalResponse>> listProjectToolApprovals(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(toolSandboxExecutionService.listProjectApprovals(projectId, status));
    }
}
