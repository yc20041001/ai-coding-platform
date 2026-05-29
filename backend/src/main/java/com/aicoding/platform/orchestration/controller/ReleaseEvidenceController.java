package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ReleaseEvidenceCenterService;
import com.aicoding.platform.orchestration.application.ReleaseExecutiveSummaryService;
import com.aicoding.platform.orchestration.application.ReleaseSignoffService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReleaseEvidenceController {

    private final ReleaseEvidenceCenterService releaseEvidenceCenterService;
    private final ReleaseSignoffService releaseSignoffService;
    private final ReleaseExecutiveSummaryService releaseExecutiveSummaryService;

    public ReleaseEvidenceController(ReleaseEvidenceCenterService releaseEvidenceCenterService,
                                      ReleaseSignoffService releaseSignoffService,
                                      ReleaseExecutiveSummaryService releaseExecutiveSummaryService) {
        this.releaseEvidenceCenterService = releaseEvidenceCenterService;
        this.releaseSignoffService = releaseSignoffService;
        this.releaseExecutiveSummaryService = releaseExecutiveSummaryService;
    }

    // ========== Evidence Bundle ==========

    @PostMapping("/api/release-rollouts/{planId}/evidence-bundle/generate")
    public ApiResponse<ReleaseEvidenceBundleResponse> generateBundle(@PathVariable String planId,
                                                                      @RequestBody GenerateReleaseEvidenceBundleRequest request) {
        return ApiResponse.ok(releaseEvidenceCenterService.generateBundle(planId, request));
    }

    @GetMapping("/api/release-rollouts/{planId}/evidence-bundle")
    public ApiResponse<ReleaseEvidenceBundleResponse> getBundle(@PathVariable String planId) {
        return ApiResponse.ok(releaseEvidenceCenterService.getBundle(planId));
    }

    @PostMapping("/api/release-rollouts/{planId}/evidence-bundle/status")
    public ApiResponse<ReleaseEvidenceBundleResponse> updateBundleStatus(@PathVariable String planId,
                                                                          @RequestParam String bundleStatus) {
        return ApiResponse.ok(releaseEvidenceCenterService.updateBundleStatus(planId, bundleStatus));
    }

    // ========== Sign-off ==========

    @GetMapping("/api/release-rollouts/{planId}/signoffs")
    public ApiResponse<List<ReleaseSignoffRecordResponse>> listSignoffs(@PathVariable String planId) {
        return ApiResponse.ok(releaseSignoffService.listSignoffs(planId));
    }

    @PostMapping("/api/release-rollouts/{planId}/signoffs")
    public ApiResponse<ReleaseSignoffRecordResponse> createSignoff(@PathVariable String planId,
                                                                    @RequestBody CreateReleaseSignoffRecordRequest request) {
        return ApiResponse.ok(releaseSignoffService.createSignoff(planId, request));
    }

    @PutMapping("/api/release-rollouts/{planId}/signoffs/{signoffId}")
    public ApiResponse<ReleaseSignoffRecordResponse> updateSignoff(@PathVariable String planId,
                                                                     @PathVariable String signoffId,
                                                                     @RequestBody UpdateReleaseSignoffRecordRequest request) {
        return ApiResponse.ok(releaseSignoffService.updateSignoff(signoffId, request));
    }

    @PostMapping("/api/release-rollouts/{planId}/signoffs/{signoffId}/status")
    public ApiResponse<ReleaseSignoffRecordResponse> updateSignoffStatus(@PathVariable String planId,
                                                                          @PathVariable String signoffId,
                                                                          @RequestParam String signoffStatus) {
        return ApiResponse.ok(releaseSignoffService.updateSignoffStatus(signoffId, signoffStatus));
    }

    // ========== Executive Summary / Confidence / Comparison / Trend / Report ==========

    @GetMapping("/api/release-rollouts/{planId}/executive-summary")
    public ApiResponse<ReleaseExecutiveSummaryResponse> getExecutiveSummary(@PathVariable String planId) {
        return ApiResponse.ok(releaseExecutiveSummaryService.getExecutiveSummary(planId));
    }

    @GetMapping("/api/release-rollouts/{planId}/confidence-snapshot")
    public ApiResponse<ReleaseConfidenceSnapshotResponse> getConfidenceSnapshot(@PathVariable String planId) {
        return ApiResponse.ok(releaseExecutiveSummaryService.getConfidenceSnapshot(planId));
    }

    @GetMapping("/api/release-rollouts/{planId}/comparison")
    public ApiResponse<ReleaseComparisonResponse> getComparison(@PathVariable String planId) {
        return ApiResponse.ok(releaseExecutiveSummaryService.getComparison(planId));
    }

    @GetMapping("/api/release-confidence/trend")
    public ApiResponse<List<ReleaseConfidenceTrendResponse>> getTrend() {
        return ApiResponse.ok(releaseExecutiveSummaryService.getTrend());
    }

    @GetMapping("/api/release-rollouts/{planId}/executive-report")
    public ApiResponse<ReleaseExecutiveReportResponse> generateExecutiveReport(@PathVariable String planId) {
        return ApiResponse.ok(releaseExecutiveSummaryService.generateExecutiveReport(planId));
    }

    @PostMapping("/api/release-rollouts/{planId}/confidence-snapshot")
    public ApiResponse<ReleaseConfidenceSnapshotResponse> takeConfidenceSnapshot(@PathVariable String planId) {
        return ApiResponse.ok(releaseExecutiveSummaryService.takeConfidenceSnapshot(planId));
    }
}
