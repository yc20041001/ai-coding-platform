package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.IncidentRetrospectiveService;
import com.aicoding.platform.orchestration.application.KnowledgeQualityReviewService;
import com.aicoding.platform.orchestration.dto.IncidentRetrospectiveResponse;
import com.aicoding.platform.orchestration.dto.IncidentRetrospectiveSummaryResponse;
import com.aicoding.platform.orchestration.dto.KnowledgeQualityReviewResponse;
import com.aicoding.platform.orchestration.dto.KnowledgeQualityStatusSummaryResponse;
import com.aicoding.platform.orchestration.dto.CreateKnowledgeQualityReviewRequest;
import com.aicoding.platform.orchestration.dto.SimilarIncidentRegressionCheckResponse;
import com.aicoding.platform.orchestration.dto.UpdateIncidentRetrospectiveRequest;
import com.aicoding.platform.orchestration.dto.UpdateKnowledgeQualityReviewRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IncidentRetrospectiveController {

    private final IncidentRetrospectiveService retrospectiveService;
    private final KnowledgeQualityReviewService qualityReviewService;

    public IncidentRetrospectiveController(IncidentRetrospectiveService retrospectiveService,
                                            KnowledgeQualityReviewService qualityReviewService) {
        this.retrospectiveService = retrospectiveService;
        this.qualityReviewService = qualityReviewService;
    }

    // --- Retrospective Endpoints (6) ---

    @PostMapping("/api/orchestration/incidents/{incidentId}/retrospective-draft")
    public ApiResponse<IncidentRetrospectiveResponse> createRetrospectiveDraft(
            @PathVariable Long incidentId) {
        return ApiResponse.ok(retrospectiveService.createDraft(incidentId));
    }

    @PutMapping("/api/orchestration/incident-retrospectives/{retrospectiveId}")
    public ApiResponse<IncidentRetrospectiveResponse> updateRetrospective(
            @PathVariable Long retrospectiveId,
            @RequestBody UpdateIncidentRetrospectiveRequest request) {
        return ApiResponse.ok(retrospectiveService.updateRetrospective(retrospectiveId, request));
    }

    @GetMapping("/api/orchestration/incident-retrospectives/{retrospectiveId}")
    public ApiResponse<IncidentRetrospectiveResponse> getRetrospective(
            @PathVariable Long retrospectiveId) {
        return ApiResponse.ok(retrospectiveService.getRetrospective(retrospectiveId));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/retrospective")
    public ApiResponse<IncidentRetrospectiveResponse> getIncidentRetrospective(
            @PathVariable Long incidentId) {
        return ApiResponse.ok(retrospectiveService.getIncidentRetrospective(incidentId));
    }

    @GetMapping("/api/projects/{projectId}/incident-retrospectives")
    public ApiResponse<PageResult<IncidentRetrospectiveSummaryResponse>> listProjectRetrospectives(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(retrospectiveService.listProjectRetrospectives(projectId, status, pageQuery));
    }

    @GetMapping("/api/orchestration/incidents/{incidentId}/regression-check")
    public ApiResponse<SimilarIncidentRegressionCheckResponse> checkRegression(
            @PathVariable Long incidentId) {
        return ApiResponse.ok(retrospectiveService.checkRegression(incidentId));
    }

    // --- Knowledge Quality Review Endpoints (4) ---

    @PostMapping("/api/orchestration/incidents/{incidentId}/knowledge-quality-reviews")
    public ApiResponse<KnowledgeQualityReviewResponse> createKnowledgeQualityReview(
            @PathVariable Long incidentId,
            @RequestBody CreateKnowledgeQualityReviewRequest request) {
        return ApiResponse.ok(qualityReviewService.createReview(incidentId, request));
    }

    @PutMapping("/api/orchestration/knowledge-quality-reviews/{reviewId}")
    public ApiResponse<KnowledgeQualityReviewResponse> updateKnowledgeQualityReview(
            @PathVariable Long reviewId,
            @RequestBody UpdateKnowledgeQualityReviewRequest request) {
        return ApiResponse.ok(qualityReviewService.updateReview(reviewId, request));
    }

    @GetMapping("/api/orchestration/knowledge-quality-reviews/{reviewId}")
    public ApiResponse<KnowledgeQualityReviewResponse> getKnowledgeQualityReview(
            @PathVariable Long reviewId) {
        return ApiResponse.ok(qualityReviewService.getReview(reviewId));
    }

    @GetMapping("/api/projects/{projectId}/knowledge-quality-reviews")
    public ApiResponse<KnowledgeQualityStatusSummaryResponse> listProjectKnowledgeQualityReviews(
            @PathVariable Long projectId) {
        return ApiResponse.ok(qualityReviewService.getProjectStatusSummary(projectId));
    }
}
