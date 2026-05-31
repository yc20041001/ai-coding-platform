package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceKnowledgeEntryEntity;
import com.aicoding.platform.orchestration.dto.GovernanceKnowledgeEntryResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceKnowledgeEntryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceKnowledgeBaseService {

    private final GovernanceKnowledgeEntryMapper mapper;

    public GovernanceKnowledgeBaseService(GovernanceKnowledgeEntryMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceKnowledgeEntryResponse createEntry(String title, String category, String sourceType,
                                                          String summaryText, String detailMarkdown, String tagsJson) {
        GovernanceKnowledgeEntryEntity entity = new GovernanceKnowledgeEntryEntity();
        entity.setTitle(title); entity.setCategory(category);
        entity.setSourceType(sourceType != null ? sourceType : "RECOMMENDATION");
        entity.setSummaryText(summaryText); entity.setDetailMarkdown(detailMarkdown);
        entity.setTagsJson(tagsJson); entity.setEffectivenessScore(BigDecimal.ZERO); entity.setReuseCount(0);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceKnowledgeEntryResponse> listEntries() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceKnowledgeEntryEntity>()
                .orderByDesc(GovernanceKnowledgeEntryEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceKnowledgeEntryResponse getEntry(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceKnowledgeEntryResponse updateEntry(String idStr, String title, String summaryText,
                                                          String detailMarkdown, String tagsJson) {
        GovernanceKnowledgeEntryEntity entity = findEntity(idStr);
        if (title != null) entity.setTitle(title);
        if (summaryText != null) entity.setSummaryText(summaryText);
        if (detailMarkdown != null) entity.setDetailMarkdown(detailMarkdown);
        if (tagsJson != null) entity.setTagsJson(tagsJson);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceKnowledgeEntryResponse> search(String keyword, String category) {
        LambdaQueryWrapper<GovernanceKnowledgeEntryEntity> w = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) w.eq(GovernanceKnowledgeEntryEntity::getCategory, category);
        if (keyword != null && !keyword.isEmpty()) {
            w.and(q -> q.like(GovernanceKnowledgeEntryEntity::getTitle, keyword)
                    .or().like(GovernanceKnowledgeEntryEntity::getSummaryText, keyword));
        }
        w.orderByDesc(GovernanceKnowledgeEntryEntity::getEffectivenessScore);
        return mapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GovernanceKnowledgeEntryResponse> getTopEntries() {
        LambdaQueryWrapper<GovernanceKnowledgeEntryEntity> w = new LambdaQueryWrapper<>();
        w.orderByDesc(GovernanceKnowledgeEntryEntity::getEffectivenessScore).last("LIMIT 10");
        List<GovernanceKnowledgeEntryEntity> list = mapper.selectList(w);
        if (list.isEmpty()) {
            w = new LambdaQueryWrapper<>(); w.last("LIMIT 10");
            list = mapper.selectList(w);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceKnowledgeEntryEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceKnowledgeEntryEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Knowledge entry 不存在");
        return entity;
    }

    private GovernanceKnowledgeEntryResponse toResponse(GovernanceKnowledgeEntryEntity e) {
        GovernanceKnowledgeEntryResponse r = new GovernanceKnowledgeEntryResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
        r.setSourceType(e.getSourceType()); r.setSourceId(e.getSourceId() != null ? e.getSourceId().toString() : null);
        r.setTitle(e.getTitle()); r.setCategory(e.getCategory()); r.setTagsJson(e.getTagsJson());
        r.setSummaryText(e.getSummaryText()); r.setDetailMarkdown(e.getDetailMarkdown());
        r.setEffectivenessScore(e.getEffectivenessScore()); r.setReuseCount(e.getReuseCount());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
