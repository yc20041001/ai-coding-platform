package com.aicoding.platform.observability.controller;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.observability.application.SystemOverviewApplicationService;
import com.aicoding.platform.observability.dto.SystemOverviewResponse;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemOverviewController {

    private final SystemOverviewApplicationService systemOverviewApplicationService;

    public SystemOverviewController(SystemOverviewApplicationService systemOverviewApplicationService) {
        this.systemOverviewApplicationService = systemOverviewApplicationService;
    }

    @GetMapping("/api/observability/overview")
    public ApiResponse<SystemOverviewResponse> getGlobalOverview() {
        requireAdmin();
        return ApiResponse.ok(systemOverviewApplicationService.getGlobalOverview());
    }

    @GetMapping("/api/projects/{projectId}/observability/overview")
    public ApiResponse<SystemOverviewResponse> getProjectOverview(@PathVariable Long projectId) {
        requireAdmin();
        return ApiResponse.ok(systemOverviewApplicationService.getProjectOverview(projectId));
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }
}
