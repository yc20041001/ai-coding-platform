package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernanceBenchmarkAdoptionController {

    private final GovernanceBenchmarkAdoptionService adoptionService;
    private final GovernanceImprovementCampaignService campaignService;
    private final GovernanceUpliftMeasurementService upliftService;

    public GovernanceBenchmarkAdoptionController(GovernanceBenchmarkAdoptionService adoptionService,
                                                  GovernanceImprovementCampaignService campaignService,
                                                  GovernanceUpliftMeasurementService upliftService) {
        this.adoptionService = adoptionService;
        this.campaignService = campaignService;
        this.upliftService = upliftService;
    }

    @PostMapping("/api/governance-benchmark-adoption/records")
    public ApiResponse<GovernanceBenchmarkAdoptionRecordResponse> createRecord(@RequestParam String projectId,
                                                                                @RequestParam String projectName,
                                                                                @RequestParam String metricKey,
                                                                                @RequestParam(required = false) String adoptionStatus,
                                                                                @RequestParam(required = false) String blockerType,
                                                                                @RequestParam(required = false) String blockerNote) {
        return ApiResponse.ok(adoptionService.createRecord(projectId, projectName, metricKey, adoptionStatus, blockerType, blockerNote));
    }

    @GetMapping("/api/governance-benchmark-adoption/records")
    public ApiResponse<List<GovernanceBenchmarkAdoptionRecordResponse>> listRecords() { return ApiResponse.ok(adoptionService.listRecords()); }

    @PostMapping("/api/governance-benchmark-adoption/records/{recordId}/status")
    public ApiResponse<GovernanceBenchmarkAdoptionRecordResponse> updateRecordStatus(@PathVariable String recordId,
                                                                                      @RequestParam String status) {
        return ApiResponse.ok(adoptionService.updateRecordStatus(recordId, status));
    }

    @PostMapping("/api/governance-benchmark-adoption/campaigns")
    public ApiResponse<GovernanceCrossTeamImprovementCampaignResponse> createCampaign(@RequestParam String campaignKey,
                                                                                       @RequestParam String campaignName,
                                                                                       @RequestParam(required = false) String improvementWindow) {
        return ApiResponse.ok(campaignService.createCampaign(campaignKey, campaignName, improvementWindow));
    }

    @GetMapping("/api/governance-benchmark-adoption/campaigns")
    public ApiResponse<List<GovernanceCrossTeamImprovementCampaignResponse>> listCampaigns() { return ApiResponse.ok(campaignService.listCampaigns()); }

    @PostMapping("/api/governance-benchmark-adoption/campaigns/{campaignId}/status")
    public ApiResponse<GovernanceCrossTeamImprovementCampaignResponse> updateCampaignStatus(@PathVariable String campaignId,
                                                                                             @RequestParam String status) {
        return ApiResponse.ok(campaignService.updateCampaignStatus(campaignId, status));
    }

    @PostMapping("/api/governance-benchmark-adoption/uplift/refresh")
    public ApiResponse<String> refreshUplift() { upliftService.refreshUplift(); return ApiResponse.ok("Uplift refreshed"); }

    @GetMapping("/api/governance-benchmark-adoption/uplift")
    public ApiResponse<List<GovernanceUpliftMeasurementSnapshotResponse>> listUplift() { return ApiResponse.ok(upliftService.listUplift()); }

    @GetMapping("/api/governance-benchmark-adoption/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("adoptionCount", adoptionService.listRecords().size());
        resp.put("campaignCount", campaignService.listCampaigns().size());
        resp.put("upliftCount", upliftService.listUplift().size());
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-benchmark-adoption/report")
    public ApiResponse<String> getReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Benchmark Adoption Report\n\n");
        md.append("- Adoption Records: ").append(adoptionService.listRecords().size()).append("\n");
        md.append("- Campaigns: ").append(campaignService.listCampaigns().size()).append("\n");
        md.append("- Uplift Measurements: ").append(upliftService.listUplift().size()).append("\n");
        return ApiResponse.ok(md.toString());
    }
}
