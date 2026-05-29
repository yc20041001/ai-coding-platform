package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.BetaEnvironmentReadinessService;
import com.aicoding.platform.orchestration.application.BetaTrialFeedbackService;
import com.aicoding.platform.orchestration.application.BetaTrialSessionService;
import com.aicoding.platform.orchestration.dto.BetaEnvironmentReadinessResponse;
import com.aicoding.platform.orchestration.dto.BetaPassBlockSummaryResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialDashboardResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialFeedbackResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialFeedbackSummaryResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialSessionResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialSessionSummaryResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaEnvironmentReadinessRequest;
import com.aicoding.platform.orchestration.dto.CreateBetaTrialFeedbackRequest;
import com.aicoding.platform.orchestration.dto.CreateBetaTrialSessionRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaTrialFeedbackRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaTrialSessionRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BetaTrialController {

    private final BetaTrialSessionService betaTrialSessionService;
    private final BetaTrialFeedbackService betaTrialFeedbackService;
    private final BetaEnvironmentReadinessService betaEnvironmentReadinessService;

    public BetaTrialController(BetaTrialSessionService betaTrialSessionService,
                               BetaTrialFeedbackService betaTrialFeedbackService,
                               BetaEnvironmentReadinessService betaEnvironmentReadinessService) {
        this.betaTrialSessionService = betaTrialSessionService;
        this.betaTrialFeedbackService = betaTrialFeedbackService;
        this.betaEnvironmentReadinessService = betaEnvironmentReadinessService;
    }

    // ========== Beta Trial Sessions ==========

    @PostMapping("/api/beta-sessions")
    public ApiResponse<BetaTrialSessionResponse> createSession(@RequestBody CreateBetaTrialSessionRequest request) {
        return ApiResponse.ok(betaTrialSessionService.createSession(request));
    }

    @PutMapping("/api/beta-sessions/{id}")
    public ApiResponse<BetaTrialSessionResponse> updateSession(@PathVariable String id,
                                                               @RequestBody UpdateBetaTrialSessionRequest request) {
        return ApiResponse.ok(betaTrialSessionService.updateSession(id, request));
    }

    @GetMapping("/api/beta-sessions/{id}")
    public ApiResponse<BetaTrialSessionResponse> getSession(@PathVariable String id) {
        return ApiResponse.ok(betaTrialSessionService.getSession(id));
    }

    @GetMapping("/api/projects/{projectId}/beta-sessions")
    public ApiResponse<List<BetaTrialSessionSummaryResponse>> listSessions(@PathVariable String projectId) {
        return ApiResponse.ok(betaTrialSessionService.listSessions(projectId));
    }

    @GetMapping("/api/beta-sessions/{id}/export-markdown")
    public ApiResponse<String> exportSessionMarkdown(@PathVariable String id) {
        return ApiResponse.ok(betaTrialSessionService.exportSessionMarkdown(id));
    }

    // ========== Beta Trial Feedback ==========

    @PostMapping("/api/beta-sessions/{sessionId}/feedback")
    public ApiResponse<BetaTrialFeedbackResponse> createFeedback(@PathVariable String sessionId,
                                                                 @RequestBody CreateBetaTrialFeedbackRequest request) {
        return ApiResponse.ok(betaTrialFeedbackService.createFeedback(sessionId, request));
    }

    @PutMapping("/api/beta-feedback/{id}")
    public ApiResponse<BetaTrialFeedbackResponse> updateFeedback(@PathVariable String id,
                                                                 @RequestBody UpdateBetaTrialFeedbackRequest request) {
        return ApiResponse.ok(betaTrialFeedbackService.updateFeedback(id, request));
    }

    @GetMapping("/api/beta-feedback/{id}")
    public ApiResponse<BetaTrialFeedbackResponse> getFeedback(@PathVariable String id) {
        return ApiResponse.ok(betaTrialFeedbackService.getFeedback(id));
    }

    @GetMapping("/api/beta-sessions/{sessionId}/feedback")
    public ApiResponse<List<BetaTrialFeedbackSummaryResponse>> listFeedback(@PathVariable String sessionId,
                                                                             @RequestParam(required = false) String severity,
                                                                             @RequestParam(required = false) String triageStatus) {
        return ApiResponse.ok(betaTrialFeedbackService.listFeedback(sessionId, severity, triageStatus));
    }

    @GetMapping("/api/beta-sessions/{sessionId}/feedback/pass-block-summary")
    public ApiResponse<BetaPassBlockSummaryResponse> getPassBlockSummary(@PathVariable String sessionId) {
        return ApiResponse.ok(betaTrialFeedbackService.getPassBlockSummary(sessionId));
    }

    @DeleteMapping("/api/beta-feedback/{id}")
    public ApiResponse<Void> deleteFeedback(@PathVariable String id) {
        betaTrialFeedbackService.deleteFeedback(id);
        return ApiResponse.ok();
    }

    // ========== Environment Readiness ==========

    @PostMapping("/api/projects/{projectId}/environment-readiness")
    public ApiResponse<BetaEnvironmentReadinessResponse> createCheck(@PathVariable String projectId,
                                                                     @RequestBody CreateBetaEnvironmentReadinessRequest request) {
        request.setProjectId(projectId);
        return ApiResponse.ok(betaEnvironmentReadinessService.createCheck(request));
    }

    @GetMapping("/api/environment-readiness/{id}")
    public ApiResponse<BetaEnvironmentReadinessResponse> getCheck(@PathVariable String id) {
        return ApiResponse.ok(betaEnvironmentReadinessService.getCheck(id));
    }

    @GetMapping("/api/environment-readiness")
    public ApiResponse<List<BetaEnvironmentReadinessResponse>> listChecks(@RequestParam(required = false) String projectId,
                                                                          @RequestParam(required = false) String sessionId) {
        return ApiResponse.ok(betaEnvironmentReadinessService.listChecks(projectId, sessionId));
    }

    // ========== Dashboard ==========

    @GetMapping("/api/projects/{projectId}/beta-dashboard")
    public ApiResponse<BetaTrialDashboardResponse> getDashboard(@PathVariable String projectId) {
        return ApiResponse.ok(betaEnvironmentReadinessService.getDashboard(projectId));
    }
}
