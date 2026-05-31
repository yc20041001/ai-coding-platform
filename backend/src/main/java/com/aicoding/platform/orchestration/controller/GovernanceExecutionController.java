package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceExecutionController {

    private final GovernancePlaybookTemplateService playbookTemplateService;
    private final GovernanceExecutionPlanService executionPlanService;
    private final GovernanceHandoffAssistantService handoffAssistantService;

    public GovernanceExecutionController(GovernancePlaybookTemplateService playbookTemplateService,
                                          GovernanceExecutionPlanService executionPlanService,
                                          GovernanceHandoffAssistantService handoffAssistantService) {
        this.playbookTemplateService = playbookTemplateService;
        this.executionPlanService = executionPlanService;
        this.handoffAssistantService = handoffAssistantService;
    }

    // ========== Playbook Template ==========
    @PostMapping("/api/governance-execution/playbook-templates")
    public ApiResponse<GovernancePlaybookTemplateResponse> createTemplate(@RequestBody CreateGovernancePlaybookTemplateRequest req) {
        return ApiResponse.ok(playbookTemplateService.createTemplate(req));
    }
    @GetMapping("/api/governance-execution/playbook-templates")
    public ApiResponse<List<GovernancePlaybookTemplateResponse>> listTemplates() {
        return ApiResponse.ok(playbookTemplateService.listTemplates());
    }
    @GetMapping("/api/governance-execution/playbook-templates/{templateId}")
    public ApiResponse<GovernancePlaybookTemplateResponse> getTemplate(@PathVariable String templateId) {
        return ApiResponse.ok(playbookTemplateService.getTemplate(templateId));
    }
    @PutMapping("/api/governance-execution/playbook-templates/{templateId}")
    public ApiResponse<GovernancePlaybookTemplateResponse> updateTemplate(@PathVariable String templateId,
                                                                           @RequestBody UpdateGovernancePlaybookTemplateRequest req) {
        return ApiResponse.ok(playbookTemplateService.updateTemplate(templateId, req));
    }
    @PostMapping("/api/governance-execution/playbook-templates/{templateId}/status")
    public ApiResponse<GovernancePlaybookTemplateResponse> updateTemplateStatus(@PathVariable String templateId,
                                                                                 @RequestParam Boolean enabled) {
        return ApiResponse.ok(playbookTemplateService.updateTemplateStatus(templateId, enabled));
    }
    @GetMapping("/api/governance-execution/playbook-match-preview/{recommendationId}")
    public ApiResponse<GovernancePlaybookMatchPreviewResponse> matchPreview(@PathVariable String recommendationId) {
        return ApiResponse.ok(playbookTemplateService.matchPreview(recommendationId));
    }

    // ========== Execution Plan ==========
    @PostMapping("/api/governance-execution/plans")
    public ApiResponse<GovernanceExecutionPlanResponse> createPlan(@RequestParam String recommendationId,
                                                                    @RequestParam(required = false) String templateKey) {
        return ApiResponse.ok(executionPlanService.createPlan(recommendationId, templateKey));
    }
    @GetMapping("/api/governance-execution/plans")
    public ApiResponse<List<GovernanceExecutionPlanResponse>> listPlans() {
        return ApiResponse.ok(executionPlanService.listPlans());
    }
    @GetMapping("/api/governance-execution/plans/{planId}")
    public ApiResponse<GovernanceExecutionPlanResponse> getPlan(@PathVariable String planId) {
        return ApiResponse.ok(executionPlanService.getPlan(planId));
    }
    @PutMapping("/api/governance-execution/plans/{planId}")
    public ApiResponse<GovernanceExecutionPlanResponse> updatePlan(@PathVariable String planId,
                                                                    @RequestParam(required = false) String ownerName,
                                                                    @RequestParam(required = false) String dueAt,
                                                                    @RequestParam(required = false) String summaryText) {
        return ApiResponse.ok(executionPlanService.updatePlan(planId, ownerName, dueAt, summaryText));
    }
    @PostMapping("/api/governance-execution/plans/{planId}/status")
    public ApiResponse<GovernanceExecutionPlanResponse> updatePlanStatus(@PathVariable String planId,
                                                                          @RequestParam String status) {
        return ApiResponse.ok(executionPlanService.updatePlanStatus(planId, status));
    }
    @PostMapping("/api/governance-execution/plans/{planId}/steps/{stepKey}/status")
    public ApiResponse<GovernanceExecutionPlanResponse> updateStepStatus(@PathVariable String planId,
                                                                          @PathVariable String stepKey,
                                                                          @RequestParam String status) {
        return ApiResponse.ok(executionPlanService.updateStepStatus(planId, stepKey, status));
    }
    @GetMapping("/api/governance-execution/dashboard")
    public ApiResponse<GovernanceExecutionDashboardResponse> getDashboard() {
        return ApiResponse.ok(executionPlanService.getDashboard());
    }
    @GetMapping("/api/governance-execution/report")
    public ApiResponse<String> getReport() {
        return ApiResponse.ok(executionPlanService.getReport());
    }

    // ========== Handoff Checklist ==========
    @PostMapping("/api/governance-execution/handoffs")
    public ApiResponse<GovernanceHandoffChecklistResponse> createHandoff(@RequestParam String recommendationId,
                                                                           @RequestParam(required = false) String executionPlanId,
                                                                           @RequestParam(required = false) String fromOwnerName,
                                                                           @RequestParam(required = false) String toOwnerName) {
        return ApiResponse.ok(handoffAssistantService.createChecklist(recommendationId, executionPlanId, fromOwnerName, toOwnerName));
    }
    @GetMapping("/api/governance-execution/handoffs")
    public ApiResponse<List<GovernanceHandoffChecklistResponse>> listHandoffs() {
        return ApiResponse.ok(handoffAssistantService.listChecklists());
    }
    @GetMapping("/api/governance-execution/handoffs/{checklistId}")
    public ApiResponse<GovernanceHandoffChecklistResponse> getHandoff(@PathVariable String checklistId) {
        return ApiResponse.ok(handoffAssistantService.getChecklist(checklistId));
    }
    @PutMapping("/api/governance-execution/handoffs/{checklistId}")
    public ApiResponse<GovernanceHandoffChecklistResponse> updateHandoff(@PathVariable String checklistId,
                                                                          @RequestParam(required = false) String handoffNote) {
        return ApiResponse.ok(handoffAssistantService.updateChecklist(checklistId, handoffNote));
    }
    @PostMapping("/api/governance-execution/handoffs/{checklistId}/status")
    public ApiResponse<GovernanceHandoffChecklistResponse> updateHandoffStatus(@PathVariable String checklistId,
                                                                                @RequestParam String status) {
        return ApiResponse.ok(handoffAssistantService.updateChecklistStatus(checklistId, status));
    }
}
