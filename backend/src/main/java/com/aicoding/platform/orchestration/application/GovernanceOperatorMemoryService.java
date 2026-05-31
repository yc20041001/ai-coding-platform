package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceOperatorActionMemoryEntity;
import com.aicoding.platform.orchestration.dto.GovernanceOperatorActionMemoryResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceOperatorActionMemoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceOperatorMemoryService {

    private final GovernanceOperatorActionMemoryMapper mapper;

    public GovernanceOperatorMemoryService(GovernanceOperatorActionMemoryMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceOperatorActionMemoryResponse recordAction(String sessionIdStr, String actionType, String actionTargetType,
                                                                 String operatorName, Boolean acceptedFlag, Boolean successFlag,
                                                                 Integer durationSeconds, String noteText) {
        GovernanceOperatorActionMemoryEntity entity = new GovernanceOperatorActionMemoryEntity();
        entity.setSessionId(parseLong(sessionIdStr));
        entity.setActionType(actionType); entity.setActionTargetType(actionTargetType);
        entity.setOperatorName(operatorName); entity.setOccurredAt(LocalDateTime.now());
        entity.setAcceptedFlag(Boolean.TRUE.equals(acceptedFlag) ? 1 : 0);
        entity.setSuccessFlag(Boolean.TRUE.equals(successFlag) ? 1 : 0);
        entity.setDurationSeconds(durationSeconds); entity.setNoteText(noteText);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceOperatorActionMemoryResponse> listActions(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceOperatorActionMemoryEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceOperatorActionMemoryEntity::getSessionId, sessionId);
        w.orderByDesc(GovernanceOperatorActionMemoryEntity::getOccurredAt);
        return mapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GovernanceOperatorActionMemoryResponse> listAllActions() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceOperatorActionMemoryEntity>()
                .orderByDesc(GovernanceOperatorActionMemoryEntity::getOccurredAt).last("LIMIT 100"))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceOperatorActionMemoryResponse toResponse(GovernanceOperatorActionMemoryEntity e) {
        GovernanceOperatorActionMemoryResponse r = new GovernanceOperatorActionMemoryResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setGuidedTaskId(e.getGuidedTaskId() != null ? e.getGuidedTaskId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setActionType(e.getActionType());
        r.setActionTargetType(e.getActionTargetType());
        r.setActionTargetId(e.getActionTargetId() != null ? e.getActionTargetId().toString() : null);
        r.setAcceptedFlag(e.getAcceptedFlag() != null && e.getAcceptedFlag() == 1);
        r.setSuccessFlag(e.getSuccessFlag() != null && e.getSuccessFlag() == 1);
        r.setDurationSeconds(e.getDurationSeconds()); r.setNoteText(e.getNoteText());
        r.setOccurredAt(e.getOccurredAt()); r.setCreateTime(e.getCreateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
