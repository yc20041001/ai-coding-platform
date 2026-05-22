package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.orchestration.application.ToolOperatorReviewService;
import com.aicoding.platform.orchestration.dto.CreateToolOperatorReviewRequest;
import com.aicoding.platform.orchestration.dto.ToolOperatorReviewResponse;
import com.aicoding.platform.orchestration.dto.UpdateToolOperatorReviewRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ToolOperatorReviewController {

    private final ToolOperatorReviewService reviewService;

    public ToolOperatorReviewController(ToolOperatorReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/orchestration/operator-reviews")
    public ApiResponse<ToolOperatorReviewResponse> createReview(@RequestBody CreateToolOperatorReviewRequest request) {
        return ApiResponse.ok(reviewService.createReview(request));
    }

    @PutMapping("/api/orchestration/operator-reviews/{id}")
    public ApiResponse<ToolOperatorReviewResponse> updateReview(@PathVariable String id,
                                                                 @RequestBody UpdateToolOperatorReviewRequest request) {
        return ApiResponse.ok(reviewService.updateReview(id, request));
    }

    @GetMapping("/api/orchestration/operator-reviews/{id}")
    public ApiResponse<ToolOperatorReviewResponse> getReview(@PathVariable String id) {
        return ApiResponse.ok(reviewService.getReview(id));
    }

    @GetMapping("/api/projects/{projectId}/operator-reviews")
    public ApiResponse<PageResult<ToolOperatorReviewResponse>> listProjectReviews(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createTime,desc") String sort) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        pageQuery.setSort(sort);
        return ApiResponse.ok(reviewService.listProjectReviews(projectId, status, severity, pageQuery));
    }

    @GetMapping("/api/orchestration/operator-reviews/by-target")
    public ApiResponse<List<ToolOperatorReviewResponse>> listTargetReviews(
            @RequestParam String targetType,
            @RequestParam String targetId) {
        return ApiResponse.ok(reviewService.listTargetReviews(targetType, targetId));
    }
}
