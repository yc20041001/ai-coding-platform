package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ReleaseReadinessReportService;
import com.aicoding.platform.orchestration.application.ReleaseRolloutPlanService;
import com.aicoding.platform.orchestration.application.ReleaseRolloutStepService;
import com.aicoding.platform.orchestration.application.ReleaseVerificationService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReleaseRolloutController {

    private final ReleaseRolloutPlanService releaseRolloutPlanService;
    private final ReleaseRolloutStepService releaseRolloutStepService;
    private final ReleaseVerificationService releaseVerificationService;
    private final ReleaseReadinessReportService releaseReadinessReportService;

    public ReleaseRolloutController(ReleaseRolloutPlanService releaseRolloutPlanService,
                                    ReleaseRolloutStepService releaseRolloutStepService,
                                    ReleaseVerificationService releaseVerificationService,
                                    ReleaseReadinessReportService releaseReadinessReportService) {
        this.releaseRolloutPlanService = releaseRolloutPlanService;
        this.releaseRolloutStepService = releaseRolloutStepService;
        this.releaseVerificationService = releaseVerificationService;
        this.releaseReadinessReportService = releaseReadinessReportService;
    }

    // ========== Rollout Plans ==========

    @PostMapping("/api/projects/{projectId}/rollout/plans")
    public ApiResponse<ReleaseRolloutPlanResponse> createPlan(@PathVariable String projectId,
                                                               @RequestBody CreateReleaseRolloutPlanRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(releaseRolloutPlanService.createPlan(request));
    }

    @GetMapping("/api/projects/{projectId}/rollout/plans")
    public ApiResponse<List<ReleaseRolloutPlanResponse>> listPlans(@PathVariable String projectId) {
        return ApiResponse.ok(releaseRolloutPlanService.listPlans(projectId));
    }

    @GetMapping("/api/projects/{projectId}/rollout/plans/{planId}")
    public ApiResponse<ReleaseRolloutPlanResponse> getPlan(@PathVariable String projectId,
                                                            @PathVariable String planId) {
        return ApiResponse.ok(releaseRolloutPlanService.getPlan(planId));
    }

    @PutMapping("/api/projects/{projectId}/rollout/plans/{planId}")
    public ApiResponse<ReleaseRolloutPlanResponse> updatePlan(@PathVariable String projectId,
                                                               @PathVariable String planId,
                                                               @RequestBody UpdateReleaseRolloutPlanRequest request) {
        return ApiResponse.ok(releaseRolloutPlanService.updatePlan(planId, request));
    }

    @PutMapping("/api/projects/{projectId}/rollout/plans/{planId}/status")
    public ApiResponse<ReleaseRolloutPlanResponse> updatePlanStatus(@PathVariable String projectId,
                                                                     @PathVariable String planId,
                                                                     @RequestParam String status) {
        return ApiResponse.ok(releaseRolloutPlanService.updatePlanStatus(planId, status));
    }

    // ========== Rollout Steps ==========

    @GetMapping("/api/projects/{projectId}/rollout/plans/{planId}/steps")
    public ApiResponse<List<ReleaseRolloutStepResponse>> listSteps(@PathVariable String projectId,
                                                                    @PathVariable String planId) {
        return ApiResponse.ok(releaseRolloutStepService.listSteps(planId));
    }

    @PostMapping("/api/projects/{projectId}/rollout/plans/{planId}/steps")
    public ApiResponse<ReleaseRolloutStepResponse> createStep(@PathVariable String projectId,
                                                               @PathVariable String planId,
                                                               @RequestBody CreateReleaseRolloutStepRequest request) {
        request.setPlanId(planId);
        request.setProjectId(projectId);
        return ApiResponse.ok(releaseRolloutStepService.createStep(request));
    }

    @PutMapping("/api/projects/{projectId}/rollout/plans/{planId}/steps/{stepId}")
    public ApiResponse<ReleaseRolloutStepResponse> updateStep(@PathVariable String projectId,
                                                               @PathVariable String planId,
                                                               @PathVariable String stepId,
                                                               @RequestBody UpdateReleaseRolloutStepRequest request) {
        return ApiResponse.ok(releaseRolloutStepService.updateStep(stepId, request));
    }

    @PutMapping("/api/projects/{projectId}/rollout/plans/{planId}/steps/{stepId}/status")
    public ApiResponse<ReleaseRolloutStepResponse> updateStepStatus(@PathVariable String projectId,
                                                                     @PathVariable String planId,
                                                                     @PathVariable String stepId,
                                                                     @RequestParam String stepStatus,
                                                                     @RequestParam(required = false) String actualResult,
                                                                     @RequestParam(required = false) String evidenceJson,
                                                                     @RequestParam(required = false) String operatorId) {
        return ApiResponse.ok(releaseRolloutStepService.updateStepStatus(stepId, stepStatus, actualResult, evidenceJson, operatorId));
    }

    // ========== Verification Records ==========

    @GetMapping("/api/projects/{projectId}/rollout/plans/{planId}/verifications")
    public ApiResponse<List<ReleaseVerificationRecordResponse>> listVerifications(@PathVariable String projectId,
                                                                                    @PathVariable String planId,
                                                                                    @RequestParam(required = false) String phase) {
        return ApiResponse.ok(releaseVerificationService.listVerifications(planId, phase));
    }

    @PostMapping("/api/projects/{projectId}/rollout/plans/{planId}/verifications")
    public ApiResponse<ReleaseVerificationRecordResponse> createVerification(@PathVariable String projectId,
                                                                              @PathVariable String planId,
                                                                              @RequestBody CreateReleaseVerificationRecordRequest request) {
        request.setPlanId(planId);
        request.setProjectId(projectId);
        return ApiResponse.ok(releaseVerificationService.createVerification(request));
    }

    @PutMapping("/api/projects/{projectId}/rollout/plans/{planId}/verifications/{recordId}")
    public ApiResponse<ReleaseVerificationRecordResponse> updateVerification(@PathVariable String projectId,
                                                                              @PathVariable String planId,
                                                                              @PathVariable String recordId,
                                                                              @RequestBody UpdateReleaseVerificationRecordRequest request) {
        return ApiResponse.ok(releaseVerificationService.updateVerification(recordId, request));
    }

    // ========== Dashboard, Summary, Report ==========

    @GetMapping("/api/projects/{projectId}/rollout/readiness-dashboard")
    public ApiResponse<ReleaseReadinessDashboardResponse> getDashboard(@PathVariable String projectId,
                                                                        @RequestParam(required = false) String releaseLabel) {
        return ApiResponse.ok(releaseReadinessReportService.getDashboard(projectId, releaseLabel));
    }

    @GetMapping("/api/projects/{projectId}/rollout/plans/{planId}/summary")
    public ApiResponse<ReleaseRolloutSummaryResponse> getSummary(@PathVariable String projectId,
                                                                  @PathVariable String planId) {
        return ApiResponse.ok(releaseReadinessReportService.getSummary(planId));
    }

    @GetMapping("/api/projects/{projectId}/rollout/plans/{planId}/report")
    public ApiResponse<ReleaseReadinessReportResponse> generateReport(@PathVariable String projectId,
                                                                       @PathVariable String planId) {
        return ApiResponse.ok(releaseReadinessReportService.generateReport(planId));
    }
}
