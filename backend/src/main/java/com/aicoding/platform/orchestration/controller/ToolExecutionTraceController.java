package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolExecutionTraceService;
import com.aicoding.platform.orchestration.dto.ToolExecutionTraceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolExecutionTraceController {

    private final ToolExecutionTraceService traceService;

    public ToolExecutionTraceController(ToolExecutionTraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/api/tool-sandbox-executions/{executionId}/trace")
    public ApiResponse<ToolExecutionTraceResponse> getTrace(@PathVariable Long executionId) {
        return ApiResponse.ok(traceService.getTrace(executionId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}/tool-execution-traces")
    public ApiResponse<List<ToolExecutionTraceResponse>> getRunTraces(@PathVariable Long runId) {
        return ApiResponse.ok(traceService.listRunTraces(runId));
    }

    @GetMapping("/api/tasks/{taskId}/tool-execution-traces")
    public ApiResponse<List<ToolExecutionTraceResponse>> getTaskTraces(@PathVariable Long taskId) {
        return ApiResponse.ok(traceService.listTaskTraces(taskId));
    }
}
