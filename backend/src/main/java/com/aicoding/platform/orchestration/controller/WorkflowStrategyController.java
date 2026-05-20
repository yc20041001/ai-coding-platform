package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.WorkflowStrategyCatalogService;
import com.aicoding.platform.orchestration.dto.WorkflowStrategyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WorkflowStrategyController {

    private final WorkflowStrategyCatalogService workflowStrategyCatalogService;

    public WorkflowStrategyController(WorkflowStrategyCatalogService workflowStrategyCatalogService) {
        this.workflowStrategyCatalogService = workflowStrategyCatalogService;
    }

    @GetMapping("/api/multi-agent-strategies")
    public ApiResponse<List<WorkflowStrategyResponse>> listStrategies() {
        return ApiResponse.ok(workflowStrategyCatalogService.listStrategies());
    }
}
