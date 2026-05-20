package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.WorkflowTemplateApplicationService;
import com.aicoding.platform.orchestration.dto.UpdateWorkflowTemplateStatusRequest;
import com.aicoding.platform.orchestration.dto.WorkflowTemplateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WorkflowTemplateController {

    private final WorkflowTemplateApplicationService workflowTemplateApplicationService;

    public WorkflowTemplateController(WorkflowTemplateApplicationService workflowTemplateApplicationService) {
        this.workflowTemplateApplicationService = workflowTemplateApplicationService;
    }

    @GetMapping("/api/workflow-templates")
    public ApiResponse<List<WorkflowTemplateResponse>> listTemplates(@RequestParam(required = false) String status) {
        return ApiResponse.ok(workflowTemplateApplicationService.listTemplates(status));
    }

    @GetMapping("/api/workflow-templates/{templateId}")
    public ApiResponse<WorkflowTemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.ok(workflowTemplateApplicationService.getTemplate(templateId));
    }

    @PutMapping("/api/workflow-templates/{templateId}/status")
    public ApiResponse<WorkflowTemplateResponse> updateStatus(@PathVariable Long templateId,
                                                               @RequestBody UpdateWorkflowTemplateStatusRequest request) {
        return ApiResponse.ok(workflowTemplateApplicationService.updateStatus(templateId,
                request != null ? request.getStatus() : null));
    }
}
