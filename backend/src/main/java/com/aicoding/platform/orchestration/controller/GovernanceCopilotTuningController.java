package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GovernanceCopilotTuningController {

    private final GovernanceOperatorFeedbackService feedbackService;
    private final GovernanceAdaptiveGuidanceService adaptiveGuidanceService;
    private final GovernanceCopilotTuningService copilotTuningService;

    public GovernanceCopilotTuningController(GovernanceOperatorFeedbackService feedbackService,
                                              GovernanceAdaptiveGuidanceService adaptiveGuidanceService,
                                              GovernanceCopilotTuningService copilotTuningService) {
        this.feedbackService = feedbackService;
        this.adaptiveGuidanceService = adaptiveGuidanceService;
        this.copilotTuningService = copilotTuningService;
    }

    // ========== Feedback ==========
    @PostMapping("/api/governance-copilot/feedback")
    public ApiResponse<GovernanceOperatorFeedbackResponse> recordFeedback(@RequestParam String sessionId,
                                                                           @RequestParam String feedbackTargetType,
                                                                           @RequestParam int feedbackRating,
                                                                           @RequestParam(required = false) Boolean helpfulFlag,
                                                                           @RequestParam(required = false) Boolean acceptedFlag,
                                                                           @RequestParam(required = false) String reasonCode,
                                                                           @RequestParam(required = false) String noteText) {
        return ApiResponse.ok(feedbackService.recordFeedback(sessionId, feedbackTargetType, feedbackRating,
                helpfulFlag, acceptedFlag, reasonCode, noteText));
    }

    @GetMapping("/api/governance-copilot/feedback")
    public ApiResponse<List<GovernanceOperatorFeedbackResponse>> listFeedback(@RequestParam(required = false) String sessionId) {
        if (sessionId != null) return ApiResponse.ok(feedbackService.listFeedback(sessionId));
        return ApiResponse.ok(feedbackService.listAllFeedback());
    }

    // ========== Adaptive Signals ==========
    @PostMapping("/api/governance-copilot/signals/refresh")
    public ApiResponse<String> refreshSignals() {
        adaptiveGuidanceService.refreshSignals();
        return ApiResponse.ok("Adaptive signals refreshed");
    }

    @GetMapping("/api/governance-copilot/signals")
    public ApiResponse<List<GovernanceAdaptiveGuidanceSignalResponse>> listSignals() {
        return ApiResponse.ok(adaptiveGuidanceService.listSignals());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-copilot/signals/dashboard")
    public ApiResponse<Map<String, Object>> getSignalDashboard() {
        return ApiResponse.ok(adaptiveGuidanceService.getDashboard());
    }

    // ========== Tuning Snapshot ==========
    @PostMapping("/api/governance-copilot/tuning/refresh")
    public ApiResponse<String> refreshTuning() {
        copilotTuningService.refreshSnapshot();
        return ApiResponse.ok("Tuning snapshot refreshed");
    }

    @GetMapping("/api/governance-copilot/tuning/snapshots")
    public ApiResponse<List<GovernanceCopilotTuningSnapshotResponse>> listSnapshots() {
        return ApiResponse.ok(copilotTuningService.listSnapshots());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-copilot/tuning/dashboard")
    public ApiResponse<Map<String, Object>> getTuningDashboard() {
        return ApiResponse.ok(copilotTuningService.getDashboard());
    }

    @GetMapping("/api/governance-copilot/tuning/report")
    public ApiResponse<String> getReport() {
        return ApiResponse.ok(copilotTuningService.getReport());
    }
}
