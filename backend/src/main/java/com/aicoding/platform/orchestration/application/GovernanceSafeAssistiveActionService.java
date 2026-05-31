package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceSafeAssistiveActionEntity;
import com.aicoding.platform.orchestration.dto.GovernanceSafeAssistiveActionResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceSafeAssistiveActionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceSafeAssistiveActionService {

    private final GovernanceSafeAssistiveActionMapper mapper;

    public GovernanceSafeAssistiveActionService(GovernanceSafeAssistiveActionMapper mapper) { this.mapper = mapper; }

    @Transactional
    public List<GovernanceSafeAssistiveActionResponse> generateActions(String draftPlanIdStr) {
        Long draftPlanId = parseLong(draftPlanIdStr);
        // Delete existing
        LambdaQueryWrapper<GovernanceSafeAssistiveActionEntity> d = new LambdaQueryWrapper<>();
        d.eq(GovernanceSafeAssistiveActionEntity::getDraftPlanId, draftPlanId);
        mapper.delete(d);

        List<GovernanceSafeAssistiveActionEntity> actions = new ArrayList<>();
        int order = 0;

        String[][] defaults = {
            {"OPEN_PLAYBOOK_DRAFT", "Prepare playbook draft", "SAFE", "Draft playbook steps for this recommendation"},
            {"OPEN_RECIPE_DRAFT", "Prepare recipe guidance", "SAFE", "Draft recipe application notes"},
            {"PREPARE_HANDOFF_NOTE", "Prepare handoff note", "CAUTION", "Draft handoff note template"},
            {"PREPARE_WAIVER_REVIEW", "Prepare waiver review", "REVIEW_REQUIRED", "Review active waiver impact"},
            {"PREPARE_FORECAST_CHECK", "Prepare forecast check", "INFO", "Review forecast relevance"},
            {"PREPARE_RISK_SUMMARY", "Prepare risk summary", "CAUTION", "Summarize risk factors"}
        };

        for (String[] def : defaults) {
            GovernanceSafeAssistiveActionEntity a = new GovernanceSafeAssistiveActionEntity();
            a.setDraftPlanId(draftPlanId); a.setActionType(def[0]); a.setActionTitle(def[1]);
            a.setSafetyLevel(def[2]); a.setActionSummary(def[3]);
            a.setActionStatus("PENDING"); a.setConfirmationRequired(1);
            a.setChecklistJson("[{\"key\":\"review\",\"label\":\"Review content\",\"done\":false}]");
            a.setActionOrder(order++);
            actions.add(a);
        }

        for (var a : actions) mapper.insert(a);
        return listActions(draftPlanIdStr);
    }

    @Transactional(readOnly = true)
    public List<GovernanceSafeAssistiveActionResponse> listActions(String draftPlanIdStr) {
        Long draftPlanId = parseLong(draftPlanIdStr);
        LambdaQueryWrapper<GovernanceSafeAssistiveActionEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceSafeAssistiveActionEntity::getDraftPlanId, draftPlanId);
        w.orderByAsc(GovernanceSafeAssistiveActionEntity::getActionOrder);
        return mapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public GovernanceSafeAssistiveActionResponse updateActionStatus(String actionIdStr, String newStatus) {
        GovernanceSafeAssistiveActionEntity entity = findEntity(actionIdStr);
        String current = entity.getActionStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid action transition from " + current + " to " + newStatus);
        }
        entity.setActionStatus(newStatus);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("PENDING".equals(current) && ("REVIEWED".equals(next) || "SKIPPED".equals(next))) return true;
        if ("REVIEWED".equals(current) && "READY".equals(next)) return true;
        return false;
    }

    private GovernanceSafeAssistiveActionEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceSafeAssistiveActionEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Assistive action 不存在");
        return entity;
    }

    private GovernanceSafeAssistiveActionResponse toResponse(GovernanceSafeAssistiveActionEntity e) {
        GovernanceSafeAssistiveActionResponse r = new GovernanceSafeAssistiveActionResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setDraftPlanId(e.getDraftPlanId() != null ? e.getDraftPlanId().toString() : null);
        r.setActionType(e.getActionType()); r.setActionStatus(e.getActionStatus());
        r.setActionTitle(e.getActionTitle()); r.setActionSummary(e.getActionSummary());
        r.setSafetyLevel(e.getSafetyLevel());
        r.setConfirmationRequired(e.getConfirmationRequired() != null && e.getConfirmationRequired() == 1);
        r.setChecklistJson(e.getChecklistJson()); r.setPrefillPayloadJson(e.getPrefillPayloadJson());
        r.setActionOrder(e.getActionOrder());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
