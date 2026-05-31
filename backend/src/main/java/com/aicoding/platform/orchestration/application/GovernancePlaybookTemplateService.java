package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationItemEntity;
import com.aicoding.platform.orchestration.domain.GovernanceRecommendationPlaybookTemplateEntity;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.GovernanceRecommendationPlaybookTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernancePlaybookTemplateService {

    private final GovernanceRecommendationPlaybookTemplateMapper mapper;
    private final GovernanceRecommendationWorkflowService workflowService;

    public GovernancePlaybookTemplateService(GovernanceRecommendationPlaybookTemplateMapper mapper,
                                              GovernanceRecommendationWorkflowService workflowService) {
        this.mapper = mapper;
        this.workflowService = workflowService;
    }

    @Transactional
    public GovernancePlaybookTemplateResponse createTemplate(CreateGovernancePlaybookTemplateRequest req) {
        LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> dup = new LambdaQueryWrapper<>();
        dup.eq(GovernanceRecommendationPlaybookTemplateEntity::getTemplateKey, req.getTemplateKey());
        if (mapper.selectCount(dup) > 0) throw new BizException(ErrorCode.CONFLICT, "Template key " + req.getTemplateKey() + " 已存在");

        GovernanceRecommendationPlaybookTemplateEntity entity = new GovernanceRecommendationPlaybookTemplateEntity();
        entity.setTemplateKey(req.getTemplateKey()); entity.setDisplayName(req.getDisplayName());
        entity.setRecommendationCategory(req.getRecommendationCategory()); entity.setGuardrailKey(req.getGuardrailKey());
        entity.setPriority(req.getPriority()); entity.setEnabled(1);
        entity.setTemplateStepsJson(req.getTemplateStepsJson() != null ? req.getTemplateStepsJson() : "[]");
        entity.setSuccessCriteriaJson(req.getSuccessCriteriaJson()); entity.setHandoffNotes(req.getHandoffNotes());
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernancePlaybookTemplateResponse> listTemplates() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity>()
                .orderByDesc(GovernanceRecommendationPlaybookTemplateEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernancePlaybookTemplateResponse getTemplate(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernancePlaybookTemplateResponse updateTemplate(String idStr, UpdateGovernancePlaybookTemplateRequest req) {
        GovernanceRecommendationPlaybookTemplateEntity entity = findEntity(idStr);
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getRecommendationCategory() != null) entity.setRecommendationCategory(req.getRecommendationCategory());
        if (req.getGuardrailKey() != null) entity.setGuardrailKey(req.getGuardrailKey());
        if (req.getPriority() != null) entity.setPriority(req.getPriority());
        if (req.getTemplateStepsJson() != null) entity.setTemplateStepsJson(req.getTemplateStepsJson());
        if (req.getSuccessCriteriaJson() != null) entity.setSuccessCriteriaJson(req.getSuccessCriteriaJson());
        if (req.getHandoffNotes() != null) entity.setHandoffNotes(req.getHandoffNotes());
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernancePlaybookTemplateResponse updateTemplateStatus(String idStr, Boolean enabled) {
        GovernanceRecommendationPlaybookTemplateEntity entity = findEntity(idStr);
        entity.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public GovernancePlaybookMatchPreviewResponse matchPreview(String recommendationIdStr) {
        Long recId = parseLong(recommendationIdStr);
        GovernanceRecommendationItemEntity item;
        try {
            item = workflowService.getItemEntity(recId);
        } catch (Exception e) {
            // If the item doesn't exist, return empty preview
            GovernancePlaybookMatchPreviewResponse resp = new GovernancePlaybookMatchPreviewResponse();
            resp.setRecommendationId(recommendationIdStr);
            resp.setMatchMode("DEFAULT");
            return resp;
        }

        String category = item.getCategory();
        String priority = item.getPriority();
        String guardrailKey = item.getGuardrailKey();

        GovernancePlaybookMatchPreviewResponse resp = new GovernancePlaybookMatchPreviewResponse();
        resp.setRecommendationId(recommendationIdStr);

        LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);

        // Try EXACT: guardrail + priority
        if (guardrailKey != null && priority != null) {
            LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> exact = new LambdaQueryWrapper<>();
            exact.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);
            exact.eq(GovernanceRecommendationPlaybookTemplateEntity::getGuardrailKey, guardrailKey);
            exact.eq(GovernanceRecommendationPlaybookTemplateEntity::getPriority, priority);
            List<GovernanceRecommendationPlaybookTemplateEntity> exactMatch = mapper.selectList(exact);
            if (!exactMatch.isEmpty()) {
                resp.setMatchMode("EXACT");
                resp.setMatchedTemplate(toResponse(exactMatch.get(0)));
                return resp;
            }
        }

        // Try CATEGORY_PRIORITY
        if (category != null && priority != null) {
            LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> cp = new LambdaQueryWrapper<>();
            cp.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);
            cp.eq(GovernanceRecommendationPlaybookTemplateEntity::getRecommendationCategory, category);
            cp.eq(GovernanceRecommendationPlaybookTemplateEntity::getPriority, priority);
            List<GovernanceRecommendationPlaybookTemplateEntity> cpMatch = mapper.selectList(cp);
            if (!cpMatch.isEmpty()) {
                resp.setMatchMode("CATEGORY_PRIORITY");
                resp.setMatchedTemplate(toResponse(cpMatch.get(0)));
                return resp;
            }
        }

        // Try CATEGORY_ONLY
        if (category != null) {
            LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> co = new LambdaQueryWrapper<>();
            co.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);
            co.eq(GovernanceRecommendationPlaybookTemplateEntity::getRecommendationCategory, category);
            List<GovernanceRecommendationPlaybookTemplateEntity> coMatch = mapper.selectList(co);
            if (!coMatch.isEmpty()) {
                resp.setMatchMode("CATEGORY_ONLY");
                resp.setMatchedTemplate(toResponse(coMatch.get(0)));
                return resp;
            }
        }

        // DEFAULT
        resp.setMatchMode("DEFAULT");
        LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> def = new LambdaQueryWrapper<>();
        def.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);
        def.last("LIMIT 1");
        List<GovernanceRecommendationPlaybookTemplateEntity> defMatch = mapper.selectList(def);
        if (!defMatch.isEmpty()) resp.setMatchedTemplate(toResponse(defMatch.get(0)));
        return resp;
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationPlaybookTemplateEntity> getEnabledTemplates() {
        LambdaQueryWrapper<GovernanceRecommendationPlaybookTemplateEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceRecommendationPlaybookTemplateEntity::getEnabled, 1);
        return mapper.selectList(w);
    }

    private GovernanceRecommendationPlaybookTemplateEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceRecommendationPlaybookTemplateEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Playbook template 不存在");
        return entity;
    }

    private GovernancePlaybookTemplateResponse toResponse(GovernanceRecommendationPlaybookTemplateEntity e) {
        GovernancePlaybookTemplateResponse r = new GovernancePlaybookTemplateResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setTemplateKey(e.getTemplateKey()); r.setDisplayName(e.getDisplayName());
        r.setRecommendationCategory(e.getRecommendationCategory()); r.setGuardrailKey(e.getGuardrailKey());
        r.setPriority(e.getPriority()); r.setEnabled(e.getEnabled() != null && e.getEnabled() == 1);
        r.setTemplateStepsJson(e.getTemplateStepsJson()); r.setSuccessCriteriaJson(e.getSuccessCriteriaJson());
        r.setHandoffNotes(e.getHandoffNotes()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) {
        try { return Long.parseLong(v); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); }
    }
}
