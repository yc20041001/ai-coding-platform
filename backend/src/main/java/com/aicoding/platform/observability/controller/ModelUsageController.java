package com.aicoding.platform.observability.controller;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.observability.application.ModelUsageApplicationService;
import com.aicoding.platform.observability.dto.ModelUsageDailyResponse;
import com.aicoding.platform.observability.dto.ModelUsageSummaryResponse;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ModelUsageController {

    private final ModelUsageApplicationService modelUsageApplicationService;

    public ModelUsageController(ModelUsageApplicationService modelUsageApplicationService) {
        this.modelUsageApplicationService = modelUsageApplicationService;
    }

    @GetMapping("/api/observability/model-usage/summary")
    public ApiResponse<ModelUsageSummaryResponse> getGlobalSummary() {
        requireAdmin();
        return ApiResponse.ok(modelUsageApplicationService.getGlobalSummary());
    }

    @GetMapping("/api/projects/{projectId}/observability/model-usage/summary")
    public ApiResponse<ModelUsageSummaryResponse> getProjectSummary(@PathVariable Long projectId) {
        requireAdmin();
        return ApiResponse.ok(modelUsageApplicationService.getProjectSummary(projectId));
    }

    @GetMapping("/api/projects/{projectId}/observability/model-usage/daily")
    public ApiResponse<List<ModelUsageDailyResponse>> getProjectDaily(@PathVariable Long projectId) {
        requireAdmin();
        return ApiResponse.ok(modelUsageApplicationService.getProjectDaily(projectId));
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }
}
