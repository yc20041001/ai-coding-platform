package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceHandoffChecklistEntity;
import com.aicoding.platform.orchestration.dto.GovernanceHandoffChecklistResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceHandoffChecklistMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceHandoffAssistantService {

    private final GovernanceHandoffChecklistMapper governanceHandoffChecklistMapper;

    public GovernanceHandoffAssistantService(GovernanceHandoffChecklistMapper governanceHandoffChecklistMapper) {
        this.governanceHandoffChecklistMapper = governanceHandoffChecklistMapper;
    }

    @Transactional
    public GovernanceHandoffChecklistResponse createChecklist(String recommendationIdStr, String executionPlanIdStr,
                                                                String fromOwnerName, String toOwnerName) {
        GovernanceHandoffChecklistEntity entity = new GovernanceHandoffChecklistEntity();
        entity.setRecommendationId(parseLong(recommendationIdStr));
        if (executionPlanIdStr != null) entity.setExecutionPlanId(parseLong(executionPlanIdStr));
        entity.setFromOwnerName(fromOwnerName);
        entity.setToOwnerName(toOwnerName);
        entity.setChecklistStatus("OPEN");

        // Default checklist items
        String defaultItems = "[{\"key\":\"status_review\",\"label\":\"确认 recommendation 当前状态\",\"done\":false},"
                + "{\"key\":\"blocker_review\",\"label\":\"确认已存在的 blocker / waiver\",\"done\":false},"
                + "{\"key\":\"sla_check\",\"label\":\"确认下一个 SLA 截止时间\",\"done\":false},"
                + "{\"key\":\"progress_review\",\"label\":\"确认已完成步骤与剩余步骤\",\"done\":false},"
                + "{\"key\":\"sync_incidents\",\"label\":\"确认需要同步的 incident / alert / evidence\",\"done\":false}]";
        entity.setChecklistItemsJson(defaultItems);
        governanceHandoffChecklistMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceHandoffChecklistResponse> listChecklists() {
        return governanceHandoffChecklistMapper.selectList(new LambdaQueryWrapper<GovernanceHandoffChecklistEntity>()
                .orderByDesc(GovernanceHandoffChecklistEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceHandoffChecklistResponse getChecklist(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceHandoffChecklistResponse updateChecklist(String idStr, String handoffNote) {
        GovernanceHandoffChecklistEntity entity = findEntity(idStr);
        if (handoffNote != null) entity.setHandoffNote(handoffNote);
        entity.setUpdateTime(LocalDateTime.now());
        governanceHandoffChecklistMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceHandoffChecklistResponse updateChecklistStatus(String idStr, String newStatus) {
        GovernanceHandoffChecklistEntity entity = findEntity(idStr);
        String current = entity.getChecklistStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid checklist transition from " + current + " to " + newStatus);
        }
        entity.setChecklistStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) entity.setHandedOffAt(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        governanceHandoffChecklistMapper.updateById(entity);
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("OPEN".equals(current) && ("IN_PROGRESS".equals(next) || "CANCELLED".equals(next))) return true;
        if ("IN_PROGRESS".equals(current) && "COMPLETED".equals(next)) return true;
        return false;
    }

    private GovernanceHandoffChecklistEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceHandoffChecklistEntity entity = governanceHandoffChecklistMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Handoff checklist 不存在");
        return entity;
    }

    private GovernanceHandoffChecklistResponse toResponse(GovernanceHandoffChecklistEntity e) {
        GovernanceHandoffChecklistResponse r = new GovernanceHandoffChecklistResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setExecutionPlanId(e.getExecutionPlanId() != null ? e.getExecutionPlanId().toString() : null);
        r.setFromOwnerId(e.getFromOwnerId() != null ? e.getFromOwnerId().toString() : null);
        r.setFromOwnerName(e.getFromOwnerName()); r.setToOwnerId(e.getToOwnerId() != null ? e.getToOwnerId().toString() : null);
        r.setToOwnerName(e.getToOwnerName()); r.setChecklistStatus(e.getChecklistStatus());
        r.setChecklistItemsJson(e.getChecklistItemsJson()); r.setHandoffNote(e.getHandoffNote());
        r.setHandedOffAt(e.getHandedOffAt()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) {
        try { return Long.parseLong(v); }
        catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); }
    }
}
