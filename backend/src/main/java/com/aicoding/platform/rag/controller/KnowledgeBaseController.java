package com.aicoding.platform.rag.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.rag.application.KnowledgeBaseApplicationService;
import com.aicoding.platform.rag.dto.CreateKnowledgeBaseRequest;
import com.aicoding.platform.rag.dto.KnowledgeBaseResponse;
import com.aicoding.platform.rag.dto.UpdateKnowledgeBaseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeBaseController {

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;

    public KnowledgeBaseController(KnowledgeBaseApplicationService knowledgeBaseApplicationService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
    }

    @PostMapping("/api/projects/{projectId}/knowledge-bases")
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseApplicationService.createKnowledgeBase(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/knowledge-bases")
    public ApiResponse<PageResult<KnowledgeBaseResponse>> listKnowledgeBases(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(knowledgeBaseApplicationService.listKnowledgeBases(projectId, pageQuery));
    }

    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseResponse> getKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.ok(knowledgeBaseApplicationService.getKnowledgeBase(knowledgeBaseId));
    }

    @PutMapping("/api/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseResponse> updateKnowledgeBase(
            @PathVariable Long knowledgeBaseId,
            @RequestBody UpdateKnowledgeBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseApplicationService.updateKnowledgeBase(knowledgeBaseId, request));
    }

    @DeleteMapping("/api/knowledge-bases/{knowledgeBaseId}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        knowledgeBaseApplicationService.deleteKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok();
    }
}
