package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.PatchProposalReviewService;
import com.aicoding.platform.orchestration.dto.PatchProposalReviewDecisionRequest;
import com.aicoding.platform.orchestration.dto.PatchProposalReviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatchProposalReviewController {

    private final PatchProposalReviewService patchProposalReviewService;

    public PatchProposalReviewController(PatchProposalReviewService patchProposalReviewService) {
        this.patchProposalReviewService = patchProposalReviewService;
    }

    @GetMapping("/api/task-artifacts/{artifactId}/patch-review")
    public ApiResponse<PatchProposalReviewResponse> getPatchReview(@PathVariable Long artifactId) {
        return ApiResponse.ok(patchProposalReviewService.ensureReviewForArtifact(artifactId));
    }

    @PostMapping("/api/task-artifacts/{artifactId}/patch-review/decision")
    public ApiResponse<PatchProposalReviewResponse> submitDecision(
            @PathVariable Long artifactId,
            @RequestBody PatchProposalReviewDecisionRequest request) {
        return ApiResponse.ok(patchProposalReviewService.decide(artifactId, request));
    }

    @GetMapping("/api/tasks/{taskId}/patch-reviews")
    public ApiResponse<List<PatchProposalReviewResponse>> listTaskReviews(@PathVariable Long taskId) {
        return ApiResponse.ok(patchProposalReviewService.listTaskReviews(taskId));
    }
}
