package com.aicoding.platform.observability.controller;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.observability.application.ToolExecutionMetricsApplicationService;
import com.aicoding.platform.observability.dto.ToolExecutionMetricsResponse;
import com.aicoding.platform.orchestration.dto.ToolExecutionJobResponse;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolExecutionMetricsController {

    private final ToolExecutionMetricsApplicationService toolExecutionMetricsApplicationService;

    public ToolExecutionMetricsController(ToolExecutionMetricsApplicationService toolExecutionMetricsApplicationService) {
        this.toolExecutionMetricsApplicationService = toolExecutionMetricsApplicationService;
    }

    @GetMapping("/api/observability/tool-executions/metrics")
    public ApiResponse<ToolExecutionMetricsResponse> getGlobalMetrics() {
        requireAdmin();
        return ApiResponse.ok(toolExecutionMetricsApplicationService.getGlobalMetrics());
    }

    @GetMapping("/api/projects/{projectId}/observability/tool-executions/metrics")
    public ApiResponse<ToolExecutionMetricsResponse> getProjectMetrics(@PathVariable Long projectId) {
        return ApiResponse.ok(toolExecutionMetricsApplicationService.getProjectMetrics(projectId));
    }

    @GetMapping("/api/projects/{projectId}/observability/tool-executions/problem-jobs")
    public ApiResponse<List<ToolExecutionJobResponse>> listProblemJobs(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(toolExecutionMetricsApplicationService.listProblemJobs(projectId, status, limit));
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }
}
