package com.aicoding.platform.rag.controller;

import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.rag.application.KnowledgeDocumentApplicationService;
import com.aicoding.platform.rag.dto.DocumentChunkResponse;
import com.aicoding.platform.rag.dto.KnowledgeDocumentResponse;
import com.aicoding.platform.rag.dto.UploadKnowledgeDocumentRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class KnowledgeDocumentController {

    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    public KnowledgeDocumentController(KnowledgeDocumentApplicationService knowledgeDocumentApplicationService) {
        this.knowledgeDocumentApplicationService = knowledgeDocumentApplicationService;
    }

    @PostMapping("/api/projects/{projectId}/knowledge-documents")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocument(
            @PathVariable Long projectId,
            @Valid @RequestBody UploadKnowledgeDocumentRequest request) {
        return ApiResponse.ok(knowledgeDocumentApplicationService.uploadDocument(projectId, request));
    }

    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<PageResult<KnowledgeDocumentResponse>> listDocuments(
            @PathVariable Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(page);
        pageQuery.setPageSize(pageSize);
        return ApiResponse.ok(knowledgeDocumentApplicationService.listDocuments(knowledgeBaseId, pageQuery));
    }

    @GetMapping("/api/knowledge-documents/{documentId}")
    public ApiResponse<KnowledgeDocumentResponse> getDocument(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeDocumentApplicationService.getDocument(documentId));
    }

    @DeleteMapping("/api/knowledge-documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        knowledgeDocumentApplicationService.deleteDocument(documentId);
        return ApiResponse.ok();
    }

    @GetMapping("/api/knowledge-documents/{documentId}/chunks")
    public ApiResponse<List<DocumentChunkResponse>> listChunks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeDocumentApplicationService.listChunks(documentId));
    }
}
