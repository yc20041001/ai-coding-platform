package com.aicoding.platform.rag.controller;

import com.aicoding.platform.common.response.ApiResponse;
import com.aicoding.platform.rag.application.RagSearchApplicationService;
import com.aicoding.platform.rag.dto.RagSearchRequest;
import com.aicoding.platform.rag.dto.RagSearchResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagSearchController {

    private final RagSearchApplicationService ragSearchApplicationService;

    public RagSearchController(RagSearchApplicationService ragSearchApplicationService) {
        this.ragSearchApplicationService = ragSearchApplicationService;
    }

    @PostMapping("/api/projects/{projectId}/rag/search")
    public ApiResponse<RagSearchResponse> search(
            @PathVariable Long projectId,
            @Valid @RequestBody RagSearchRequest request) {
        return ApiResponse.ok(ragSearchApplicationService.search(projectId, request));
    }
}
