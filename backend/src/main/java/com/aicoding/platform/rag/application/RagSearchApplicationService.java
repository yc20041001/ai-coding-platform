package com.aicoding.platform.rag.application;

import com.aicoding.platform.chat.dto.ChatMessageReferenceResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.rag.domain.DocumentChunkEntity;
import com.aicoding.platform.rag.domain.KnowledgeBaseEntity;
import com.aicoding.platform.rag.domain.KnowledgeDocumentEntity;
import com.aicoding.platform.rag.dto.RagSearchRequest;
import com.aicoding.platform.rag.dto.RagSearchResponse;
import com.aicoding.platform.rag.dto.RagSearchResultResponse;
import com.aicoding.platform.rag.infrastructure.DocumentChunkMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagSearchApplicationService {

    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ProjectPermissionService projectPermissionService;

    public RagSearchApplicationService(DocumentChunkMapper documentChunkMapper,
                                       KnowledgeDocumentMapper knowledgeDocumentMapper,
                                       KnowledgeBaseMapper knowledgeBaseMapper,
                                       ProjectPermissionService projectPermissionService) {
        this.documentChunkMapper = documentChunkMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public RagSearchResponse search(Long projectId, RagSearchRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        long startMs = System.currentTimeMillis();

        int limit = defaultLimit(request.getLimit());
        boolean includeContent = defaultIncludeContent(request.getIncludeContent());
        String query = request.getQuery();

        // Validate knowledgeBaseId if provided
        if (request.getKnowledgeBaseId() != null && !request.getKnowledgeBaseId().isBlank()) {
            Long kbId = Long.valueOf(request.getKnowledgeBaseId());
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null || !kb.getProjectId().equals(projectId)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "知识库不存在或不属于当前项目");
            }
        }

        // Query chunks with LIKE
        LambdaQueryWrapper<DocumentChunkEntity> wrapper = new LambdaQueryWrapper<DocumentChunkEntity>()
                .eq(DocumentChunkEntity::getProjectId, projectId)
                .like(DocumentChunkEntity::getContent, query);

        if (request.getKnowledgeBaseId() != null && !request.getKnowledgeBaseId().isBlank()) {
            wrapper.eq(DocumentChunkEntity::getKnowledgeBaseId, Long.valueOf(request.getKnowledgeBaseId()));
        }

        // Get all matching chunks, then limit in memory for scoring
        List<DocumentChunkEntity> allChunks = documentChunkMapper.selectList(wrapper);

        // Load documents and KBs for reference info
        Map<Long, KnowledgeDocumentEntity> docCache = new HashMap<>();
        Map<Long, KnowledgeBaseEntity> kbCache = new HashMap<>();

        List<RagSearchResultResponse> results = allChunks.stream()
                .map(chunk -> {
                    KnowledgeDocumentEntity doc = docCache.computeIfAbsent(chunk.getDocumentId(),
                            id -> knowledgeDocumentMapper.selectById(id));
                    KnowledgeBaseEntity kb = kbCache.computeIfAbsent(chunk.getKnowledgeBaseId(),
                            id -> knowledgeBaseMapper.selectById(id));

                    BigDecimal score = calculateScore(chunk.getContent(), query,
                            doc != null ? doc.getTitle() : "");

                    RagSearchResultResponse resp = new RagSearchResultResponse();
                    resp.setChunkId(chunk.getId().toString());
                    resp.setDocumentId(chunk.getDocumentId().toString());
                    resp.setKnowledgeBaseId(kb != null ? kb.getId().toString() : null);
                    resp.setTitle(doc != null ? doc.getTitle() : "");
                    resp.setContent(includeContent ? chunk.getContent() : null);
                    resp.setScore(score);
                    resp.setReferenceType("DOCUMENT");
                    resp.setFilePath(doc != null ? doc.getFilePath() : null);
                    resp.setStartLine(null);
                    resp.setEndLine(null);
                    return resp;
                })
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        if (allChunks.isEmpty() && results.isEmpty()) {
            results = new ArrayList<>();
        }

        long elapsedMs = System.currentTimeMillis() - startMs;

        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setResults(results);
        response.setTotal((long) results.size());
        response.setElapsedMs(elapsedMs);
        return response;
    }

    private BigDecimal calculateScore(String content, String query, String title) {
        if (content == null) {
            return new BigDecimal("0.50");
        }
        String contentLower = content.toLowerCase();
        String queryLower = query.toLowerCase();
        String titleLower = title != null ? title.toLowerCase() : "";

        if (contentLower.contains(queryLower)) {
            return new BigDecimal("0.95");
        }
        if (titleLower.contains(queryLower)) {
            return new BigDecimal("0.85");
        }
        return new BigDecimal("0.50");
    }

    private int defaultLimit(Integer value) {
        if (value == null) {
            return 10;
        }
        return value;
    }

    private boolean defaultIncludeContent(Boolean value) {
        if (value == null) {
            return true;
        }
        return Boolean.TRUE.equals(value);
    }

    public List<ChatMessageReferenceResponse> toChatReferences(List<RagSearchResultResponse> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        return results.stream().map(r -> {
            ChatMessageReferenceResponse ref = new ChatMessageReferenceResponse();
            ref.setId(r.getChunkId());
            ref.setReferenceType(r.getReferenceType());
            ref.setReferenceId(r.getChunkId());
            ref.setTitle(r.getTitle());
            ref.setUrl(null);
            ref.setFilePath(r.getFilePath());
            ref.setStartLine(r.getStartLine());
            ref.setEndLine(r.getEndLine());
            ref.setScore(r.getScore());
            ref.setSnippet(r.getContent());
            return ref;
        }).collect(Collectors.toList());
    }
}
