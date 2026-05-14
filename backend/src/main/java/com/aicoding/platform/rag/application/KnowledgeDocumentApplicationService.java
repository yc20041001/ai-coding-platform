package com.aicoding.platform.rag.application;

import com.aicoding.platform.audit.application.AuditLogApplicationService;
import com.aicoding.platform.audit.domain.AuditActionType;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.rag.domain.DocumentChunkEntity;
import com.aicoding.platform.rag.domain.KnowledgeBaseEntity;
import com.aicoding.platform.rag.domain.KnowledgeDocumentEntity;
import com.aicoding.platform.rag.domain.KnowledgeDocumentStatus;
import com.aicoding.platform.rag.domain.KnowledgeDocumentType;
import com.aicoding.platform.rag.dto.DocumentChunkResponse;
import com.aicoding.platform.rag.dto.KnowledgeDocumentResponse;
import com.aicoding.platform.rag.dto.UploadKnowledgeDocumentRequest;
import com.aicoding.platform.rag.infrastructure.DocumentChunkMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.aicoding.platform.rag.infrastructure.KnowledgeDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeDocumentApplicationService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkService documentChunkService;
    private final ProjectPermissionService projectPermissionService;
    private final AuditLogApplicationService auditLogApplicationService;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            KnowledgeDocumentType.MARKDOWN.name(),
            KnowledgeDocumentType.TEXT.name(),
            KnowledgeDocumentType.CODE.name());

    public KnowledgeDocumentApplicationService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                               DocumentChunkMapper documentChunkMapper,
                                               KnowledgeBaseMapper knowledgeBaseMapper,
                                               DocumentChunkService documentChunkService,
                                               ProjectPermissionService projectPermissionService,
                                               AuditLogApplicationService auditLogApplicationService) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentChunkService = documentChunkService;
        this.projectPermissionService = projectPermissionService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public KnowledgeDocumentResponse uploadDocument(Long projectId, UploadKnowledgeDocumentRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER);

        if (!SUPPORTED_TYPES.contains(request.getDocumentType())) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "不支持的文档类型: " + request.getDocumentType() + "，当前仅支持 MARKDOWN/TEXT/CODE");
        }

        Long kbId = Long.valueOf(request.getKnowledgeBaseId());
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getProjectId().equals(projectId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "知识库不存在或不属于当前项目");
        }

        KnowledgeDocumentEntity doc = new KnowledgeDocumentEntity();
        doc.setProjectId(projectId);
        doc.setKnowledgeBaseId(kbId);
        doc.setTitle(request.getTitle());
        doc.setDocumentType(request.getDocumentType());
        doc.setSourceType(request.getSourceType() != null ? request.getSourceType() : "MANUAL");
        doc.setFileName(request.getFileName());
        doc.setFilePath(request.getFilePath());
        doc.setContent(request.getContent());
        doc.setContentHash(documentChunkService.hashContent(request.getContent()));
        doc.setStatus(KnowledgeDocumentStatus.PROCESSING.name());
        doc.setChunkCount(0L);
        doc.setTokenCount(0L);
        knowledgeDocumentMapper.insert(doc);

        try {
            int chunkSize = defaultChunkSize(kb.getChunkSize());
            int chunkOverlap = defaultChunkOverlap(kb.getChunkOverlap());

            List<String> chunks = documentChunkService.splitIntoChunks(request.getContent(), chunkSize, chunkOverlap);
            long totalTokens = 0L;
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                DocumentChunkEntity chunk = new DocumentChunkEntity();
                chunk.setProjectId(projectId);
                chunk.setKnowledgeBaseId(kbId);
                chunk.setDocumentId(doc.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkContent);
                chunk.setContentHash(documentChunkService.hashContent(chunkContent));
                chunk.setTokenCount(documentChunkService.estimateTokens(chunkContent));
                chunk.setEmbeddingMock(documentChunkService.mockEmbedding(chunkContent));
                totalTokens += chunk.getTokenCount();
                documentChunkMapper.insert(chunk);
            }

            doc.setStatus(KnowledgeDocumentStatus.COMPLETED.name());
            doc.setChunkCount((long) chunks.size());
            doc.setTokenCount(totalTokens);
            knowledgeDocumentMapper.updateById(doc);

            kb.setDocumentCount(kb.getDocumentCount() + 1);
            kb.setChunkCount(kb.getChunkCount() + chunks.size());
            knowledgeBaseMapper.updateById(kb);

            auditLogApplicationService.recordSuccess(projectId, doc.getId(),
                    AuditActionType.RAG_DOCUMENT_UPLOAD.name(), "DOCUMENT",
                    "Upload document \"" + doc.getTitle() + "\" to KB #" + kbId);

        } catch (Exception e) {
            doc.setStatus(KnowledgeDocumentStatus.FAILED.name());
            doc.setErrorMessage("切片处理异常: " + e.getMessage());
            knowledgeDocumentMapper.updateById(doc);

            auditLogApplicationService.recordFailure(projectId, doc.getId(),
                    AuditActionType.RAG_DOCUMENT_UPLOAD.name(), "DOCUMENT",
                    "Upload document \"" + doc.getTitle() + "\" to KB #" + kbId, doc.getErrorMessage());
        }

        return toDocumentResponse(doc);
    }

    @Transactional(readOnly = true)
    public PageResult<KnowledgeDocumentResponse> listDocuments(Long knowledgeBaseId, PageQuery pageQuery) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        projectPermissionService.checkProjectRole(kb.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(KnowledgeDocumentEntity::getCreateTime);

        Page<KnowledgeDocumentEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<KnowledgeDocumentEntity> result = knowledgeDocumentMapper.selectPage(page, wrapper);

        List<KnowledgeDocumentResponse> records = result.getRecords().stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse getDocument(Long documentId) {
        KnowledgeDocumentEntity doc = getDocumentOrThrow(documentId);
        projectPermissionService.checkProjectRole(doc.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);
        return toDocumentResponse(doc);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        KnowledgeDocumentEntity doc = getDocumentOrThrow(documentId);
        projectPermissionService.checkProjectRole(doc.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunkEntity>()
                .eq(DocumentChunkEntity::getDocumentId, documentId));

        knowledgeDocumentMapper.deleteById(documentId);

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
        if (kb != null) {
            kb.setDocumentCount(Math.max(0, kb.getDocumentCount() - 1));
            kb.setChunkCount(Math.max(0, kb.getChunkCount() - doc.getChunkCount()));
            knowledgeBaseMapper.updateById(kb);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> listChunks(Long documentId) {
        KnowledgeDocumentEntity doc = getDocumentOrThrow(documentId);
        projectPermissionService.checkProjectRole(doc.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<DocumentChunkEntity> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkEntity>()
                        .eq(DocumentChunkEntity::getDocumentId, documentId)
                        .orderByAsc(DocumentChunkEntity::getChunkIndex));

        return chunks.stream().map(this::toChunkResponse).collect(Collectors.toList());
    }

    public KnowledgeDocumentEntity getDocumentOrThrow(Long documentId) {
        KnowledgeDocumentEntity doc = knowledgeDocumentMapper.selectById(documentId);
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return doc;
    }

    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentResponse resp = new KnowledgeDocumentResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setKnowledgeBaseId(entity.getKnowledgeBaseId().toString());
        resp.setTitle(entity.getTitle());
        resp.setSourceType(entity.getSourceType());
        resp.setDocumentType(entity.getDocumentType());
        resp.setFileName(entity.getFileName());
        resp.setFilePath(entity.getFilePath());
        resp.setStatus(entity.getStatus());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setChunkCount(entity.getChunkCount());
        resp.setTokenCount(entity.getTokenCount());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private DocumentChunkResponse toChunkResponse(DocumentChunkEntity entity) {
        DocumentChunkResponse resp = new DocumentChunkResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setKnowledgeBaseId(entity.getKnowledgeBaseId().toString());
        resp.setDocumentId(entity.getDocumentId().toString());
        resp.setChunkIndex(entity.getChunkIndex());
        resp.setContent(entity.getContent());
        resp.setTokenCount(entity.getTokenCount());
        resp.setMetadata(entity.getMetadata());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    private int defaultChunkSize(Integer value) {
        if (value == null) {
            return 1000;
        }
        return value;
    }

    private int defaultChunkOverlap(Integer value) {
        if (value == null) {
            return 100;
        }
        return value;
    }
}
