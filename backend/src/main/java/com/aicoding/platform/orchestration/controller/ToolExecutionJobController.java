package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolExecutionJobService;
import com.aicoding.platform.orchestration.dto.RetryToolExecutionJobRequest;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolExecutionJobController {

    private final ToolExecutionJobService toolExecutionJobService;

    public ToolExecutionJobController(ToolExecutionJobService toolExecutionJobService) {
        this.toolExecutionJobService = toolExecutionJobService;
    }

    @GetMapping("/api/tool-execution-jobs/{jobId}")
    public ApiResponse<ToolExecutionJobResponse> getJob(@PathVariable Long jobId) {
        return ApiResponse.ok(toolExecutionJobService.getJob(jobId));
    }

    @GetMapping("/api/tool-sandbox-executions/{executionId}/jobs")
    public ApiResponse<List<ToolExecutionJobResponse>> getExecutionJobs(@PathVariable Long executionId) {
        return ApiResponse.ok(toolExecutionJobService.listByExecution(executionId));
    }

    @GetMapping("/api/multi-agent-runs/{runId}/tool-execution-jobs")
    public ApiResponse<List<ToolExecutionJobResponse>> getRunJobs(@PathVariable Long runId) {
        return ApiResponse.ok(toolExecutionJobService.listByRun(runId));
    }

    @PostMapping("/api/tool-execution-jobs/{jobId}/retry")
    public ApiResponse<ToolExecutionJobResponse> retryJob(
            @PathVariable Long jobId,
            @RequestBody(required = false) RetryToolExecutionJobRequest request) {
        RetryToolExecutionJobRequest req = request != null ? request : new RetryToolExecutionJobRequest();
        return ApiResponse.ok(toolExecutionJobService.retryJob(jobId, req));
    }

    @PostMapping("/api/tool-execution-jobs/{jobId}/cancel")
    public ApiResponse<ToolExecutionJobResponse> cancelJob(@PathVariable Long jobId) {
        return ApiResponse.ok(toolExecutionJobService.cancelJob(jobId));
    }

    // ========================
    // 37C New Endpoints
    // ========================

    @GetMapping("/api/projects/{projectId}/tool-execution-jobs/failed")
    public ApiResponse<List<ToolExecutionJobResponse>> listFailedJobs(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(toolExecutionJobService.listFailedJobs(projectId, status));
    }

    @PostMapping("/api/tool-execution-jobs/{jobId}/manual-retry")
    public ApiResponse<ToolExecutionJobResponse> manualRetry(
            @PathVariable Long jobId,
            @RequestBody(required = false) RetryToolExecutionJobRequest request) {
        RetryToolExecutionJobRequest req = request != null ? request : new RetryToolExecutionJobRequest();
        return ApiResponse.ok(toolExecutionJobService.manualRetry(jobId, req));
    }

    @PostMapping("/api/tool-execution-jobs/recover-timeouts")
    public ApiResponse<Integer> recoverTimeouts() {
        int count = toolExecutionJobService.recoverTimedOutRunningJobs(null);
        return ApiResponse.ok(count);
    }

    @PostMapping("/api/tool-execution-jobs/dispatch-retries")
    public ApiResponse<Integer> dispatchRetries() {
        int count = toolExecutionJobService.dispatchRetries();
        return ApiResponse.ok(count);
    }
}
