package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.GovernanceWorkspaceSessionEntity;
import com.aicoding.platform.orchestration.dto.GovernanceWorkspaceSessionResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceWorkspaceSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernanceWorkspaceService {

    private final GovernanceWorkspaceSessionMapper workspaceSessionMapper;

    public GovernanceWorkspaceService(GovernanceWorkspaceSessionMapper workspaceSessionMapper) {
        this.workspaceSessionMapper = workspaceSessionMapper;
    }

    @Transactional
    public GovernanceWorkspaceSessionResponse createSession(String operatorName, String focusMode) {
        GovernanceWorkspaceSessionEntity entity = new GovernanceWorkspaceSessionEntity();
        entity.setOperatorName(operatorName != null ? operatorName : "Admin");
        entity.setFocusMode(focusMode != null ? focusMode : "PRIORITY_FIRST");
        entity.setSessionStatus("ACTIVE");
        entity.setStartedAt(LocalDateTime.now());
        workspaceSessionMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceWorkspaceSessionResponse> listSessions() {
        return workspaceSessionMapper.selectList(new LambdaQueryWrapper<GovernanceWorkspaceSessionEntity>()
                .orderByDesc(GovernanceWorkspaceSessionEntity::getCreateTime))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceWorkspaceSessionResponse getSession(String idStr) {
        return toResponse(findEntity(idStr));
    }

    @Transactional
    public GovernanceWorkspaceSessionResponse updateSession(String idStr, String focusMode, String selectedProjectId,
                                                              String selectedRecommendationId, String selectedOwnerId) {
        GovernanceWorkspaceSessionEntity entity = findEntity(idStr);
        if (focusMode != null) entity.setFocusMode(focusMode);
        if (selectedProjectId != null) entity.setSelectedProjectId(parseLong(selectedProjectId));
        if (selectedRecommendationId != null) entity.setSelectedRecommendationId(parseLong(selectedRecommendationId));
        if (selectedOwnerId != null) entity.setSelectedOwnerId(parseLong(selectedOwnerId));
        entity.setUpdateTime(LocalDateTime.now());
        workspaceSessionMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional
    public GovernanceWorkspaceSessionResponse updateSessionStatus(String idStr, String newStatus) {
        GovernanceWorkspaceSessionEntity entity = findEntity(idStr);
        String current = entity.getSessionStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid session transition from " + current + " to " + newStatus);
        }
        entity.setSessionStatus(newStatus);
        if ("COMPLETED".equals(newStatus) || "ARCHIVED".equals(newStatus)) {
            entity.setEndedAt(LocalDateTime.now());
        }
        entity.setUpdateTime(LocalDateTime.now());
        workspaceSessionMapper.updateById(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public GovernanceWorkspaceSessionResponse getActiveSession() {
        LambdaQueryWrapper<GovernanceWorkspaceSessionEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceWorkspaceSessionEntity::getSessionStatus, "ACTIVE").last("LIMIT 1");
        GovernanceWorkspaceSessionEntity entity = workspaceSessionMapper.selectOne(w);
        if (entity == null) {
            // Create default active session
            return createSession("Admin", "PRIORITY_FIRST");
        }
        return toResponse(entity);
    }

    private boolean isValidTransition(String current, String next) {
        if ("ACTIVE".equals(current) && ("PAUSED".equals(next) || "COMPLETED".equals(next))) return true;
        if ("PAUSED".equals(current) && "ACTIVE".equals(next)) return true;
        if ("COMPLETED".equals(current) && "ARCHIVED".equals(next)) return true;
        return false;
    }

    private GovernanceWorkspaceSessionEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceWorkspaceSessionEntity entity = workspaceSessionMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "Session 不存在");
        return entity;
    }

    private GovernanceWorkspaceSessionResponse toResponse(GovernanceWorkspaceSessionEntity e) {
        GovernanceWorkspaceSessionResponse r = new GovernanceWorkspaceSessionResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setSessionStatus(e.getSessionStatus());
        r.setFocusMode(e.getFocusMode());
        r.setSelectedProjectId(e.getSelectedProjectId() != null ? e.getSelectedProjectId().toString() : null);
        r.setSelectedRecommendationId(e.getSelectedRecommendationId() != null ? e.getSelectedRecommendationId().toString() : null);
        r.setSelectedOwnerId(e.getSelectedOwnerId() != null ? e.getSelectedOwnerId().toString() : null);
        r.setContextSummary(e.getContextSummary()); r.setStartedAt(e.getStartedAt());
        r.setEndedAt(e.getEndedAt()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { throw new BizException(ErrorCode.BAD_REQUEST, "ID 格式无效"); } }
}
