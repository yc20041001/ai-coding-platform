package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.GovernanceBaselineTemplateService;
import com.aicoding.platform.orchestration.application.ReleasePortfolioGovernanceService;
import com.aicoding.platform.orchestration.application.ReleaseRiskHeatmapService;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReleaseGovernanceController {

    private final ReleasePortfolioGovernanceService releasePortfolioGovernanceService;
    private final GovernanceBaselineTemplateService governanceBaselineTemplateService;
    private final ReleaseRiskHeatmapService releaseRiskHeatmapService;

    public ReleaseGovernanceController(ReleasePortfolioGovernanceService releasePortfolioGovernanceService,
                                        GovernanceBaselineTemplateService governanceBaselineTemplateService,
                                        ReleaseRiskHeatmapService releaseRiskHeatmapService) {
        this.releasePortfolioGovernanceService = releasePortfolioGovernanceService;
        this.governanceBaselineTemplateService = governanceBaselineTemplateService;
        this.releaseRiskHeatmapService = releaseRiskHeatmapService;
    }

    // ========== Portfolio ==========

    @PostMapping("/api/release-governance/portfolio/refresh")
    public ApiResponse<String> refreshPortfolio() {
        releasePortfolioGovernanceService.refreshPortfolio();
        return ApiResponse.ok("Portfolio snapshot refreshed");
    }

    @GetMapping("/api/release-governance/portfolio/dashboard")
    public ApiResponse<ReleasePortfolioDashboardResponse> getDashboard() {
        return ApiResponse.ok(releasePortfolioGovernanceService.getDashboard());
    }

    @GetMapping("/api/release-governance/portfolio/ranking")
    public ApiResponse<List<ReleasePortfolioRankingResponse>> getRanking() {
        return ApiResponse.ok(releasePortfolioGovernanceService.getRanking());
    }

    @GetMapping("/api/release-governance/summary")
    public ApiResponse<MultiProjectGovernanceSummaryResponse> getSummary() {
        return ApiResponse.ok(releasePortfolioGovernanceService.getSummary());
    }

    // ========== Baseline Template ==========

    @PostMapping("/api/release-governance/baseline-templates")
    public ApiResponse<GovernanceBaselineTemplateResponse> createTemplate(@RequestBody CreateGovernanceBaselineTemplateRequest request) {
        return ApiResponse.ok(governanceBaselineTemplateService.createTemplate(request));
    }

    @GetMapping("/api/release-governance/baseline-templates")
    public ApiResponse<List<GovernanceBaselineTemplateResponse>> listTemplates(@RequestParam(required = false) String scope) {
        return ApiResponse.ok(governanceBaselineTemplateService.listTemplates(scope));
    }

    @GetMapping("/api/release-governance/baseline-templates/{templateId}")
    public ApiResponse<GovernanceBaselineTemplateResponse> getTemplate(@PathVariable String templateId) {
        return ApiResponse.ok(governanceBaselineTemplateService.getTemplate(templateId));
    }

    @PutMapping("/api/release-governance/baseline-templates/{templateId}")
    public ApiResponse<GovernanceBaselineTemplateResponse> updateTemplate(@PathVariable String templateId,
                                                                           @RequestBody UpdateGovernanceBaselineTemplateRequest request) {
        return ApiResponse.ok(governanceBaselineTemplateService.updateTemplate(templateId, request));
    }

    @PostMapping("/api/release-governance/baseline-templates/{templateId}/status")
    public ApiResponse<GovernanceBaselineTemplateResponse> updateTemplateStatus(@PathVariable String templateId,
                                                                                @RequestParam Boolean enabled) {
        return ApiResponse.ok(governanceBaselineTemplateService.updateTemplateStatus(templateId, enabled));
    }

    // ========== Heatmap ==========

    @PostMapping("/api/release-governance/heatmap/refresh")
    public ApiResponse<String> refreshHeatmap() {
        releaseRiskHeatmapService.refreshHeatmap();
        return ApiResponse.ok("Risk heatmap refreshed");
    }

    @GetMapping("/api/release-governance/heatmap")
    public ApiResponse<ReleaseRiskHeatmapResponse> getHeatmap() {
        return ApiResponse.ok(releaseRiskHeatmapService.getHeatmap());
    }
}
