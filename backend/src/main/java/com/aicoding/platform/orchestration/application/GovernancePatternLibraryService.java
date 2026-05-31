package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernancePatternLibraryItemEntity;
import com.aicoding.platform.orchestration.dto.GovernancePatternLibraryItemResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernancePatternLibraryItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernancePatternLibraryService {

    private final GovernancePatternLibraryItemMapper mapper;

    public GovernancePatternLibraryService(GovernancePatternLibraryItemMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernancePatternLibraryItemResponse createPattern(String patternKey, String displayName, String category,
                                                                String guardrailKey, String priority, String patternJson) {
        LambdaQueryWrapper<GovernancePatternLibraryItemEntity> dup = new LambdaQueryWrapper<>();
        dup.eq(GovernancePatternLibraryItemEntity::getPatternKey, patternKey);
        if (mapper.selectCount(dup) > 0) throw new BizException(ErrorCode.CONFLICT, "Pattern key " + patternKey + " 已存在");
        GovernancePatternLibraryItemEntity entity = new GovernancePatternLibraryItemEntity();
        entity.setPatternKey(patternKey); entity.setDisplayName(displayName);
        entity.setRecommendationCategory(category); entity.setGuardrailKey(guardrailKey);
        entity.setPriority(priority); entity.setPatternJson(patternJson != null ? patternJson : "{}");
        entity.setEnabled(1);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernancePatternLibraryItemResponse> listPatterns() {
        return mapper.selectList(new LambdaQueryWrapper<GovernancePatternLibraryItemEntity>()
                .orderByDesc(GovernancePatternLibraryItemEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernancePatternLibraryItemResponse getPattern(String idStr) { return toResponse(findEntity(idStr)); }

    @Transactional
    public GovernancePatternLibraryItemResponse updatePattern(String idStr, String displayName, String patternJson, String notes) {
        GovernancePatternLibraryItemEntity entity = findEntity(idStr);
        if (displayName != null) entity.setDisplayName(displayName);
        if (patternJson != null) entity.setPatternJson(patternJson);
        if (notes != null) entity.setNotes(notes);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernancePatternLibraryItemResponse updatePatternStatus(String idStr, Boolean enabled) {
        GovernancePatternLibraryItemEntity entity = findEntity(idStr);
        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private GovernancePatternLibraryItemEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernancePatternLibraryItemEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Pattern 不存在");
        return entity;
    }

    private GovernancePatternLibraryItemResponse toResponse(GovernancePatternLibraryItemEntity e) {
        GovernancePatternLibraryItemResponse r = new GovernancePatternLibraryItemResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setPatternKey(e.getPatternKey()); r.setDisplayName(e.getDisplayName());
        r.setRecommendationCategory(e.getRecommendationCategory()); r.setGuardrailKey(e.getGuardrailKey());
        r.setPriority(e.getPriority()); r.setPatternJson(e.getPatternJson()); r.setNotes(e.getNotes());
        r.setEnabled(e.getEnabled() != null && e.getEnabled() == 1);
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
