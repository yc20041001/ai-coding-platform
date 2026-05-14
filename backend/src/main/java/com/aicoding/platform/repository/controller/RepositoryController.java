package com.aicoding.platform.repository.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.repository.application.RepositoryApplicationService;
import com.aicoding.platform.repository.dto.BindRepositoryRequest;
import com.aicoding.platform.repository.dto.CloneRepositoryRequest;
import com.aicoding.platform.repository.dto.GitOperationResponse;
import com.aicoding.platform.repository.dto.PullRepositoryRequest;
import com.aicoding.platform.repository.dto.RepositoryBranchResponse;
import com.aicoding.platform.repository.dto.RepositoryDiffResponse;
import com.aicoding.platform.repository.dto.RepositoryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/repository")
public class RepositoryController {

    private final RepositoryApplicationService repositoryApplicationService;

    public RepositoryController(RepositoryApplicationService repositoryApplicationService) {
        this.repositoryApplicationService = repositoryApplicationService;
    }

    @PostMapping("/bind")
    public ApiResponse<RepositoryResponse> bind(@PathVariable Long projectId,
                                                 @Valid @RequestBody BindRepositoryRequest request) {
        return ApiResponse.ok(repositoryApplicationService.bindRepository(projectId, request));
    }

    @PostMapping("/clone")
    public ApiResponse<GitOperationResponse> clone(@PathVariable Long projectId,
                                                    @RequestBody CloneRepositoryRequest request) {
        return ApiResponse.ok(repositoryApplicationService.cloneRepository(projectId, request));
    }

    @PostMapping("/pull")
    public ApiResponse<GitOperationResponse> pull(@PathVariable Long projectId,
                                                   @RequestBody PullRepositoryRequest request) {
        return ApiResponse.ok(repositoryApplicationService.pullRepository(projectId, request));
    }

    @GetMapping("/branches")
    public ApiResponse<List<RepositoryBranchResponse>> branches(@PathVariable Long projectId) {
        return ApiResponse.ok(repositoryApplicationService.getBranches(projectId));
    }

    @GetMapping("/diff")
    public ApiResponse<RepositoryDiffResponse> diff(@PathVariable Long projectId,
                                                     @RequestParam String base,
                                                     @RequestParam String head) {
        return ApiResponse.ok(repositoryApplicationService.getDiff(projectId, base, head));
    }
}
