package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernanceDraftPlanningController {

    private final GovernanceDraftPlanningService draftPlanningService;
    private final GovernanceSafeAssistiveActionService assistiveActionService;
    private final GovernanceRecommendationPackageService packageService;

    public GovernanceDraftPlanningController(GovernanceDraftPlanningService draftPlanningService,
                                              GovernanceSafeAssistiveActionService assistiveActionService,
                                              GovernanceRecommendationPackageService packageService) {
        this.draftPlanningService = draftPlanningService;
        this.assistiveActionService = assistiveActionService;
        this.packageService = packageService;
    }

    // ========== Draft Plan ==========
    @PostMapping("/api/governance-draft-plans")
    public ApiResponse<GovernanceDraftRemediationPlanResponse> createPlan(@RequestParam String planTitle,
                                                                            @RequestParam(required = false) String scopeType) {
        return ApiResponse.ok(draftPlanningService.createPlan(planTitle, scopeType));
    }

    @GetMapping("/api/governance-draft-plans")
    public ApiResponse<List<GovernanceDraftRemediationPlanResponse>> listPlans() {
        return ApiResponse.ok(draftPlanningService.listPlans());
    }

    @GetMapping("/api/governance-draft-plans/{planId}")
    public ApiResponse<GovernanceDraftRemediationPlanResponse> getPlan(@PathVariable String planId) {
        return ApiResponse.ok(draftPlanningService.getPlan(planId));
    }

    @PutMapping("/api/governance-draft-plans/{planId}")
    public ApiResponse<GovernanceDraftRemediationPlanResponse> updatePlan(@PathVariable String planId,
                                                                           @RequestParam(required = false) String planTitle,
                                                                           @RequestParam(required = false) String summaryText,
                                                                           @RequestParam(required = false) String goalText,
                                                                           @RequestParam(required = false) String proposedStepsJson) {
        return ApiResponse.ok(draftPlanningService.updatePlan(planId, planTitle, summaryText, goalText, proposedStepsJson));
    }

    @PostMapping("/api/governance-draft-plans/{planId}/status")
    public ApiResponse<GovernanceDraftRemediationPlanResponse> updatePlanStatus(@PathVariable String planId,
                                                                                  @RequestParam String status) {
        return ApiResponse.ok(draftPlanningService.updatePlanStatus(planId, status));
    }

    @PostMapping("/api/governance-draft-plans/{planId}/refresh")
    public ApiResponse<GovernanceDraftRemediationPlanResponse> refreshPlan(@PathVariable String planId) {
        return ApiResponse.ok(draftPlanningService.refreshPlan(planId));
    }

    // ========== Safe Assistive Actions ==========
    @GetMapping("/api/governance-draft-plans/{planId}/assistive-actions")
    public ApiResponse<List<GovernanceSafeAssistiveActionResponse>> listAssistiveActions(@PathVariable String planId) {
        return ApiResponse.ok(assistiveActionService.listActions(planId));
    }

    @PostMapping("/api/governance-draft-plans/{planId}/assistive-actions/generate")
    public ApiResponse<List<GovernanceSafeAssistiveActionResponse>> generateActions(@PathVariable String planId) {
        return ApiResponse.ok(assistiveActionService.generateActions(planId));
    }

    @PostMapping("/api/governance-assistive-actions/{actionId}/status")
    public ApiResponse<GovernanceSafeAssistiveActionResponse> updateActionStatus(@PathVariable String actionId,
                                                                                  @RequestParam String status) {
        return ApiResponse.ok(assistiveActionService.updateActionStatus(actionId, status));
    }

    // ========== Recommendation Package ==========
    @GetMapping("/api/governance-recommendation-packages")
    public ApiResponse<List<GovernanceRecommendationPackageResponse>> listPackages() {
        return ApiResponse.ok(packageService.listPackages());
    }

    @GetMapping("/api/governance-recommendation-packages/{packageId}")
    public ApiResponse<GovernanceRecommendationPackageResponse> getPackage(@PathVariable String packageId) {
        return ApiResponse.ok(packageService.getPackage(packageId));
    }

    @PostMapping("/api/governance-recommendation-packages/{packageId}/status")
    public ApiResponse<GovernanceRecommendationPackageResponse> updatePackageStatus(@PathVariable String packageId,
                                                                                     @RequestParam String status) {
        return ApiResponse.ok(packageService.updatePackageStatus(packageId, status));
    }

    // ========== Dashboard & Report ==========
    @GetMapping("/api/governance-draft-planning/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        var plans = draftPlanningService.listPlans();
        var packages = packageService.listPackages();
        long readyReview = plans.stream().filter(p -> "READY_FOR_REVIEW".equals(p.getPlanStatus())).count();
        long submitReady = packages.stream().filter(p -> Boolean.TRUE.equals(p.getSubmitReadyFlag())).count();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("draftPlanCount", plans.size());
        resp.put("readyForReviewCount", readyReview);
        resp.put("submitReadyPackageCount", submitReady);
        resp.put("topDraftPlans", plans.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        resp.put("topPackages", packages.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-draft-planning/report")
    public ApiResponse<String> getReport() {
        var plans = draftPlanningService.listPlans();
        StringBuilder md = new StringBuilder();
        md.append("# Governance Draft Planning Report\n\n");
        md.append("Total Draft Plans: ").append(plans.size()).append("\n\n");
        for (var p : plans) {
            md.append("- **").append(p.getPlanTitle()).append("** (").append(p.getPlanStatus()).append(") — risk: ").append(p.getRiskLevel()).append("\n");
        }
        return ApiResponse.ok(md.toString());
    }
}
