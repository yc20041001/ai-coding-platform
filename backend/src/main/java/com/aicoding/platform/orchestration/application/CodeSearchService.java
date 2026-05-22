package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.CodeIndexChunkEntity;
import com.aicoding.platform.orchestration.domain.CodeIndexFileEntity;
import com.aicoding.platform.orchestration.domain.CodeIndexSymbolEntity;
import com.aicoding.platform.orchestration.dto.CodeSearchRequest;
import com.aicoding.platform.orchestration.dto.CodeSearchResponse;
import com.aicoding.platform.orchestration.dto.CodeSearchResultResponse;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexChunkMapper;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexFileMapper;
import com.aicoding.platform.orchestration.infrastructure.CodeIndexSymbolMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodeSearchService {

    private final CodeIndexFileMapper codeIndexFileMapper;
    private final CodeIndexSymbolMapper codeIndexSymbolMapper;
    private final CodeIndexChunkMapper codeIndexChunkMapper;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    public CodeSearchService(CodeIndexFileMapper codeIndexFileMapper,
                              CodeIndexSymbolMapper codeIndexSymbolMapper,
                              CodeIndexChunkMapper codeIndexChunkMapper) {
        this.codeIndexFileMapper = codeIndexFileMapper;
        this.codeIndexSymbolMapper = codeIndexSymbolMapper;
        this.codeIndexChunkMapper = codeIndexChunkMapper;
    }

    public CodeSearchResponse search(Long projectId, CodeSearchRequest request) {
        String searchType = request.getSearchType() != null ? request.getSearchType() : "ALL";
        int limit = request.getLimit() > 0 ? Math.min(request.getLimit(), MAX_LIMIT) : DEFAULT_LIMIT;
        String keyword = request.getKeyword() != null ? request.getKeyword() : "";
        String branch = request.getBranch();
        String language = request.getLanguage();
        String pathPrefix = request.getPathPrefix();

        List<CodeSearchResultResponse> results = new ArrayList<>();

        switch (searchType.toUpperCase()) {
            case "FILE" -> searchFiles(projectId, keyword, branch, pathPrefix, limit, results);
            case "SYMBOL" -> searchSymbols(projectId, keyword, language, pathPrefix, limit, results);
            case "CHUNK" -> searchChunks(projectId, keyword, pathPrefix, limit, results);
            default -> {
                // ALL: search files first, then symbols, then chunks
                int remaining = limit;
                List<CodeSearchResultResponse> fileResults = new ArrayList<>();
                searchFiles(projectId, keyword, branch, pathPrefix, remaining, fileResults);
                results.addAll(fileResults);
                remaining -= fileResults.size();

                if (remaining > 0) {
                    List<CodeSearchResultResponse> symbolResults = new ArrayList<>();
                    searchSymbols(projectId, keyword, language, pathPrefix, remaining, symbolResults);
                    results.addAll(symbolResults);
                    remaining -= symbolResults.size();
                }

                if (remaining > 0) {
                    List<CodeSearchResultResponse> chunkResults = new ArrayList<>();
                    searchChunks(projectId, keyword, pathPrefix, remaining, chunkResults);
                    results.addAll(chunkResults);
                }
            }
        }

        return new CodeSearchResponse(results, results.size(), keyword, searchType);
    }

    private void searchFiles(Long projectId, String keyword, String branch, String pathPrefix,
                              int limit, List<CodeSearchResultResponse> results) {
        LambdaQueryWrapper<CodeIndexFileEntity> wrapper = new LambdaQueryWrapper<CodeIndexFileEntity>()
                .eq(CodeIndexFileEntity::getProjectId, projectId);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CodeIndexFileEntity::getFilePath, keyword);
        }
        if (branch != null && !branch.isBlank()) {
            wrapper.eq(CodeIndexFileEntity::getBranch, branch);
        }
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            wrapper.like(CodeIndexFileEntity::getFilePath, pathPrefix);
        }

        wrapper.last("LIMIT " + limit);

        for (CodeIndexFileEntity file : codeIndexFileMapper.selectList(wrapper)) {
            CodeSearchResultResponse r = new CodeSearchResultResponse();
            r.setResultType("FILE");
            r.setFilePath(file.getFilePath());
            r.setStartLine(0);
            r.setEndLine(file.getLineCount());
            results.add(r);
        }
    }

    private void searchSymbols(Long projectId, String keyword, String language,
                                String pathPrefix, int limit, List<CodeSearchResultResponse> results) {
        LambdaQueryWrapper<CodeIndexSymbolEntity> wrapper = new LambdaQueryWrapper<CodeIndexSymbolEntity>()
                .eq(CodeIndexSymbolEntity::getProjectId, projectId);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CodeIndexSymbolEntity::getSymbolName, keyword);
        }
        if (language != null && !language.isBlank() && !"ALL".equalsIgnoreCase(language)) {
            wrapper.eq(CodeIndexSymbolEntity::getLanguage, language);
        }
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            wrapper.like(CodeIndexSymbolEntity::getFilePath, pathPrefix);
        }

        wrapper.last("LIMIT " + limit);

        for (CodeIndexSymbolEntity sym : codeIndexSymbolMapper.selectList(wrapper)) {
            CodeSearchResultResponse r = new CodeSearchResultResponse();
            r.setResultType("SYMBOL");
            r.setFilePath(sym.getFilePath());
            r.setSymbolName(sym.getSymbolName());
            r.setSymbolType(sym.getSymbolType());
            r.setStartLine(sym.getStartLine());
            r.setEndLine(sym.getEndLine());
            r.setSnippet(sym.getSnippet());
            results.add(r);
        }
    }

    private void searchChunks(Long projectId, String keyword,
                               String pathPrefix, int limit, List<CodeSearchResultResponse> results) {
        LambdaQueryWrapper<CodeIndexChunkEntity> wrapper = new LambdaQueryWrapper<CodeIndexChunkEntity>()
                .eq(CodeIndexChunkEntity::getProjectId, projectId);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CodeIndexChunkEntity::getContent, keyword);
        }
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            wrapper.like(CodeIndexChunkEntity::getFilePath, pathPrefix);
        }

        wrapper.last("LIMIT " + limit);

        for (CodeIndexChunkEntity chunk : codeIndexChunkMapper.selectList(wrapper)) {
            CodeSearchResultResponse r = new CodeSearchResultResponse();
            r.setResultType("CHUNK");
            r.setFilePath(chunk.getFilePath());
            r.setStartLine(chunk.getStartLine());
            r.setEndLine(chunk.getEndLine());
            r.setSnippet(truncateSnippet(chunk.getContent()));
            results.add(r);
        }
    }

    private String truncateSnippet(String content) {
        if (content == null) return "";
        return content.length() > 300 ? content.substring(0, 300) + "..." : content;
    }
}
