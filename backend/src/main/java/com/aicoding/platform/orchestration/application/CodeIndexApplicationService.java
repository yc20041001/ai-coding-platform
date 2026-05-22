package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.dto.CodeIndexFileResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSummaryResponse;
import com.aicoding.platform.orchestration.dto.CodeIndexSymbolResponse;
import com.aicoding.platform.orchestration.dto.CodeSearchRequest;
import com.aicoding.platform.orchestration.dto.CodeSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CodeIndexApplicationService {

    private final CodeIndexBuildService codeIndexBuildService;
    private final CodeSearchService codeSearchService;

    public CodeIndexApplicationService(CodeIndexBuildService codeIndexBuildService,
                                        CodeSearchService codeSearchService) {
        this.codeIndexBuildService = codeIndexBuildService;
        this.codeSearchService = codeSearchService;
    }

    @Transactional
    public CodeIndexSummaryResponse buildIndex(Long projectId, String branch, String pathPrefix, Integer maxFiles) {
        return codeIndexBuildService.buildIndex(projectId, branch, pathPrefix, maxFiles);
    }

    public CodeIndexSummaryResponse getSummary(Long projectId) {
        return codeIndexBuildService.getSummary(projectId);
    }

    public List<CodeIndexFileResponse> listFiles(Long projectId, String branch, String pathPrefix, int limit) {
        return codeIndexBuildService.listFiles(projectId, branch, pathPrefix, limit);
    }

    public List<CodeIndexSymbolResponse> listSymbols(Long projectId, String branch, String symbolType, int limit) {
        return codeIndexBuildService.listSymbols(projectId, branch, symbolType, limit);
    }

    public CodeSearchResponse search(Long projectId, CodeSearchRequest request) {
        return codeSearchService.search(projectId, request);
    }
}
