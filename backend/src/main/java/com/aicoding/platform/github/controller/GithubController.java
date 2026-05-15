package com.aicoding.platform.github.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.github.application.GithubOAuthService;
import com.aicoding.platform.github.application.GithubPullRequestService;
import com.aicoding.platform.github.application.GithubRepositoryService;
import com.aicoding.platform.github.application.PrReviewApplicationService;
import com.aicoding.platform.github.dto.CreatePrReviewRequest;
import com.aicoding.platform.github.dto.GithubOAuthAuthorizeResponse;
import com.aicoding.platform.github.dto.GithubOAuthStatusResponse;
import com.aicoding.platform.github.dto.GithubPullRequestFileResponse;
import com.aicoding.platform.github.dto.GithubPullRequestResponse;
import com.aicoding.platform.github.dto.GithubRepositoryResponse;
import com.aicoding.platform.github.dto.PrReviewFindingResponse;
import com.aicoding.platform.github.dto.PrReviewJobResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GithubController {

    private final GithubOAuthService githubOAuthService;
    private final GithubRepositoryService githubRepositoryService;
    private final GithubPullRequestService githubPullRequestService;
    private final PrReviewApplicationService prReviewApplicationService;

    public GithubController(GithubOAuthService githubOAuthService,
                           GithubRepositoryService githubRepositoryService,
                           GithubPullRequestService githubPullRequestService,
                           PrReviewApplicationService prReviewApplicationService) {
        this.githubOAuthService = githubOAuthService;
        this.githubRepositoryService = githubRepositoryService;
        this.githubPullRequestService = githubPullRequestService;
        this.prReviewApplicationService = prReviewApplicationService;
    }

    // === OAuth endpoints ===

    @GetMapping("/api/github/oauth/authorize")
    public ApiResponse<GithubOAuthAuthorizeResponse> authorize() {
        return ApiResponse.ok(githubOAuthService.authorize());
    }

    @GetMapping(value = "/api/github/oauth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String callback(@RequestParam String code, @RequestParam String state) {
        return githubOAuthService.callback(code, state);
    }

    @GetMapping("/api/github/oauth/status")
    public ApiResponse<GithubOAuthStatusResponse> status() {
        return ApiResponse.ok(githubOAuthService.status());
    }

    @DeleteMapping("/api/github/oauth/bindings/{bindingId}")
    public ApiResponse<Void> unbind(@PathVariable Long bindingId) {
        githubOAuthService.unbind(bindingId);
        return ApiResponse.ok();
    }

    // === Repository endpoints ===

    @PostMapping("/api/github/repos/sync")
    public ApiResponse<List<GithubRepositoryResponse>> syncRepositories() {
        return ApiResponse.ok(githubRepositoryService.sync());
    }

    @GetMapping("/api/github/repos")
    public ApiResponse<List<GithubRepositoryResponse>> listRepositories() {
        return ApiResponse.ok(githubRepositoryService.list());
    }

    // === Pull Request endpoints ===

    @GetMapping("/api/github/repos/{owner}/{repo}/pull-requests")
    public ApiResponse<List<GithubPullRequestResponse>> listPullRequests(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "open") String state) {
        return ApiResponse.ok(githubPullRequestService.listPullRequests(owner, repo, state));
    }

    @GetMapping("/api/github/repos/{owner}/{repo}/pull-requests/{number}")
    public ApiResponse<GithubPullRequestResponse> getPullRequest(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int number) {
        return ApiResponse.ok(githubPullRequestService.getDetail(owner, repo, number));
    }

    @GetMapping("/api/github/repos/{owner}/{repo}/pull-requests/{number}/files")
    public ApiResponse<List<GithubPullRequestFileResponse>> getPullRequestFiles(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int number) {
        return ApiResponse.ok(githubPullRequestService.getFiles(owner, repo, number));
    }

    @GetMapping("/api/github/repos/{owner}/{repo}/pull-requests/{number}/patch")
    public ApiResponse<String> getPullRequestPatch(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int number) {
        return ApiResponse.ok(githubPullRequestService.getPatch(owner, repo, number));
    }

    // === PR Review endpoints ===

    @PostMapping("/api/projects/{projectId}/github/pr-reviews")
    public ApiResponse<PrReviewJobResponse> createReview(
            @PathVariable Long projectId,
            @Valid @RequestBody CreatePrReviewRequest request) {
        return ApiResponse.ok(prReviewApplicationService.create(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/github/pr-reviews")
    public ApiResponse<List<PrReviewJobResponse>> listReviews(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(prReviewApplicationService.listByProject(projectId, page, pageSize));
    }

    @GetMapping("/api/github/pr-reviews/{reviewJobId}")
    public ApiResponse<PrReviewJobResponse> getReviewDetail(@PathVariable Long reviewJobId) {
        return ApiResponse.ok(prReviewApplicationService.getDetail(reviewJobId));
    }

    @GetMapping("/api/github/pr-reviews/{reviewJobId}/findings")
    public ApiResponse<List<PrReviewFindingResponse>> getReviewFindings(@PathVariable Long reviewJobId) {
        return ApiResponse.ok(prReviewApplicationService.getFindings(reviewJobId));
    }
}
