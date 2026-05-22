package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolExecutionAuditExportService;
import com.aicoding.platform.orchestration.dto.ToolAuditExportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ToolExecutionAuditController {

    private final ToolExecutionAuditExportService auditExportService;

    public ToolExecutionAuditController(ToolExecutionAuditExportService auditExportService) {
        this.auditExportService = auditExportService;
    }

    @GetMapping("/api/orchestration/executions/{executionId}/audit-export")
    public ApiResponse<ToolAuditExportResponse> exportExecutionAudit(@PathVariable Long executionId) {
        return ApiResponse.ok(auditExportService.exportExecutionTrace(executionId));
    }

    @GetMapping("/api/orchestration/runs/{runId}/evidence-export")
    public ApiResponse<ToolAuditExportResponse> exportRunEvidence(@PathVariable Long runId) {
        return ApiResponse.ok(auditExportService.exportRunEvidence(runId));
    }

    @GetMapping("/api/orchestration/tasks/{taskId}/tool-audit-export")
    public ApiResponse<ToolAuditExportResponse> exportTaskToolAudit(@PathVariable Long taskId) {
        return ApiResponse.ok(auditExportService.exportTaskToolAudit(taskId));
    }
}
