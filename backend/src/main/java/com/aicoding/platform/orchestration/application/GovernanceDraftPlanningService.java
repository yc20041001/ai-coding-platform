package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceDraftRemediationPlanEntity;
import com.aicoding.platform.orchestration.dto.GovernanceDraftRemediationPlanResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceDraftRemediationPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceDraftPlanningService {

    private final GovernanceDraftRemediationPlanMapper mapper;

    public GovernanceDraftPlanningService(GovernanceDraftRemediationPlanMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceDraftRemediationPlanResponse createPlan(String planTitle, String scopeType) {
        GovernanceDraftRemediationPlanEntity entity = new GovernanceDraftRemediationPlanEntity();
        entity.setPlanTitle(planTitle);
        entity.setScopeType(scopeType != null ? scopeType : "RECOMMENDATION");
        entity.setPlanStatus("DRAFT");
        entity.setProposedStepsJson("[{\"step\":\"Analyze context\",\"status\":\"TODO\"},{\"step\":\"Apply remediation\",\"status\":\"TODO\"}]");
        entity.setRiskLevel("MEDIUM");
        entity.setHumanConfirmationRequired(1);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceDraftRemediationPlanResponse refreshPlan(String planIdStr) {
        GovernanceDraftRemediationPlanEntity entity = findEntity(planIdStr);
        // Auto-generate proposed steps based on context
        String steps = "[{\"step\":\"Review recommendation context\",\"status\":\"TODO\",\"required\":true},"
                + "{\"step\":\"Identify applicable playbook/recipe\",\"status\":\"TODO\",\"required\":true},"
                + "{\"step\":\"Prepare remediation steps\",\"status\":\"TODO\",\"required\":true},"
                + "{\"step\":\"Review and confirm\",\"status\":\"TODO\",\"required\":true}]";
        entity.setProposedStepsJson(steps);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceDraftRemediationPlanResponse> listPlans() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceDraftRemediationPlanEntity>()
                .orderByDesc(GovernanceDraftRemediationPlanEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceDraftRemediationPlanResponse getPlan(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceDraftRemediationPlanResponse updatePlan(String idStr, String planTitle, String summaryText,
                                                               String goalText, String proposedStepsJson) {
        GovernanceDraftRemediationPlanEntity entity = findEntity(idStr);
        if (planTitle != null) entity.setPlanTitle(planTitle);
        if (summaryText != null) entity.setSummaryText(summaryText);
        if (goalText != null) entity.setGoalText(goalText);
        if (proposedStepsJson != null) entity.setProposedStepsJson(proposedStepsJson);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceDraftRemediationPlanResponse updatePlanStatus(String idStr, String newStatus) {
        GovernanceDraftRemediationPlanEntity entity = findEntity(idStr);
        String current = entity.getPlanStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid plan transition from " + current + " to " + newStatus);
        }
        entity.setPlanStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("DRAFT".equals(current) && "READY_FOR_REVIEW".equals(next)) return true;
        if ("READY_FOR_REVIEW".equals(current) && "REVIEWED".equals(next)) return true;
        if ("REVIEWED".equals(current) && "ARCHIVED".equals(next)) return true;
        return false;
    }

    private GovernanceDraftRemediationPlanEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceDraftRemediationPlanEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Draft plan 不存在");
        return entity;
    }

    private GovernanceDraftRemediationPlanResponse toResponse(GovernanceDraftRemediationPlanEntity e) {
        GovernanceDraftRemediationPlanResponse r = new GovernanceDraftRemediationPlanResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setPlanStatus(e.getPlanStatus());
        r.setPlanTitle(e.getPlanTitle()); r.setScopeType(e.getScopeType());
        r.setSummaryText(e.getSummaryText()); r.setGoalText(e.getGoalText());
        r.setProposedStepsJson(e.getProposedStepsJson());
        r.setLinkedBundleId(e.getLinkedBundleId() != null ? e.getLinkedBundleId().toString() : null);
        r.setLinkedPlaybookKey(e.getLinkedPlaybookKey()); r.setLinkedRecipeKey(e.getLinkedRecipeKey());
        r.setRiskLevel(e.getRiskLevel());
        r.setHumanConfirmationRequired(e.getHumanConfirmationRequired() != null && e.getHumanConfirmationRequired() == 1);
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
