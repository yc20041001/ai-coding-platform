package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernancePortfolioBenchmarkController {

    private final GovernancePortfolioBenchmarkService benchmarkService;
    private final GovernanceBestPracticeAlignmentService alignmentService;
    private final GovernanceMaturityScorecardService scorecardService;

    public GovernancePortfolioBenchmarkController(GovernancePortfolioBenchmarkService benchmarkService,
                                                   GovernanceBestPracticeAlignmentService alignmentService,
                                                   GovernanceMaturityScorecardService scorecardService) {
        this.benchmarkService = benchmarkService;
        this.alignmentService = alignmentService;
        this.scorecardService = scorecardService;
    }

    @PostMapping("/api/governance-benchmark/benchmarks/refresh")
    public ApiResponse<String> refreshBenchmarks() { benchmarkService.refreshBenchmarks(); return ApiResponse.ok("Benchmarks refreshed"); }
    @GetMapping("/api/governance-benchmark/benchmarks")
    public ApiResponse<List<GovernancePortfolioBenchmarkSnapshotResponse>> listBenchmarks() { return ApiResponse.ok(benchmarkService.listBenchmarks()); }
    @PostMapping("/api/governance-benchmark/alignments/refresh")
    public ApiResponse<String> refreshAlignments() { alignmentService.refreshAlignments(); return ApiResponse.ok("Alignments refreshed"); }
    @GetMapping("/api/governance-benchmark/alignments")
    public ApiResponse<List<GovernanceBestPracticeAlignmentItemResponse>> listAlignments() { return ApiResponse.ok(alignmentService.listAlignments()); }
    @PostMapping("/api/governance-benchmark/scorecards/refresh")
    public ApiResponse<String> refreshScorecards() { scorecardService.refreshScorecards(); return ApiResponse.ok("Scorecards refreshed"); }
    @GetMapping("/api/governance-benchmark/scorecards")
    public ApiResponse<List<GovernanceMaturityScorecardResponse>> listScorecards() { return ApiResponse.ok(scorecardService.listScorecards()); }

    @GetMapping("/api/governance-benchmark/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("benchmarkCount", benchmarkService.listBenchmarks().size());
        resp.put("alignmentCount", alignmentService.listAlignments().size());
        resp.put("scorecardCount", scorecardService.listScorecards().size());
        resp.put("benchmarks", benchmarkService.listBenchmarks());
        resp.put("alignments", alignmentService.listAlignments());
        resp.put("scorecards", scorecardService.listScorecards());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-benchmark/report")
    public ApiResponse<String> getReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Governance Benchmark Report\n\n");
        md.append("- Benchmarks: ").append(benchmarkService.listBenchmarks().size()).append("\n");
        md.append("- Alignments: ").append(alignmentService.listAlignments().size()).append("\n");
        md.append("- Scorecards: ").append(scorecardService.listScorecards().size()).append("\n");
        return ApiResponse.ok(md.toString());
    }
}
