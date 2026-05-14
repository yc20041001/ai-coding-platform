package com.aicoding.platform.orchestrator.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestrator.application.AgentOrchestratorService;
import com.aicoding.platform.orchestrator.dto.AgentExecutionResponse;
import com.aicoding.platform.orchestrator.dto.ExecuteTaskRequest;
import com.aicoding.platform.orchestrator.dto.ModelRequestLogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AgentOrchestratorController {

    private final AgentOrchestratorService agentOrchestratorService;

    public AgentOrchestratorController(AgentOrchestratorService agentOrchestratorService) {
        this.agentOrchestratorService = agentOrchestratorService;
    }

    @PostMapping("/api/tasks/{taskId}/execute")
    public ApiResponse<AgentExecutionResponse> executeTask(@PathVariable Long taskId,
                                                            @RequestBody ExecuteTaskRequest request) {
        return ApiResponse.ok(agentOrchestratorService.executeTask(taskId, request));
    }

    @GetMapping("/api/tasks/{taskId}/executions")
    public ApiResponse<PageResult<AgentExecutionResponse>> listExecutions(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(agentOrchestratorService.listExecutions(taskId, pageQuery));
    }

    @GetMapping("/api/agent-executions/{executionId}")
    public ApiResponse<AgentExecutionResponse> getExecutionDetail(@PathVariable Long executionId) {
        return ApiResponse.ok(agentOrchestratorService.getExecutionDetail(executionId));
    }

    @GetMapping("/api/agent-executions/{executionId}/model-logs")
    public ApiResponse<List<ModelRequestLogResponse>> getModelLogs(@PathVariable Long executionId) {
        return ApiResponse.ok(agentOrchestratorService.getModelLogs(executionId));
    }
}
