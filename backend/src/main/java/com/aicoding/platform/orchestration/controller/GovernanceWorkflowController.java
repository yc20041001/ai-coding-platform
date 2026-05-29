package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.GovernanceRecommendationWorkflowService;
import com.aicoding.platform.orchestration.application.GovernanceWaiverManagementService;
import com.aicoding.platform.orchestration.application.GovernanceWorkflowSummaryService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceWorkflowController {

    private final GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService;
    private final GovernanceWaiverManagementService governanceWaiverManagementService;
    private final GovernanceWorkflowSummaryService governanceWorkflowSummaryService;

    public GovernanceWorkflowController(GovernanceRecommendationWorkflowService governanceRecommendationWorkflowService,
                                         GovernanceWaiverManagementService governanceWaiverManagementService,
                                         GovernanceWorkflowSummaryService governanceWorkflowSummaryService) {
        this.governanceRecommendationWorkflowService = governanceRecommendationWorkflowService;
        this.governanceWaiverManagementService = governanceWaiverManagementService;
        this.governanceWorkflowSummaryService = governanceWorkflowSummaryService;
    }

    // ========== Recommendation Workflow ==========

    @PostMapping("/api/governance-workflow/recommendations/sync")
    public ApiResponse<String> syncRecommendations() {
        int count = governanceRecommendationWorkflowService.syncRecommendations();
        return ApiResponse.ok("Synced " + count + " recommendations");
    }

    @GetMapping("/api/governance-workflow/recommendations")
    public ApiResponse<List<GovernanceRecommendationItemResponse>> listRecommendations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ApiResponse.ok(governanceRecommendationWorkflowService.listItems(status, priority));
    }

    @GetMapping("/api/governance-workflow/recommendations/{itemId}")
    public ApiResponse<GovernanceRecommendationItemResponse> getRecommendation(@PathVariable String itemId) {
        return ApiResponse.ok(governanceRecommendationWorkflowService.getItem(itemId));
    }

    @PutMapping("/api/governance-workflow/recommendations/{itemId}")
    public ApiResponse<GovernanceRecommendationItemResponse> updateRecommendation(@PathVariable String itemId,
                                                                                   @RequestBody UpdateGovernanceRecommendationItemRequest request) {
        return ApiResponse.ok(governanceRecommendationWorkflowService.updateItem(itemId, request));
    }

    @PostMapping("/api/governance-workflow/recommendations/{itemId}/status")
    public ApiResponse<GovernanceRecommendationItemResponse> updateRecommendationStatus(@PathVariable String itemId,
                                                                                        @RequestParam String status) {
        return ApiResponse.ok(governanceRecommendationWorkflowService.updateItemStatus(itemId, status));
    }

    // ========== Waiver ==========

    @PostMapping("/api/governance-workflow/recommendations/{itemId}/waivers")
    public ApiResponse<GovernanceWaiverRequestResponse> createWaiver(@PathVariable String itemId,
                                                                      @RequestBody CreateGovernanceWaiverRequestRequest request) {
        return ApiResponse.ok(governanceWaiverManagementService.createWaiver(itemId, request));
    }

    @GetMapping("/api/governance-workflow/recommendations/{itemId}/waivers")
    public ApiResponse<List<GovernanceWaiverRequestResponse>> listWaivers(@PathVariable String itemId) {
        return ApiResponse.ok(governanceWaiverManagementService.listWaivers(itemId));
    }

    @PutMapping("/api/governance-workflow/waivers/{waiverId}")
    public ApiResponse<GovernanceWaiverRequestResponse> updateWaiver(@PathVariable String waiverId,
                                                                      @RequestBody UpdateGovernanceWaiverRequestRequest request) {
        return ApiResponse.ok(governanceWaiverManagementService.updateWaiver(waiverId, request));
    }

    @PostMapping("/api/governance-workflow/waivers/{waiverId}/status")
    public ApiResponse<GovernanceWaiverRequestResponse> updateWaiverStatus(@PathVariable String waiverId,
                                                                            @RequestParam String status,
                                                                            @RequestParam(required = false) String approvalNote) {
        return ApiResponse.ok(governanceWaiverManagementService.updateWaiverStatus(waiverId, status, approvalNote));
    }

    @PostMapping("/api/governance-workflow/waivers/scan-expiry")
    public ApiResponse<String> scanExpiredWaivers() {
        int count = governanceWaiverManagementService.scanExpiredWaivers();
        return ApiResponse.ok("Expired " + count + " waivers");
    }

    // ========== Workflow Snapshot & Summary ==========

    @PostMapping("/api/governance-workflow/snapshots/refresh")
    public ApiResponse<String> refreshSnapshot() {
        governanceWorkflowSummaryService.refreshSnapshot();
        return ApiResponse.ok("Workflow snapshot refreshed");
    }

    @GetMapping("/api/governance-workflow/dashboard")
    public ApiResponse<GovernanceWorkflowDashboardResponse> getDashboard() {
        return ApiResponse.ok(governanceWorkflowSummaryService.getDashboard());
    }

    @GetMapping("/api/governance-workflow/summary")
    public ApiResponse<GovernanceWorkflowSummaryResponse> getSummary() {
        return ApiResponse.ok(governanceWorkflowSummaryService.getSummary());
    }

    @GetMapping("/api/governance-workflow/report")
    public ApiResponse<GovernanceWorkflowSummaryResponse> getReport() {
        return ApiResponse.ok(governanceWorkflowSummaryService.getSummary());
    }
}
