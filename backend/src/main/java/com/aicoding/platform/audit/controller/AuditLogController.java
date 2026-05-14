package com.aicoding.platform.audit.controller;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.dto.AuditLogQueryRequest;
import com.aicoding.platform.audit.dto.AuditLogResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.security.context.LoginUser;
import com.aicoding.platform.security.context.LoginUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

    private final AuditLogApplicationService auditLogApplicationService;

    public AuditLogController(AuditLogApplicationService auditLogApplicationService) {
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping("/api/audit/logs")
    public ApiResponse<PageResult<AuditLogResponse>> listAuditLogs(AuditLogQueryRequest request) {
        requireAdmin();
        return ApiResponse.ok(auditLogApplicationService.list(request));
    }

    @GetMapping("/api/projects/{projectId}/audit/logs")
    public ApiResponse<PageResult<AuditLogResponse>> listProjectAuditLogs(@PathVariable Long projectId,
                                                                           AuditLogQueryRequest request) {
        requireAdmin();
        return ApiResponse.ok(auditLogApplicationService.listByProject(projectId, request));
    }

    private void requireAdmin() {
        LoginUser currentUser = LoginUserContext.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要平台管理员权限");
        }
    }
}
