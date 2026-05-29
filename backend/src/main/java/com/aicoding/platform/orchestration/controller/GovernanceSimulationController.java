package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GovernanceSimulationController {

    private final GovernanceSimulationService governanceSimulationService;
    private final GovernanceWhatIfPlannerService governanceWhatIfPlannerService;
    private final PolicyTuningSuggestionService policyTuningSuggestionService;

    public GovernanceSimulationController(GovernanceSimulationService governanceSimulationService,
                                           GovernanceWhatIfPlannerService governanceWhatIfPlannerService,
                                           PolicyTuningSuggestionService policyTuningSuggestionService) {
        this.governanceSimulationService = governanceSimulationService;
        this.governanceWhatIfPlannerService = governanceWhatIfPlannerService;
        this.policyTuningSuggestionService = policyTuningSuggestionService;
    }

    // ========== Scenario CRUD ==========
    @PostMapping("/api/governance-simulation/scenarios")
    public ApiResponse<GovernanceSimulationScenarioResponse> createScenario(@RequestBody CreateGovernanceSimulationScenarioRequest req) {
        return ApiResponse.ok(governanceSimulationService.createScenario(req));
    }

    @GetMapping("/api/governance-simulation/scenarios")
    public ApiResponse<List<GovernanceSimulationScenarioResponse>> listScenarios() {
        return ApiResponse.ok(governanceSimulationService.listScenarios());
    }

    @GetMapping("/api/governance-simulation/scenarios/{scenarioId}")
    public ApiResponse<GovernanceSimulationScenarioResponse> getScenario(@PathVariable String scenarioId) {
        return ApiResponse.ok(governanceSimulationService.getScenario(scenarioId));
    }

    @PutMapping("/api/governance-simulation/scenarios/{scenarioId}")
    public ApiResponse<GovernanceSimulationScenarioResponse> updateScenario(@PathVariable String scenarioId,
                                                                             @RequestBody UpdateGovernanceSimulationScenarioRequest req) {
        return ApiResponse.ok(governanceSimulationService.updateScenario(scenarioId, req));
    }

    @PostMapping("/api/governance-simulation/scenarios/{scenarioId}/status")
    public ApiResponse<GovernanceSimulationScenarioResponse> updateScenarioStatus(@PathVariable String scenarioId,
                                                                                   @RequestParam String status) {
        return ApiResponse.ok(governanceSimulationService.updateScenarioStatus(scenarioId, status));
    }

    @PostMapping("/api/governance-simulation/scenarios/{scenarioId}/run")
    public ApiResponse<GovernanceSimulationResultResponse> runScenario(@PathVariable String scenarioId) {
        return ApiResponse.ok(governanceSimulationService.runScenario(scenarioId));
    }

    // ========== Result / Comparison ==========
    @GetMapping("/api/governance-simulation/scenarios/{scenarioId}/result")
    public ApiResponse<GovernanceSimulationResultResponse> getResult(@PathVariable String scenarioId) {
        return ApiResponse.ok(governanceSimulationService.getResult(scenarioId));
    }

    @GetMapping("/api/governance-simulation/scenarios/{scenarioId}/comparison")
    public ApiResponse<GovernanceSimulationComparisonResponse> getComparison(@PathVariable String scenarioId) {
        return ApiResponse.ok(governanceSimulationService.getComparison(scenarioId));
    }

    @GetMapping("/api/governance-simulation/report")
    public ApiResponse<String> getReport() {
        return ApiResponse.ok(governanceSimulationService.getReport());
    }

    // ========== Suggestions ==========
    @PostMapping("/api/governance-simulation/suggestions/refresh")
    public ApiResponse<String> refreshSuggestions() {
        policyTuningSuggestionService.refreshSuggestions();
        return ApiResponse.ok("Suggestions refreshed");
    }

    @GetMapping("/api/governance-simulation/suggestions")
    public ApiResponse<List<PolicyTuningSuggestionResponse>> listSuggestions() {
        return ApiResponse.ok(policyTuningSuggestionService.listSuggestions());
    }

    // ========== Dashboard ==========
    @GetMapping("/api/governance-simulation/dashboard")
    public ApiResponse<GovernanceSimulationDashboardResponse> getDashboard() {
        return ApiResponse.ok(governanceSimulationService.getDashboard());
    }
}
