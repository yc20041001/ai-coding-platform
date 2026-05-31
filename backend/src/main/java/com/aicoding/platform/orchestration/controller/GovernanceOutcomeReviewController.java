package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.*;
import com.aicoding.platform.orchestration.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GovernanceOutcomeReviewController {

    private final GovernanceDraftOutcomeReviewService draftOutcomeReviewService;
    private final GovernanceAssistiveQualityService assistiveQualityService;
    private final GovernancePackageEvaluationService packageEvaluationService;

    public GovernanceOutcomeReviewController(GovernanceDraftOutcomeReviewService draftOutcomeReviewService,
                                              GovernanceAssistiveQualityService assistiveQualityService,
                                              GovernancePackageEvaluationService packageEvaluationService) {
        this.draftOutcomeReviewService = draftOutcomeReviewService;
        this.assistiveQualityService = assistiveQualityService;
        this.packageEvaluationService = packageEvaluationService;
    }

    @PostMapping("/api/governance-outcome-review/draft-adoption")
    public ApiResponse<GovernanceDraftAdoptionReviewResponse> recordDraftReview(@RequestParam String draftPlanId,
                                                                                  @RequestParam String adoptionResult,
                                                                                  @RequestParam(required = false) String modificationLevel,
                                                                                  @RequestParam int usefulnessRating,
                                                                                  @RequestParam(required = false) String reasonCode,
                                                                                  @RequestParam(required = false) String outcomeNoteText) {
        return ApiResponse.ok(draftOutcomeReviewService.recordReview(draftPlanId, adoptionResult, modificationLevel, usefulnessRating, reasonCode, outcomeNoteText));
    }

    @GetMapping("/api/governance-outcome-review/draft-adoption")
    public ApiResponse<List<GovernanceDraftAdoptionReviewResponse>> listDraftReviews() {
        return ApiResponse.ok(draftOutcomeReviewService.listReviews());
    }

    @GetMapping("/api/governance-outcome-review/draft-adoption/{reviewId}")
    public ApiResponse<GovernanceDraftAdoptionReviewResponse> getDraftReview(@PathVariable String reviewId) {
        return ApiResponse.ok(draftOutcomeReviewService.getReview(reviewId));
    }

    @PostMapping("/api/governance-outcome-review/assistive-quality")
    public ApiResponse<GovernanceAssistiveActionQualityReviewResponse> recordAssistiveQuality(@RequestParam String assistiveActionId,
                                                                                                @RequestParam(required = false) String draftPlanId,
                                                                                                @RequestParam String outcomeResult,
                                                                                                @RequestParam int usefulnessRating,
                                                                                                @RequestParam(required = false) String reasonCode,
                                                                                                @RequestParam(required = false) String feedbackText) {
        return ApiResponse.ok(assistiveQualityService.recordReview(assistiveActionId, draftPlanId, outcomeResult, usefulnessRating, reasonCode, feedbackText));
    }

    @GetMapping("/api/governance-outcome-review/assistive-quality")
    public ApiResponse<List<GovernanceAssistiveActionQualityReviewResponse>> listAssistiveQuality() {
        return ApiResponse.ok(assistiveQualityService.listReviews());
    }

    @GetMapping("/api/governance-outcome-review/assistive-quality/{reviewId}")
    public ApiResponse<GovernanceAssistiveActionQualityReviewResponse> getAssistiveQuality(@PathVariable String reviewId) {
        return ApiResponse.ok(assistiveQualityService.getReview(reviewId));
    }

    @PostMapping("/api/governance-outcome-review/package-evaluation")
    public ApiResponse<GovernancePackageReviewEvaluationResponse> recordPackageEvaluation(@RequestParam String packageId,
                                                                                            @RequestParam String evaluationResult,
                                                                                            @RequestParam int completenessScore,
                                                                                            @RequestParam int accuracyScore,
                                                                                            @RequestParam(required = false) String reasonCode,
                                                                                            @RequestParam(required = false) String reviewNotesText) {
        return ApiResponse.ok(packageEvaluationService.recordEvaluation(packageId, evaluationResult, completenessScore, accuracyScore, reasonCode, reviewNotesText));
    }

    @GetMapping("/api/governance-outcome-review/package-evaluation")
    public ApiResponse<List<GovernancePackageReviewEvaluationResponse>> listPackageEvaluations() {
        return ApiResponse.ok(packageEvaluationService.listEvaluations());
    }

    @GetMapping("/api/governance-outcome-review/package-evaluation/{evalId}")
    public ApiResponse<GovernancePackageReviewEvaluationResponse> getPackageEvaluation(@PathVariable String evalId) {
        return ApiResponse.ok(packageEvaluationService.getEvaluation(evalId));
    }

    @GetMapping("/api/governance-outcome-review/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        var drafts = draftOutcomeReviewService.listReviews();
        var actions = assistiveQualityService.listReviews();
        var packages = packageEvaluationService.listEvaluations();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("draftReviewCount", drafts.size());
        resp.put("assistiveReviewCount", actions.size());
        resp.put("packageEvalCount", packages.size());
        resp.put("topDraftReviews", drafts.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        resp.put("topAssistiveReviews", actions.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        resp.put("topPackageEvaluations", packages.stream().limit(5).collect(java.util.stream.Collectors.toList()));
        return ApiResponse.ok(resp);
    }

    @GetMapping("/api/governance-outcome-review/report")
    public ApiResponse<String> getReport() {
        var drafts = draftOutcomeReviewService.listReviews();
        var actions = assistiveQualityService.listReviews();
        var packages = packageEvaluationService.listEvaluations();
        StringBuilder md = new StringBuilder();
        md.append("# Governance Outcome Review Report\n\n");
        md.append("- Draft Adoption Reviews: ").append(drafts.size()).append("\n");
        md.append("- Assistive Quality Reviews: ").append(actions.size()).append("\n");
        md.append("- Package Evaluations: ").append(packages.size()).append("\n\n");
        for (var d : drafts) md.append("- Draft: ").append(d.getAdoptionResult()).append(" (rating: ").append(d.getUsefulnessRating()).append(")\n");
        return ApiResponse.ok(md.toString());
    }
}
