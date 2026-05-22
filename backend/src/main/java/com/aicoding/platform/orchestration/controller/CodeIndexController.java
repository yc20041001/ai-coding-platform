package com.aicoding.platform.orchestration.controller;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.application.CodeIndexApplicationService;
import com.aicoding.platform.orchestration.dto.CodeIndexFileResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSummaryResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSymbolResponse;
import com.aicoding.platform.orchestration.dto.CodeSearchRequest;
import com.aicoding.platform.orchestration.dto.CodeSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/code-index")
public class CodeIndexController {

    private final CodeIndexApplicationService codeIndexApplicationService;
    private final ProjectPermissionService projectPermissionService;

    public CodeIndexController(CodeIndexApplicationService codeIndexApplicationService,
                                ProjectPermissionService projectPermissionService) {
        this.codeIndexApplicationService = codeIndexApplicationService;
        this.projectPermissionService = projectPermissionService;
    }

    @PostMapping("/build")
    public ResponseEntity<ApiResponse<CodeIndexSummaryResponse>> buildIndex(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.DEVELOPER, ProjectRole.MAINTAINER, ProjectRole.OWNER);

        String branch = (String) body.getOrDefault("branch", "main");
        String pathPrefix = (String) body.getOrDefault("pathPrefix", "");
        Integer maxFiles = body.get("maxFiles") != null ? ((Number) body.get("maxFiles")).intValue() : 100;

        if (pathPrefix != null && !pathPrefix.isBlank()) {
            validatePathPrefix(pathPrefix);
        }

        CodeIndexSummaryResponse summary = codeIndexApplicationService.buildIndex(projectId, branch, pathPrefix, maxFiles);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CodeIndexSummaryResponse>> getSummary(
            @PathVariable Long projectId) {
        projectPermissionService.checkProjectMember(projectId);
        CodeIndexSummaryResponse summary = codeIndexApplicationService.getSummary(projectId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<CodeIndexFileResponse>>> listFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String pathPrefix,
            @RequestParam(defaultValue = "50") int limit) {
        projectPermissionService.checkProjectMember(projectId);
        List<CodeIndexFileResponse> files = codeIndexApplicationService.listFiles(projectId, branch, pathPrefix, limit);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/symbols")
    public ResponseEntity<ApiResponse<List<CodeIndexSymbolResponse>>> listSymbols(
            @PathVariable Long projectId,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String symbolType,
            @RequestParam(defaultValue = "50") int limit) {
        projectPermissionService.checkProjectMember(projectId);
        List<CodeIndexSymbolResponse> symbols = codeIndexApplicationService.listSymbols(projectId, branch, symbolType, limit);
        return ResponseEntity.ok(ApiResponse.ok(symbols));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<CodeSearchResponse>> search(
            @PathVariable Long projectId,
            @RequestBody CodeSearchRequest request) {
        projectPermissionService.checkProjectMember(projectId);

        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "搜索关键词不能为空");
        }

        if (request.getPathPrefix() != null && !request.getPathPrefix().isBlank()) {
            validatePathPrefix(request.getPathPrefix());
        }

        CodeSearchResponse response = codeIndexApplicationService.search(projectId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private void validatePathPrefix(String pathPrefix) {
        if (pathPrefix.contains("..") || pathPrefix.contains("~") || pathPrefix.contains("\0")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法路径前缀");
        }

        String lower = pathPrefix.toLowerCase();
        if (lower.contains(".env") || lower.contains(".git") || lower.contains("node_modules")
                || lower.contains("target") || lower.contains("dist")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "禁止使用敏感路径前缀");
        }
    }
}
