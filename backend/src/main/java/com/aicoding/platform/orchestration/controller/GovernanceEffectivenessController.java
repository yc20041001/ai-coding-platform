package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GovernanceEffectivenessController {

    private final GovernanceEffectivenessAnalyticsService effectivenessAnalyticsService;
    private final GovernancePlaybookPerformanceService playbookPerformanceService;
    private final GovernanceRecipeOptimizationService recipeOptimizationService;

    public GovernanceEffectivenessController(GovernanceEffectivenessAnalyticsService effectivenessAnalyticsService,
                                              GovernancePlaybookPerformanceService playbookPerformanceService,
                                              GovernanceRecipeOptimizationService recipeOptimizationService) {
        this.effectivenessAnalyticsService = effectivenessAnalyticsService;
        this.playbookPerformanceService = playbookPerformanceService;
        this.recipeOptimizationService = recipeOptimizationService;
    }

    // ========== Recipe Effectiveness ==========
    @PostMapping("/api/governance-effectiveness/recipes/refresh")
    public ApiResponse<String> refreshRecipeEffectiveness() {
        effectivenessAnalyticsService.refreshEffectiveness();
        return ApiResponse.ok("Recipe effectiveness refreshed");
    }

    @GetMapping("/api/governance-effectiveness/recipes")
    public ApiResponse<List<GovernanceRecipeEffectivenessSnapshotResponse>> getRecipeEffectiveness(
            @RequestParam(required = false) String level) {
        if (level != null) return ApiResponse.ok(effectivenessAnalyticsService.getEffectivenessListByLevel(level));
        return ApiResponse.ok(effectivenessAnalyticsService.getEffectivenessList());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-effectiveness/recipes/dashboard")
    public ApiResponse<Map<String, Object>> getRecipeEffectivenessDashboard() {
        return ApiResponse.ok(effectivenessAnalyticsService.getDashboard());
    }

    @GetMapping("/api/governance-effectiveness/recipes/trend")
    public ApiResponse<List<GovernanceRecipeEffectivenessSnapshotResponse>> getRecipeTrend(
            @RequestParam(defaultValue = "LAST_7_DAYS") String window) {
        return ApiResponse.ok(effectivenessAnalyticsService.getEffectivenessList());
    }

    // ========== Playbook Analytics ==========
    @PostMapping("/api/governance-effectiveness/playbooks/refresh")
    public ApiResponse<String> refreshPlaybookAnalytics() {
        playbookPerformanceService.refreshAnalytics();
        return ApiResponse.ok("Playbook analytics refreshed");
    }

    @GetMapping("/api/governance-effectiveness/playbooks")
    public ApiResponse<List<GovernancePlaybookAnalyticsRecordResponse>> getPlaybookAnalytics() {
        return ApiResponse.ok(playbookPerformanceService.getAnalyticsList());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-effectiveness/playbooks/dashboard")
    public ApiResponse<Map<String, Object>> getPlaybookDashboard() {
        return ApiResponse.ok(playbookPerformanceService.getDashboard());
    }

    // ========== Optimization Suggestions ==========
    @PostMapping("/api/governance-effectiveness/optimizations/refresh")
    public ApiResponse<String> refreshOptimizations() {
        recipeOptimizationService.refreshSuggestions();
        return ApiResponse.ok("Optimization suggestions refreshed");
    }

    @GetMapping("/api/governance-effectiveness/optimizations")
    public ApiResponse<List<GovernanceOptimizationSuggestionResponse>> getOptimizations() {
        return ApiResponse.ok(recipeOptimizationService.listSuggestions());
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/governance-effectiveness/optimizations/dashboard")
    public ApiResponse<Map<String, Object>> getOptimizationDashboard() {
        return ApiResponse.ok(recipeOptimizationService.getDashboard());
    }

    @GetMapping("/api/governance-effectiveness/report")
    public ApiResponse<String> getReport() {
        return ApiResponse.ok(recipeOptimizationService.getReport());
    }
}
