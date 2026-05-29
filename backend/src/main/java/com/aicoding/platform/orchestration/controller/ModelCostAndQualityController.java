package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ModelCostAlertService;
import com.aicoding.platform.orchestration.application.ModelCostAnalyticsService;
import com.aicoding.platform.orchestration.application.ModelCostReportExportService;
import com.aicoding.platform.orchestration.application.PrReviewQualityExportService;
import com.aicoding.platform.orchestration.application.PrReviewQualityService;
import com.aicoding.platform.orchestration.dto.CreatePrReviewQualityRecordRequest;
import com.aicoding.platform.orchestration.dto.ExportModelCostReportResponse;
import com.aicoding.platform.orchestration.dto.ExportPrReviewQualityReportResponse;
import com.aicoding.platform.orchestration.dto.ModelCostAlertResponse;
import com.aicoding.platform.orchestration.dto.ModelCostDashboardResponse;
import com.aicoding.platform.orchestration.dto.ModelCostSummaryResponse;
import com.aicoding.platform.orchestration.dto.ModelCostTrendResponse;
import com.aicoding.platform.orchestration.dto.PrReviewQualityDashboardResponse;
import com.aicoding.platform.orchestration.dto.PrReviewQualityRecordResponse;
import com.aicoding.platform.orchestration.dto.UpdatePrReviewQualityRecordRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ModelCostAndQualityController {

    private final ModelCostAnalyticsService modelCostAnalyticsService;
    private final ModelCostAlertService modelCostAlertService;
    private final PrReviewQualityService prReviewQualityService;
    private final ModelCostReportExportService modelCostReportExportService;
    private final PrReviewQualityExportService prReviewQualityExportService;

    public ModelCostAndQualityController(ModelCostAnalyticsService modelCostAnalyticsService,
                                         ModelCostAlertService modelCostAlertService,
                                         PrReviewQualityService prReviewQualityService,
                                         ModelCostReportExportService modelCostReportExportService,
                                         PrReviewQualityExportService prReviewQualityExportService) {
        this.modelCostAnalyticsService = modelCostAnalyticsService;
        this.modelCostAlertService = modelCostAlertService;
        this.prReviewQualityService = prReviewQualityService;
        this.modelCostReportExportService = modelCostReportExportService;
        this.prReviewQualityExportService = prReviewQualityExportService;
    }

    // ========== Model Cost Summaries ==========

    @PostMapping("/api/projects/{projectId}/model-cost/refresh")
    public ApiResponse<Void> refreshModelCost(@PathVariable String projectId) {
        modelCostAnalyticsService.refreshDailySummaries(projectId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/projects/{projectId}/model-cost/summaries")
    public ApiResponse<List<ModelCostSummaryResponse>> listModelCostSummaries(
            @PathVariable String projectId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(modelCostAnalyticsService.listCostSummaries(
                projectId, provider, modelName, startDate, endDate, page, size));
    }

    @GetMapping("/api/projects/{projectId}/model-cost/trend")
    public ApiResponse<List<ModelCostTrendResponse>> getModelCostTrend(
            @PathVariable String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(modelCostAnalyticsService.getCostTrend(projectId, startDate, endDate));
    }

    @GetMapping("/api/projects/{projectId}/model-cost/dashboard")
    public ApiResponse<ModelCostDashboardResponse> getModelCostDashboard(@PathVariable String projectId) {
        return ApiResponse.ok(modelCostAnalyticsService.getCostDashboard(projectId));
    }

    // ========== Model Cost Alerts ==========

    @PostMapping("/api/projects/{projectId}/model-cost/alerts/scan")
    public ApiResponse<List<ModelCostAlertResponse>> scanModelCostAlerts(@PathVariable String projectId) {
        return ApiResponse.ok(modelCostAlertService.scanAlerts(projectId));
    }

    @GetMapping("/api/projects/{projectId}/model-cost/alerts")
    public ApiResponse<List<ModelCostAlertResponse>> listModelCostAlerts(
            @PathVariable String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(modelCostAlertService.listAlerts(projectId, status, severity, page, size));
    }

    @PutMapping("/api/model-cost/alerts/{id}/status")
    public ApiResponse<ModelCostAlertResponse> updateModelCostAlertStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ApiResponse.ok(modelCostAlertService.updateAlertStatus(id, status));
    }

    // ========== PR Review Quality Records ==========

    @PostMapping("/api/projects/{projectId}/pr-review-quality/records")
    public ApiResponse<PrReviewQualityRecordResponse> createPrReviewQualityRecord(
            @PathVariable String projectId,
            @RequestBody CreatePrReviewQualityRecordRequest request) {
        return ApiResponse.ok(prReviewQualityService.createQualityRecord(projectId, request));
    }

    @PutMapping("/api/pr-review-quality/records/{id}")
    public ApiResponse<PrReviewQualityRecordResponse> updatePrReviewQualityRecord(
            @PathVariable String id,
            @RequestBody UpdatePrReviewQualityRecordRequest request) {
        return ApiResponse.ok(prReviewQualityService.updateQualityRecord(id, request));
    }

    @GetMapping("/api/projects/{projectId}/pr-review-quality/records")
    public ApiResponse<List<PrReviewQualityRecordResponse>> listPrReviewQualityRecords(
            @PathVariable String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(prReviewQualityService.listQualityRecords(projectId, status, page, size));
    }

    @GetMapping("/api/projects/{projectId}/pr-review-quality/dashboard")
    public ApiResponse<PrReviewQualityDashboardResponse> getPrReviewQualityDashboard(@PathVariable String projectId) {
        return ApiResponse.ok(prReviewQualityService.getQualityDashboard(projectId));
    }

    // ========== Exports ==========

    @GetMapping("/api/projects/{projectId}/export/model-cost-report")
    public ApiResponse<ExportModelCostReportResponse> exportModelCostReport(
            @PathVariable String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(modelCostReportExportService.exportReport(projectId, startDate, endDate));
    }

    @GetMapping("/api/projects/{projectId}/export/pr-review-quality-report")
    public ApiResponse<ExportPrReviewQualityReportResponse> exportPrReviewQualityReport(@PathVariable String projectId) {
        return ApiResponse.ok(prReviewQualityExportService.exportReport(projectId));
    }
}
