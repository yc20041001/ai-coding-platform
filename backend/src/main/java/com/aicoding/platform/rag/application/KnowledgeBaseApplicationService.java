package com.aicoding.platform.rag.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.pagination.PageQuery;
import com.aicoding.platform.common.pagination.PageResult;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.rag.domain.KnowledgeBaseEntity;
import com.aicoding.platform.rag.domain.KnowledgeBaseStatus;
import com.aicoding.platform.rag.dto.CreateKnowledgeBaseRequest;
import com.aicoding.platform.rag.dto.KnowledgeBaseResponse;
import com.aicoding.platform.rag.dto.UpdateKnowledgeBaseRequest;
import com.aicoding.platform.rag.infrastructure.KnowledgeBaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseApplicationService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ProjectPermissionService projectPermissionService;

    public KnowledgeBaseApplicationService(KnowledgeBaseMapper knowledgeBaseMapper,
                                           ProjectPermissionService projectPermissionService) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(Long projectId, CreateKnowledgeBaseRequest request) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        Long count = knowledgeBaseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getProjectId, projectId)
                        .eq(KnowledgeBaseEntity::getName, request.getName()));
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "知识库名称重复: " + request.getName());
        }

        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setProjectId(projectId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(KnowledgeBaseStatus.ACTIVE.name());
        entity.setEmbeddingProvider("MOCK");
        entity.setEmbeddingModel("mock-embedding-v1");
        Integer chunkSize = request.getChunkSize();
        if (chunkSize == null) {
            chunkSize = 1000;
        }
        entity.setChunkSize(chunkSize);

        Integer chunkOverlap = request.getChunkOverlap();
        if (chunkOverlap == null) {
            chunkOverlap = 100;
        }
        entity.setChunkOverlap(chunkOverlap);
        entity.setDocumentCount(0L);
        entity.setChunkCount(0L);
        knowledgeBaseMapper.insert(entity);

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResult<KnowledgeBaseResponse> listKnowledgeBases(Long projectId, PageQuery pageQuery) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        LambdaQueryWrapper<KnowledgeBaseEntity> wrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getProjectId, projectId)
                .orderByDesc(KnowledgeBaseEntity::getCreateTime);

        Page<KnowledgeBaseEntity> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        Page<KnowledgeBaseEntity> result = knowledgeBaseMapper.selectPage(page, wrapper);

        List<KnowledgeBaseResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, pageQuery.getPage(), pageQuery.getPageSize(), result.getTotal());
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBaseEntity entity = getOrThrow(knowledgeBaseId);
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);
        return toResponse(entity);
    }

    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(Long knowledgeBaseId, UpdateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = getOrThrow(knowledgeBaseId);
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus());
        }
        if (request.getChunkSize() != null && request.getChunkSize() > 0) {
            entity.setChunkSize(request.getChunkSize());
        }
        if (request.getChunkOverlap() != null && request.getChunkOverlap() >= 0) {
            entity.setChunkOverlap(request.getChunkOverlap());
        }
        knowledgeBaseMapper.updateById(entity);

        return toResponse(entity);
    }

    @Transactional
    public void deleteKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBaseEntity entity = getOrThrow(knowledgeBaseId);
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER);
        knowledgeBaseMapper.deleteById(knowledgeBaseId);
    }

    @Transactional
    public KnowledgeBaseEntity getOrCreateDefaultKnowledgeBase(Long projectId) {
        KnowledgeBaseEntity existing = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getProjectId, projectId)
                        .eq(KnowledgeBaseEntity::getName, "Default Knowledge Base"));
        if (existing != null) {
            return existing;
        }

        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setProjectId(projectId);
        entity.setName("Default Knowledge Base");
        entity.setStatus(KnowledgeBaseStatus.ACTIVE.name());
        entity.setEmbeddingProvider("MOCK");
        entity.setEmbeddingModel("mock-embedding-v1");
        entity.setChunkSize(1000);
        entity.setChunkOverlap(100);
        entity.setDocumentCount(0L);
        entity.setChunkCount(0L);
        knowledgeBaseMapper.insert(entity);
        return entity;
    }

    public KnowledgeBaseEntity getOrThrow(Long knowledgeBaseId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        return entity;
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBaseEntity entity) {
        KnowledgeBaseResponse resp = new KnowledgeBaseResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setStatus(entity.getStatus());
        resp.setEmbeddingProvider(entity.getEmbeddingProvider());
        resp.setEmbeddingModel(entity.getEmbeddingModel());
        resp.setChunkSize(entity.getChunkSize());
        resp.setChunkOverlap(entity.getChunkOverlap());
        resp.setDocumentCount(entity.getDocumentCount());
        resp.setChunkCount(entity.getChunkCount());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
