package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GovernanceOperatorLearningController {

    private final GovernanceOperatorMemoryService memoryService;
    private final GovernanceSessionLearningService learningService;
    private final GovernanceRemediationReuseService reuseService;

    public GovernanceOperatorLearningController(GovernanceOperatorMemoryService memoryService,
                                                 GovernanceSessionLearningService learningService,
                                                 GovernanceRemediationReuseService reuseService) {
        this.memoryService = memoryService;
        this.learningService = learningService;
        this.reuseService = reuseService;
    }

    // ========== Action Memory ==========
    @PostMapping("/api/governance-operator-memory/actions")
    public ApiResponse<GovernanceOperatorActionMemoryResponse> recordAction(@RequestParam String sessionId,
                                                                              @RequestParam String actionType,
                                                                              @RequestParam String actionTargetType,
                                                                              @RequestParam(required = false) String operatorName,
                                                                              @RequestParam(required = false) Boolean acceptedFlag,
                                                                              @RequestParam(required = false) Boolean successFlag,
                                                                              @RequestParam(required = false) Integer durationSeconds,
                                                                              @RequestParam(required = false) String noteText) {
        return ApiResponse.ok(memoryService.recordAction(sessionId, actionType, actionTargetType,
                operatorName, acceptedFlag, successFlag, durationSeconds, noteText));
    }

    @GetMapping("/api/governance-operator-memory/actions")
    public ApiResponse<List<GovernanceOperatorActionMemoryResponse>> listActions(@RequestParam(required = false) String sessionId) {
        if (sessionId != null) return ApiResponse.ok(memoryService.listActions(sessionId));
        return ApiResponse.ok(memoryService.listAllActions());
    }

    // ========== Insight ==========
    @PostMapping("/api/governance-operator-memory/insights/refresh")
    public ApiResponse<String> refreshInsight(@RequestParam String sessionId) {
        learningService.refreshInsight(sessionId);
        return ApiResponse.ok("Insight refreshed for session " + sessionId);
    }

    @GetMapping("/api/governance-operator-memory/insights")
    public ApiResponse<List<GovernanceWorkspaceSessionInsightResponse>> listInsights() {
        return ApiResponse.ok(learningService.listInsights());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-operator-memory/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.ok(learningService.getDashboard());
    }

    @GetMapping("/api/governance-operator-memory/report")
    public ApiResponse<String> getReport() {
        return ApiResponse.ok(learningService.getReport());
    }

    // ========== Reuse Bundles ==========
    @PostMapping("/api/governance-operator-memory/reuse-bundles")
    public ApiResponse<GovernanceRemediationReuseBundleResponse> createBundle(@RequestParam String bundleKey,
                                                                               @RequestParam String title,
                                                                               @RequestParam String category,
                                                                               @RequestParam(required = false) String guardrailKey,
                                                                               @RequestParam(required = false) String priority,
                                                                               @RequestParam(required = false) String actionSequenceJson) {
        return ApiResponse.ok(reuseService.createBundle(bundleKey, title, category, guardrailKey, priority, actionSequenceJson));
    }

    @GetMapping("/api/governance-operator-memory/reuse-bundles")
    public ApiResponse<List<GovernanceRemediationReuseBundleResponse>> listBundles() {
        return ApiResponse.ok(reuseService.listBundles());
    }

    @GetMapping("/api/governance-operator-memory/reuse-bundles/{bundleId}")
    public ApiResponse<GovernanceRemediationReuseBundleResponse> getBundle(@PathVariable String bundleId) {
        return ApiResponse.ok(reuseService.getBundle(bundleId));
    }

    @PutMapping("/api/governance-operator-memory/reuse-bundles/{bundleId}")
    public ApiResponse<GovernanceRemediationReuseBundleResponse> updateBundle(@PathVariable String bundleId,
                                                                               @RequestParam(required = false) String title,
                                                                               @RequestParam(required = false) String actionSequenceJson) {
        return ApiResponse.ok(reuseService.updateBundle(bundleId, title, actionSequenceJson));
    }

    @PostMapping("/api/governance-operator-memory/reuse-bundles/{bundleId}/status")
    public ApiResponse<GovernanceRemediationReuseBundleResponse> updateBundleStatus(@PathVariable String bundleId,
                                                                                     @RequestParam Boolean enabled) {
        return ApiResponse.ok(reuseService.updateBundleStatus(bundleId, enabled));
    }

    @PostMapping("/api/governance-operator-memory/reuse-bundles/refresh")
    public ApiResponse<String> refreshBundles() {
        reuseService.refreshBundles();
        return ApiResponse.ok("Reuse bundles refreshed");
    }
}
