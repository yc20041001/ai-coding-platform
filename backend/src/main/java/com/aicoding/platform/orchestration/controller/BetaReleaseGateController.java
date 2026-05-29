package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.BetaReleaseDecisionService;
import com.aicoding.platform.orchestration.application.BetaReleaseGateEvaluationService;
import com.aicoding.platform.orchestration.application.BetaReleaseGateRuleService;
import com.aicoding.platform.orchestration.dto.BetaReleaseDecisionResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateDashboardResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateEvaluationResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateRuleResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseReadinessReportResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaReleaseDecisionRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaReleaseDecisionRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class BetaReleaseGateController {

    private final BetaReleaseGateRuleService betaReleaseGateRuleService;
    private final BetaReleaseGateEvaluationService betaReleaseGateEvaluationService;
    private final BetaReleaseDecisionService betaReleaseDecisionService;

    public BetaReleaseGateController(BetaReleaseGateRuleService betaReleaseGateRuleService,
                                     BetaReleaseGateEvaluationService betaReleaseGateEvaluationService,
                                     BetaReleaseDecisionService betaReleaseDecisionService) {
        this.betaReleaseGateRuleService = betaReleaseGateRuleService;
        this.betaReleaseGateEvaluationService = betaReleaseGateEvaluationService;
        this.betaReleaseDecisionService = betaReleaseDecisionService;
    }

    // ========== Gate Rules ==========

    @GetMapping("/api/projects/{projectId}/beta/release-gate/rules")
    public ApiResponse<List<BetaReleaseGateRuleResponse>> listGateRules(@PathVariable String projectId) {
        return ApiResponse.ok(betaReleaseGateRuleService.listRules(projectId));
    }

    @PutMapping("/api/projects/{projectId}/beta/release-gate/rules/{ruleId}")
    public ApiResponse<BetaReleaseGateRuleResponse> updateGateRule(
            @PathVariable String projectId,
            @PathVariable String ruleId,
            @RequestParam(required = false) String enabled,
            @RequestParam(required = false) String blocking,
            @RequestParam(required = false) BigDecimal thresholdValue) {
        return ApiResponse.ok(betaReleaseGateRuleService.updateRule(ruleId, enabled, blocking, thresholdValue));
    }

    // ========== Gate Evaluations ==========

    @PostMapping("/api/projects/{projectId}/beta/release-gate/evaluate")
    public ApiResponse<List<BetaReleaseGateEvaluationResponse>> evaluateReleaseGate(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "MANUAL") String evaluationType,
            @RequestParam(required = false) String evaluationTarget) {
        return ApiResponse.ok(betaReleaseGateEvaluationService.evaluate(projectId, evaluationType, evaluationTarget));
    }

    @GetMapping("/api/projects/{projectId}/beta/release-gate/evaluations")
    public ApiResponse<List<BetaReleaseGateEvaluationResponse>> listGateEvaluations(
            @PathVariable String projectId,
            @RequestParam(required = false) String evaluationTarget,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(betaReleaseGateEvaluationService.listEvaluations(projectId, evaluationTarget, page, size));
    }

    // ========== Gate Dashboard ==========

    @GetMapping("/api/projects/{projectId}/beta/release-gate/dashboard")
    public ApiResponse<BetaReleaseGateDashboardResponse> getGateDashboard(@PathVariable String projectId) {
        return ApiResponse.ok(betaReleaseGateEvaluationService.getGateDashboard(projectId));
    }

    // ========== Release Decisions ==========

    @PostMapping("/api/projects/{projectId}/beta/release-gate/decisions")
    public ApiResponse<BetaReleaseDecisionResponse> createReleaseDecision(
            @PathVariable String projectId,
            @RequestBody CreateBetaReleaseDecisionRequest request) {
        return ApiResponse.ok(betaReleaseDecisionService.createDecision(projectId, request));
    }

    @PutMapping("/api/projects/{projectId}/beta/release-gate/decisions/{decisionId}")
    public ApiResponse<BetaReleaseDecisionResponse> updateReleaseDecision(
            @PathVariable String projectId,
            @PathVariable String decisionId,
            @RequestBody UpdateBetaReleaseDecisionRequest request) {
        return ApiResponse.ok(betaReleaseDecisionService.updateDecision(decisionId, request));
    }

    @GetMapping("/api/projects/{projectId}/beta/release-gate/decisions")
    public ApiResponse<List<BetaReleaseDecisionResponse>> listReleaseDecisions(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(betaReleaseDecisionService.listDecisions(projectId, page, size));
    }

    @GetMapping("/api/projects/{projectId}/beta/release-gate/decisions/{decisionId}")
    public ApiResponse<BetaReleaseDecisionResponse> getReleaseDecision(
            @PathVariable String projectId,
            @PathVariable String decisionId) {
        return ApiResponse.ok(betaReleaseDecisionService.getDecision(decisionId));
    }

    // ========== Readiness Report ==========

    @GetMapping("/api/projects/{projectId}/beta/release-gate/readiness-report")
    public ApiResponse<BetaReleaseReadinessReportResponse> getReadinessReport(
            @PathVariable String projectId,
            @RequestParam(required = false) String releaseLabel) {
        return ApiResponse.ok(betaReleaseDecisionService.generateReadinessReport(projectId, releaseLabel));
    }
}
