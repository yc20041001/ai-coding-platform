package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaTrialFeedbackEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialSessionEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialSessionStatus;
import com.aicoding.platform.orchestration.dto.BetaTrialSessionResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialSessionSummaryResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaTrialSessionRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaTrialSessionRequest;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialFeedbackMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaTrialSessionService {

    private final BetaTrialSessionMapper betaTrialSessionMapper;
    private final BetaTrialFeedbackMapper betaTrialFeedbackMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaTrialSessionService(BetaTrialSessionMapper betaTrialSessionMapper,
                                   BetaTrialFeedbackMapper betaTrialFeedbackMapper,
                                   ProjectPermissionService projectPermissionService) {
        this.betaTrialSessionMapper = betaTrialSessionMapper;
        this.betaTrialFeedbackMapper = betaTrialFeedbackMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public BetaTrialSessionResponse createSession(CreateBetaTrialSessionRequest request) {
        Long projectId = parseLong(request.getProjectId(), "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        BetaTrialSessionEntity entity = new BetaTrialSessionEntity();
        entity.setProjectId(projectId);
        entity.setTitle(request.getTitle());
        entity.setParticipantRole(request.getParticipantRole());
        entity.setEnvironmentType(request.getEnvironmentType());
        entity.setProviderMode(request.getProviderMode());
        entity.setGithubOauthStatus(request.getGithubOauthStatus());
        entity.setSessionStatus(BetaTrialSessionStatus.PLANNED.name());
        entity.setStartedAt(LocalDateTime.now());
        betaTrialSessionMapper.insert(entity);

        return toSessionResponse(entity);
    }

    @Transactional
    public BetaTrialSessionResponse updateSession(String id, UpdateBetaTrialSessionRequest request) {
        Long sessionId = parseLong(id, "id");
        BetaTrialSessionEntity entity = betaTrialSessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用会话不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        String newStatus = request.getSessionStatus();
        if (newStatus != null && !newStatus.isBlank()) {
            validateStatusTransition(entity.getSessionStatus(), newStatus);
            entity.setSessionStatus(newStatus);
            if (BetaTrialSessionStatus.COMPLETED.name().equals(newStatus)
                    || BetaTrialSessionStatus.CANCELED.name().equals(newStatus)) {
                entity.setEndedAt(LocalDateTime.now());
            }
        }

        if (request.getBlockedAtStep() != null) {
            entity.setBlockedAtStep(request.getBlockedAtStep());
        }
        if (request.getBlockerSummary() != null) {
            entity.setBlockerSummary(request.getBlockerSummary());
        }
        if (request.getCompletedPathJson() != null) {
            entity.setCompletedPathJson(request.getCompletedPathJson());
        }
        if (request.getSatisfactionScore() != null) {
            entity.setSatisfactionScore(request.getSatisfactionScore());
        }
        if (request.getContinueIntent() != null) {
            entity.setContinueIntent(request.getContinueIntent());
        }
        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary());
        }
        if (request.getStartedAt() != null && !request.getStartedAt().isBlank()) {
            entity.setStartedAt(LocalDateTime.parse(request.getStartedAt()));
        }
        if (request.getEndedAt() != null && !request.getEndedAt().isBlank()) {
            entity.setEndedAt(LocalDateTime.parse(request.getEndedAt()));
        }

        betaTrialSessionMapper.updateById(entity);
        return toSessionResponse(entity);
    }

    @Transactional(readOnly = true)
    public BetaTrialSessionResponse getSession(String id) {
        Long sessionId = parseLong(id, "id");
        BetaTrialSessionEntity entity = betaTrialSessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用会话不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toSessionResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BetaTrialSessionSummaryResponse> listSessions(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        List<BetaTrialSessionEntity> entities = betaTrialSessionMapper.selectList(
                new LambdaQueryWrapper<BetaTrialSessionEntity>()
                        .eq(BetaTrialSessionEntity::getProjectId, projectId)
                        .orderByDesc(BetaTrialSessionEntity::getCreateTime));

        return entities.stream().map(this::toSummaryResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportSessionMarkdown(String id) {
        Long sessionId = parseLong(id, "id");
        BetaTrialSessionEntity entity = betaTrialSessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用会话不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());

        List<BetaTrialFeedbackEntity> feedbacks = betaTrialFeedbackMapper.selectList(
                new LambdaQueryWrapper<BetaTrialFeedbackEntity>()
                        .eq(BetaTrialFeedbackEntity::getSessionId, sessionId));

        StringBuilder sb = new StringBuilder();
        sb.append("# Beta 试用报告: ").append(entity.getTitle()).append("\n\n");
        sb.append("- **状态**: ").append(entity.getSessionStatus()).append("\n");
        sb.append("- **参与角色**: ").append(entity.getParticipantRole()).append("\n");
        sb.append("- **环境类型**: ").append(entity.getEnvironmentType()).append("\n");
        sb.append("- **供应商模式**: ").append(entity.getProviderMode()).append("\n");
        sb.append("- **GitHub OAuth**: ").append(entity.getGithubOauthStatus()).append("\n");
        if (entity.getStartedAt() != null) {
            sb.append("- **开始时间**: ").append(entity.getStartedAt()).append("\n");
        }
        if (entity.getEndedAt() != null) {
            sb.append("- **结束时间**: ").append(entity.getEndedAt()).append("\n");
        }
        if (entity.getSatisfactionScore() != null) {
            sb.append("- **满意度评分**: ").append(entity.getSatisfactionScore()).append("/10\n");
        }
        if (entity.getContinueIntent() != null) {
            sb.append("- **继续意向**: ").append(entity.getContinueIntent()).append("\n");
        }
        if (entity.getSummary() != null && !entity.getSummary().isBlank()) {
            sb.append("\n## 总结\n\n").append(entity.getSummary()).append("\n");
        }
        if (entity.getBlockerSummary() != null && !entity.getBlockerSummary().isBlank()) {
            sb.append("\n## 阻塞点\n\n").append(entity.getBlockerSummary()).append("\n");
        }

        if (!feedbacks.isEmpty()) {
            sb.append("\n## 反馈列表\n\n");
            for (BetaTrialFeedbackEntity fb : feedbacks) {
                sb.append("- **[").append(fb.getSeverity()).append("]** ");
                sb.append(fb.getTitle());
                if (fb.getCategory() != null) {
                    sb.append(" (").append(fb.getCategory()).append(")");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private void validateStatusTransition(String current, String next) {
        if (current.equals(next)) {
            return;
        }
        boolean valid = switch (BetaTrialSessionStatus.valueOf(current)) {
            case PLANNED -> next.equals(BetaTrialSessionStatus.IN_PROGRESS.name())
                    || next.equals(BetaTrialSessionStatus.CANCELED.name());
            case IN_PROGRESS -> next.equals(BetaTrialSessionStatus.COMPLETED.name())
                    || next.equals(BetaTrialSessionStatus.BLOCKED.name());
            case BLOCKED -> next.equals(BetaTrialSessionStatus.IN_PROGRESS.name());
            case COMPLETED, CANCELED -> false;
        };
        if (!valid) {
            throw new BizException(ErrorCode.CONFLICT,
                    "无效的状态转换: " + current + " → " + next);
        }
    }

    private BetaTrialSessionResponse toSessionResponse(BetaTrialSessionEntity entity) {
        BetaTrialSessionResponse resp = new BetaTrialSessionResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTitle(entity.getTitle());
        resp.setParticipantRole(entity.getParticipantRole());
        resp.setEnvironmentType(entity.getEnvironmentType());
        resp.setProviderMode(entity.getProviderMode());
        resp.setGithubOauthStatus(entity.getGithubOauthStatus());
        resp.setSessionStatus(entity.getSessionStatus());
        resp.setStartedAt(entity.getStartedAt());
        resp.setEndedAt(entity.getEndedAt());
        resp.setCompletedPathJson(entity.getCompletedPathJson());
        resp.setBlockedAtStep(entity.getBlockedAtStep());
        resp.setBlockerSummary(entity.getBlockerSummary());
        resp.setSatisfactionScore(entity.getSatisfactionScore());
        resp.setContinueIntent(entity.getContinueIntent());
        resp.setSummary(entity.getSummary());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private BetaTrialSessionSummaryResponse toSummaryResponse(BetaTrialSessionEntity entity) {
        BetaTrialSessionSummaryResponse resp = new BetaTrialSessionSummaryResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setTitle(entity.getTitle());
        resp.setParticipantRole(entity.getParticipantRole());
        resp.setEnvironmentType(entity.getEnvironmentType());
        resp.setProviderMode(entity.getProviderMode());
        resp.setSessionStatus(entity.getSessionStatus());
        resp.setContinueIntent(entity.getContinueIntent());
        resp.setSatisfactionScore(entity.getSatisfactionScore());
        resp.setStartedAt(entity.getStartedAt());
        resp.setEndedAt(entity.getEndedAt());
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
