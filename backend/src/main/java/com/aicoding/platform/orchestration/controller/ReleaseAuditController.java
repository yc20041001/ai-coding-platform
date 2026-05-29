package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ReleaseAuditTrailService;
import com.aicoding.platform.orchestration.application.ReleasePostmortemReviewService;
import com.aicoding.platform.orchestration.application.ReleaseRollbackDrillService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReleaseAuditController {

    private final ReleaseRollbackDrillService releaseRollbackDrillService;
    private final ReleaseAuditTrailService releaseAuditTrailService;
    private final ReleasePostmortemReviewService releasePostmortemReviewService;

    public ReleaseAuditController(ReleaseRollbackDrillService releaseRollbackDrillService,
                                  ReleaseAuditTrailService releaseAuditTrailService,
                                  ReleasePostmortemReviewService releasePostmortemReviewService) {
        this.releaseRollbackDrillService = releaseRollbackDrillService;
        this.releaseAuditTrailService = releaseAuditTrailService;
        this.releasePostmortemReviewService = releasePostmortemReviewService;
    }

    // ========== Rollback Drills ==========

    @PostMapping("/api/release-rollouts/{planId}/rollback-drills")
    public ApiResponse<ReleaseRollbackDrillResponse> createDrill(@PathVariable String planId,
                                                                  @RequestBody CreateReleaseRollbackDrillRequest request) {
        request.setPlanId(planId);
        return ApiResponse.ok(releaseRollbackDrillService.createDrill(request));
    }

    @GetMapping("/api/release-rollouts/{planId}/rollback-drills")
    public ApiResponse<List<ReleaseRollbackDrillResponse>> listDrills(@PathVariable String planId) {
        return ApiResponse.ok(releaseRollbackDrillService.listDrills(planId));
    }

    @GetMapping("/api/release-rollouts/{planId}/rollback-drills/{drillId}")
    public ApiResponse<ReleaseRollbackDrillResponse> getDrill(@PathVariable String planId,
                                                               @PathVariable String drillId) {
        return ApiResponse.ok(releaseRollbackDrillService.getDrill(drillId));
    }

    @PutMapping("/api/release-rollouts/{planId}/rollback-drills/{drillId}")
    public ApiResponse<ReleaseRollbackDrillResponse> updateDrill(@PathVariable String planId,
                                                                  @PathVariable String drillId,
                                                                  @RequestBody UpdateReleaseRollbackDrillRequest request) {
        return ApiResponse.ok(releaseRollbackDrillService.updateDrill(drillId, request));
    }

    @PostMapping("/api/release-rollouts/{planId}/rollback-drills/{drillId}/status")
    public ApiResponse<ReleaseRollbackDrillResponse> updateDrillStatus(@PathVariable String planId,
                                                                        @PathVariable String drillId,
                                                                        @RequestParam String drillStatus) {
        return ApiResponse.ok(releaseRollbackDrillService.updateDrillStatus(drillId, drillStatus));
    }

    @GetMapping("/api/release-rollouts/{planId}/rollback-drills/readiness")
    public ApiResponse<Boolean> checkRollbackReadiness(@PathVariable String planId) {
        Long planIdLong = Long.valueOf(planId);
        return ApiResponse.ok(releaseRollbackDrillService.isRollbackReady(planIdLong));
    }

    // ========== Audit Events ==========

    @GetMapping("/api/release-rollouts/{planId}/audit-events")
    public ApiResponse<List<ReleaseAuditEventResponse>> listAuditEvents(@PathVariable String planId) {
        return ApiResponse.ok(releaseAuditTrailService.listEvents(planId));
    }

    @GetMapping("/api/release-rollouts/{planId}/audit-timeline")
    public ApiResponse<ReleaseAuditTimelineResponse> getAuditTimeline(@PathVariable String planId) {
        return ApiResponse.ok(releaseAuditTrailService.getTimeline(planId));
    }

    @GetMapping("/api/release-rollouts/{planId}/audit-report")
    public ApiResponse<ReleaseAuditReportResponse> generateAuditReport(@PathVariable String planId) {
        return ApiResponse.ok(releaseAuditTrailService.generateAuditReport(planId));
    }

    // ========== Postmortem Reviews ==========

    @PostMapping("/api/release-rollouts/{planId}/postmortem-review")
    public ApiResponse<ReleasePostmortemReviewResponse> createPostmortemReview(@PathVariable String planId,
                                                                                @RequestBody CreateReleasePostmortemReviewRequest request) {
        request.setPlanId(planId);
        return ApiResponse.ok(releasePostmortemReviewService.createReview(request));
    }

    @GetMapping("/api/release-rollouts/{planId}/postmortem-review")
    public ApiResponse<ReleasePostmortemReviewResponse> getPostmortemReview(@PathVariable String planId) {
        return ApiResponse.ok(releasePostmortemReviewService.getReview(planId));
    }

    @PutMapping("/api/release-rollouts/{planId}/postmortem-review/{reviewId}")
    public ApiResponse<ReleasePostmortemReviewResponse> updatePostmortemReview(@PathVariable String planId,
                                                                                @PathVariable String reviewId,
                                                                                @RequestBody UpdateReleasePostmortemReviewRequest request) {
        return ApiResponse.ok(releasePostmortemReviewService.updateReview(reviewId, request));
    }

    @PostMapping("/api/release-rollouts/{planId}/postmortem-review/{reviewId}/status")
    public ApiResponse<ReleasePostmortemReviewResponse> updatePostmortemReviewStatus(@PathVariable String planId,
                                                                                      @PathVariable String reviewId,
                                                                                      @RequestParam String reviewStatus) {
        return ApiResponse.ok(releasePostmortemReviewService.updateReviewStatus(reviewId, reviewStatus));
    }

    @GetMapping("/api/release-rollouts/{planId}/postmortem-review/prefill")
    public ApiResponse<ReleasePostmortemReviewResponse> getPrefilledPostmortemReview(@PathVariable String planId) {
        return ApiResponse.ok(releasePostmortemReviewService.getPrefilledReview(planId));
    }
}
