package com.aicoding.platform.repository.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.repository.application.RepositoryApplicationService;
import com.aicoding.platform.repository.dto.GithubRepositoryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GithubRepositoryController {

    private final RepositoryApplicationService repositoryApplicationService;

    public GithubRepositoryController(RepositoryApplicationService repositoryApplicationService) {
        this.repositoryApplicationService = repositoryApplicationService;
    }

    @GetMapping("/repositories")
    public ApiResponse<PageResult<GithubRepositoryResponse>> listRepositories(
            @Valid PageQuery pageQuery,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(repositoryApplicationService.listGithubRepositories(pageQuery, keyword));
    }
}
