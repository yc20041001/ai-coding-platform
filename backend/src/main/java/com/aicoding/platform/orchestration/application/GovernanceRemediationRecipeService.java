package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRemediationRecipeEntity;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRemediationRecipeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceRemediationRecipeService {

    private final GovernanceRemediationRecipeMapper mapper;
    private final GovernanceRecommendationWorkflowService workflowService;

    public GovernanceRemediationRecipeService(GovernanceRemediationRecipeMapper mapper,
                                               GovernanceRecommendationWorkflowService workflowService) {
        this.mapper = mapper;
        this.workflowService = workflowService;
    }

    @Transactional
    public GovernanceRemediationRecipeResponse createRecipe(String recipeKey, String displayName, String recipeType,
                                                              String category, String guardrailKey, String stepsJson) {
        LambdaQueryWrapper<GovernanceRemediationRecipeEntity> dup = new LambdaQueryWrapper<>();
        dup.eq(GovernanceRemediationRecipeEntity::getRecipeKey, recipeKey);
        if (mapper.selectCount(dup) > 0) throw new BizException(ErrorCode.CONFLICT, "Recipe key " + recipeKey + " 已存在");
        GovernanceRemediationRecipeEntity entity = new GovernanceRemediationRecipeEntity();
        entity.setRecipeKey(recipeKey); entity.setDisplayName(displayName);
        entity.setRecipeType(recipeType != null ? recipeType : "REMEDIATION");
        entity.setRecommendationCategory(category); entity.setGuardrailKey(guardrailKey);
        entity.setStepsJson(stepsJson != null ? stepsJson : "[]");
        entity.setEffectivenessScore(BigDecimal.ZERO); entity.setUsageCount(0); entity.setEnabled(1);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRemediationRecipeResponse> listRecipes() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceRemediationRecipeEntity>()
                .orderByDesc(GovernanceRemediationRecipeEntity::getEffectivenessScore))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceRemediationRecipeResponse getRecipe(String idStr) { return toResponse(findEntity(idStr)); }

    @Transactional
    public GovernanceRemediationRecipeResponse updateRecipe(String idStr, String displayName, String stepsJson) {
        GovernanceRemediationRecipeEntity entity = findEntity(idStr);
        if (displayName != null) entity.setDisplayName(displayName);
        if (stepsJson != null) entity.setStepsJson(stepsJson);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceRemediationRecipeResponse updateRecipeStatus(String idStr, Boolean enabled) {
        GovernanceRemediationRecipeEntity entity = findEntity(idStr);
        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceRemediationRecipeResponse> getRecipeRecommendations(String recommendationIdStr) {
        Long recId = parseLong(recommendationIdStr);
        GovernanceRecommendationItemEntity item;
        try {
            item = workflowService.getItemEntity(recId);
        } catch (Exception e) { return List.of(); }

        String category = item.getCategory();
        String guardrailKey = item.getGuardrailKey();

        List<GovernanceRemediationRecipeEntity> enabled = mapper.selectList(
                new LambdaQueryWrapper<GovernanceRemediationRecipeEntity>().eq(GovernanceRemediationRecipeEntity::getEnabled, 1));

        List<RecipeScore> scored = new ArrayList<>();
        for (var recipe : enabled) {
            int score;
            if (category != null && category.equals(recipe.getRecommendationCategory())
                    && guardrailKey != null && guardrailKey.equals(recipe.getGuardrailKey())) {
                score = 100;
            } else if (category != null && category.equals(recipe.getRecommendationCategory())) {
                score = 40;
            } else {
                score = 10;
            }
            scored.add(new RecipeScore(recipe, score));
        }

        scored.sort((a, b) -> Integer.compare(b.matchScore, a.matchScore));
        return scored.stream().map(s -> toResponse(s.recipe)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GovernanceRemediationRecipeResponse> getTopRecipes() {
        LambdaQueryWrapper<GovernanceRemediationRecipeEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceRemediationRecipeEntity::getEnabled, 1);
        w.orderByDesc(GovernanceRemediationRecipeEntity::getEffectivenessScore).last("LIMIT 10");
        return mapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private static class RecipeScore {
        final GovernanceRemediationRecipeEntity recipe;
        final int matchScore;
        RecipeScore(GovernanceRemediationRecipeEntity r, int s) { this.recipe = r; this.matchScore = s; }
    }

    private GovernanceRemediationRecipeEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceRemediationRecipeEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Recipe 不存在");
        return entity;
    }

    private GovernanceRemediationRecipeResponse toResponse(GovernanceRemediationRecipeEntity e) {
        GovernanceRemediationRecipeResponse r = new GovernanceRemediationRecipeResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setRecipeKey(e.getRecipeKey()); r.setDisplayName(e.getDisplayName()); r.setRecipeType(e.getRecipeType());
        r.setRecommendationCategory(e.getRecommendationCategory()); r.setGuardrailKey(e.getGuardrailKey());
        r.setStepsJson(e.getStepsJson()); r.setPrerequisitesJson(e.getPrerequisitesJson());
        r.setSuccessCriteriaJson(e.getSuccessCriteriaJson());
        r.setEffectivenessScore(e.getEffectivenessScore()); r.setUsageCount(e.getUsageCount());
        r.setEnabled(e.getEnabled() != null && e.getEnabled() == 1);
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.valueOf(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
