package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaEnvironmentReadinessEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialFeedbackEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialSessionEntity;
import com.aicoding.platform.orchestration.dto.BetaEnvironmentReadinessResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialDashboardResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaEnvironmentReadinessRequest;
import com.aicoding.platform.orchestration.infrastructure.BetaEnvironmentReadinessMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialFeedbackMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaEnvironmentReadinessService {

    private final BetaEnvironmentReadinessMapper betaEnvironmentReadinessMapper;
    private final BetaTrialSessionMapper betaTrialSessionMapper;
    private final BetaTrialFeedbackMapper betaTrialFeedbackMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaEnvironmentReadinessService(BetaEnvironmentReadinessMapper betaEnvironmentReadinessMapper,
                                           BetaTrialSessionMapper betaTrialSessionMapper,
                                           BetaTrialFeedbackMapper betaTrialFeedbackMapper,
                                           ProjectPermissionService projectPermissionService) {
        this.betaEnvironmentReadinessMapper = betaEnvironmentReadinessMapper;
        this.betaTrialSessionMapper = betaTrialSessionMapper;
        this.betaTrialFeedbackMapper = betaTrialFeedbackMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public BetaEnvironmentReadinessResponse createCheck(CreateBetaEnvironmentReadinessRequest request) {
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId,
                ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        BetaEnvironmentReadinessEntity entity = new BetaEnvironmentReadinessEntity();
        entity.setProjectId(projectId);
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            entity.setSessionId(parseLong(request.getSessionId(), "sessionId"));
        }
        entity.setTargetName(request.getTargetName());
        entity.setTargetType(request.getTargetType());
        entity.setCheckStatus(request.getCheckStatus());
        entity.setSummary(request.getSummary());
        entity.setDetailJson(request.getDetailJson());
        entity.setCheckedAt(LocalDateTime.now());
        betaEnvironmentReadinessMapper.insert(entity);

        return toReadinessResponse(entity);
    }

    @Transactional(readOnly = true)
    public BetaEnvironmentReadinessResponse getCheck(String id) {
        Long checkId = parseLong(id, "id");
        BetaEnvironmentReadinessEntity entity = betaEnvironmentReadinessMapper.selectById(checkId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "环境就绪检查记录不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toReadinessResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BetaEnvironmentReadinessResponse> listChecks(String projectIdStr, String sessionIdStr) {
        if (projectIdStr == null && sessionIdStr == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "projectId 或 sessionId 至少提供一个");
        }

        LambdaQueryWrapper<BetaEnvironmentReadinessEntity> query = new LambdaQueryWrapper<>();
        if (projectIdStr != null && !projectIdStr.isBlank()) {
            Long projectId = parseLong(projectIdStr, "projectId");
            projectPermissionService.checkProjectMember(projectId);
            query.eq(BetaEnvironmentReadinessEntity::getProjectId, projectId);
        }
        if (sessionIdStr != null && !sessionIdStr.isBlank()) {
            Long sessionId = parseLong(sessionIdStr, "sessionId");
            query.eq(BetaEnvironmentReadinessEntity::getSessionId, sessionId);
        }
        query.orderByDesc(BetaEnvironmentReadinessEntity::getCheckedAt);

        return betaEnvironmentReadinessMapper.selectList(query).stream()
                .map(this::toReadinessResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetaTrialDashboardResponse getDashboard(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        List<BetaTrialSessionEntity> sessions = betaTrialSessionMapper.selectList(
                new LambdaQueryWrapper<BetaTrialSessionEntity>()
                        .eq(BetaTrialSessionEntity::getProjectId, projectId));

        List<BetaTrialFeedbackEntity> allFeedback = betaTrialFeedbackMapper.selectList(
                new LambdaQueryWrapper<BetaTrialFeedbackEntity>()
                        .eq(BetaTrialFeedbackEntity::getProjectId, projectId));

        List<BetaEnvironmentReadinessEntity> readiness = betaEnvironmentReadinessMapper.selectList(
                new LambdaQueryWrapper<BetaEnvironmentReadinessEntity>()
                        .eq(BetaEnvironmentReadinessEntity::getProjectId, projectId));

        BetaTrialDashboardResponse resp = new BetaTrialDashboardResponse();
        resp.setTotalSessions((long) sessions.size());
        resp.setCompletedSessions(sessions.stream()
                .filter(s -> "COMPLETED".equals(s.getSessionStatus())).count());
        resp.setBlockedSessions(sessions.stream()
                .filter(s -> "BLOCKED".equals(s.getSessionStatus())).count());
        resp.setInProgressSessions(sessions.stream()
                .filter(s -> "IN_PROGRESS".equals(s.getSessionStatus())).count());

        // Average satisfaction score
        List<BetaTrialSessionEntity> scored = sessions.stream()
                .filter(s -> s.getSatisfactionScore() != null).toList();
        if (!scored.isEmpty()) {
            double avg = scored.stream()
                    .mapToInt(BetaTrialSessionEntity::getSatisfactionScore)
                    .average().orElse(0.0);
            resp.setAverageSatisfactionScore(Math.round(avg * 100.0) / 100.0);
        } else {
            resp.setAverageSatisfactionScore(0.0);
        }

        resp.setContinueYesCount(sessions.stream()
                .filter(s -> "YES".equals(s.getContinueIntent())).count());
        resp.setP0Count(allFeedback.stream()
                .filter(f -> "P0".equals(f.getSeverity())).count());
        resp.setP1Count(allFeedback.stream()
                .filter(f -> "P1".equals(f.getSeverity())).count());
        resp.setReleaseBlockingCount(allFeedback.stream()
                .filter(f -> f.getReleaseBlocking() != null && f.getReleaseBlocking()).count());

        resp.setReadinessPassCount(readiness.stream()
                .filter(r -> "PASS".equals(r.getCheckStatus())).count());
        resp.setReadinessWarnCount(readiness.stream()
                .filter(r -> "WARN".equals(r.getCheckStatus())).count());
        resp.setReadinessFailCount(readiness.stream()
                .filter(r -> "FAIL".equals(r.getCheckStatus())).count());

        return resp;
    }

    private BetaEnvironmentReadinessResponse toReadinessResponse(BetaEnvironmentReadinessEntity entity) {
        BetaEnvironmentReadinessResponse resp = new BetaEnvironmentReadinessResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setSessionId(entity.getSessionId() != null ? entity.getSessionId().toString() : null);
        resp.setTargetName(entity.getTargetName());
        resp.setTargetType(entity.getTargetType());
        resp.setCheckStatus(entity.getCheckStatus());
        resp.setSummary(entity.getSummary());
        resp.setDetailJson(entity.getDetailJson());
        resp.setCheckedAt(entity.getCheckedAt());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
