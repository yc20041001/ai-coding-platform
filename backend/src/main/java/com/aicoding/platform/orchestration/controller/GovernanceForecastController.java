package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceForecastController {

    private final GovernanceCapacityForecastService governanceCapacityForecastService;
    private final PredictiveRiskSignalService predictiveRiskSignalService;
    private final GovernanceBacklogHealthService governanceBacklogHealthService;

    public GovernanceForecastController(GovernanceCapacityForecastService governanceCapacityForecastService,
                                         PredictiveRiskSignalService predictiveRiskSignalService,
                                         GovernanceBacklogHealthService governanceBacklogHealthService) {
        this.governanceCapacityForecastService = governanceCapacityForecastService;
        this.predictiveRiskSignalService = predictiveRiskSignalService;
        this.governanceBacklogHealthService = governanceBacklogHealthService;
    }

    // ========== Capacity Forecast ==========
    @PostMapping("/api/governance-forecast/capacity/refresh")
    public ApiResponse<String> refreshCapacity(@RequestParam(defaultValue = "7") int horizonDays) {
        governanceCapacityForecastService.refreshForecast(horizonDays);
        if (horizonDays == 7) governanceCapacityForecastService.refreshForecast(14);
        return ApiResponse.ok("Capacity forecast refreshed");
    }

    @GetMapping("/api/governance-forecast/capacity")
    public ApiResponse<List<GovernanceCapacityForecastResponse>> getCapacityForecasts(@RequestParam(required = false) Integer horizonDays) {
        return ApiResponse.ok(governanceCapacityForecastService.getForecasts(horizonDays));
    }

    @GetMapping("/api/governance-forecast/capacity/dashboard")
    public ApiResponse<GovernanceCapacityDashboardResponse> getCapacityDashboard(@RequestParam(required = false) Integer horizonDays) {
        return ApiResponse.ok(governanceCapacityForecastService.getDashboard(horizonDays));
    }

    // ========== Predictive Risk Signals ==========
    @PostMapping("/api/governance-forecast/risk-signals/refresh")
    public ApiResponse<String> refreshRiskSignals() {
        predictiveRiskSignalService.refreshSignals();
        return ApiResponse.ok("Risk signals refreshed");
    }

    @GetMapping("/api/governance-forecast/risk-signals")
    public ApiResponse<List<PredictiveRiskSignalResponse>> getRiskSignals() {
        return ApiResponse.ok(predictiveRiskSignalService.getSignals());
    }

    @GetMapping("/api/governance-forecast/risk-signals/dashboard")
    public ApiResponse<PredictiveRiskDashboardResponse> getRiskDashboard() {
        return ApiResponse.ok(predictiveRiskSignalService.getDashboard());
    }

    // ========== Backlog Health ==========
    @PostMapping("/api/governance-forecast/backlog/refresh")
    public ApiResponse<String> refreshBacklog() {
        governanceBacklogHealthService.refreshBacklog();
        return ApiResponse.ok("Backlog snapshot refreshed");
    }

    @GetMapping("/api/governance-forecast/backlog")
    public ApiResponse<List<GovernanceBacklogSnapshotResponse>> getBacklogList() {
        return ApiResponse.ok(governanceBacklogHealthService.getBacklogList());
    }

    @GetMapping("/api/governance-forecast/backlog/dashboard")
    public ApiResponse<GovernanceBacklogDashboardResponse> getBacklogDashboard() {
        return ApiResponse.ok(governanceBacklogHealthService.getDashboard());
    }

    // ========== Summary & Report ==========
    @GetMapping("/api/governance-forecast/summary")
    public ApiResponse<GovernanceForecastSummaryResponse> getSummary() {
        return ApiResponse.ok(governanceBacklogHealthService.getSummary());
    }

    @GetMapping("/api/governance-forecast/report")
    public ApiResponse<GovernanceForecastSummaryResponse> getReport() {
        return ApiResponse.ok(governanceBacklogHealthService.getSummary());
    }
}
