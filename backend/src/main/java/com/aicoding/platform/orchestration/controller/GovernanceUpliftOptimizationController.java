package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernanceUpliftOptimizationController {

    private final GovernanceBenchmarkEvolutionService evolutionService;
    private final GovernanceCampaignRankingService rankingService;
    private final GovernanceProgressMapService progressService;

    public GovernanceUpliftOptimizationController(GovernanceBenchmarkEvolutionService evolutionService,
                                                   GovernanceCampaignRankingService rankingService,
                                                   GovernanceProgressMapService progressService) {
        this.evolutionService = evolutionService;
        this.rankingService = rankingService;
        this.progressService = progressService;
    }

    @PostMapping("/api/governance-uplift-optimization/evolution/refresh")
    public ApiResponse<String> refreshEvolution() { evolutionService.refreshEvolution(); return ApiResponse.ok("Evolution refreshed"); }
    @GetMapping("/api/governance-uplift-optimization/evolution")
    public ApiResponse<List<GovernanceBenchmarkEvolutionSnapshotResponse>> listEvolution() { return ApiResponse.ok(evolutionService.listEvolution()); }
    @PostMapping("/api/governance-uplift-optimization/campaign-ranking/refresh")
    public ApiResponse<String> refreshRanking() { rankingService.refreshRanking(); return ApiResponse.ok("Ranking refreshed"); }
    @GetMapping("/api/governance-uplift-optimization/campaign-ranking")
    public ApiResponse<List<GovernanceCampaignEffectivenessRankingResponse>> listRanking() { return ApiResponse.ok(rankingService.listRanking()); }
    @PostMapping("/api/governance-uplift-optimization/progress-map/refresh")
    public ApiResponse<String> refreshProgress() { progressService.refreshProgress(); return ApiResponse.ok("Progress refreshed"); }
    @GetMapping("/api/governance-uplift-optimization/progress-map")
    public ApiResponse<List<GovernanceProgressMapSnapshotResponse>> listProgress() { return ApiResponse.ok(progressService.listProgress()); }

    @GetMapping("/api/governance-uplift-optimization/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("evolutionCount", evolutionService.listEvolution().size());
        resp.put("rankingCount", rankingService.listRanking().size());
        resp.put("progressCount", progressService.listProgress().size());
        resp.put("evolution", evolutionService.listEvolution());
        resp.put("ranking", rankingService.listRanking());
        resp.put("progress", progressService.listProgress());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-uplift-optimization/report")
    public ApiResponse<String> getReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Uplift Optimization Report\n\n");
        md.append("- Evolution: ").append(evolutionService.listEvolution().size()).append("\n");
        md.append("- Rankings: ").append(rankingService.listRanking().size()).append("\n");
        md.append("- Progress: ").append(progressService.listProgress().size()).append("\n");
        return ApiResponse.ok(md.toString());
    }
}
