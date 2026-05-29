package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaFeedbackTriageStatus;
import com.aicoding.platform.orchestration.domain.BetaTrialFeedbackEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialSessionEntity;
import com.aicoding.platform.orchestration.dto.BetaPassBlockSummaryResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialFeedbackResponse;
import com.aicoding.platform.orchestration.dto.BetaTrialFeedbackSummaryResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaTrialFeedbackRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaTrialFeedbackRequest;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialFeedbackMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaTrialFeedbackService {

    private final BetaTrialFeedbackMapper betaTrialFeedbackMapper;
    private final BetaTrialSessionMapper betaTrialSessionMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaTrialFeedbackService(BetaTrialFeedbackMapper betaTrialFeedbackMapper,
                                    BetaTrialSessionMapper betaTrialSessionMapper,
                                    ProjectPermissionService projectPermissionService) {
        this.betaTrialFeedbackMapper = betaTrialFeedbackMapper;
        this.betaTrialSessionMapper = betaTrialSessionMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public BetaTrialFeedbackResponse createFeedback(String sessionIdStr, CreateBetaTrialFeedbackRequest request) {
        Long sessionId = parseLong(sessionIdStr, "sessionId");
        BetaTrialSessionEntity session = betaTrialSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用会话不存在");
        }
        projectPermissionService.checkProjectRole(session.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        BetaTrialFeedbackEntity entity = new BetaTrialFeedbackEntity();
        entity.setSessionId(sessionId);
        entity.setProjectId(session.getProjectId());
        entity.setCategory(request.getCategory());
        entity.setSubcategory(request.getSubcategory());
        entity.setSeverity(request.getSeverity());
        entity.setSourceType(request.getSourceType());
        entity.setTitle(request.getTitle());
        entity.setDetail(request.getDetail());
        entity.setExpectedBehavior(request.getExpectedBehavior());
        entity.setActualBehavior(request.getActualBehavior());
        entity.setSuggestedAction(request.getSuggestedAction());
        entity.setReleaseBlocking(request.getReleaseBlocking() != null && request.getReleaseBlocking());
        entity.setTriageStatus(BetaFeedbackTriageStatus.NEW.name());
        betaTrialFeedbackMapper.insert(entity);

        return toFeedbackResponse(entity);
    }

    @Transactional
    public BetaTrialFeedbackResponse updateFeedback(String id, UpdateBetaTrialFeedbackRequest request) {
        Long feedbackId = parseLong(id, "id");
        BetaTrialFeedbackEntity entity = betaTrialFeedbackMapper.selectById(feedbackId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用反馈不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getSubcategory() != null) {
            entity.setSubcategory(request.getSubcategory());
        }
        if (request.getSeverity() != null) {
            entity.setSeverity(request.getSeverity());
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDetail() != null) {
            entity.setDetail(request.getDetail());
        }
        if (request.getExpectedBehavior() != null) {
            entity.setExpectedBehavior(request.getExpectedBehavior());
        }
        if (request.getActualBehavior() != null) {
            entity.setActualBehavior(request.getActualBehavior());
        }
        if (request.getSuggestedAction() != null) {
            entity.setSuggestedAction(request.getSuggestedAction());
        }
        if (request.getTriageStatus() != null) {
            entity.setTriageStatus(request.getTriageStatus());
        }
        if (request.getMappedIncidentId() != null) {
            entity.setMappedIncidentId(parseLong(request.getMappedIncidentId(), "mappedIncidentId"));
        }
        if (request.getMappedKnownIssueId() != null) {
            entity.setMappedKnownIssueId(parseLong(request.getMappedKnownIssueId(), "mappedKnownIssueId"));
        }
        if (request.getReleaseBlocking() != null) {
            entity.setReleaseBlocking(request.getReleaseBlocking());
        }

        betaTrialFeedbackMapper.updateById(entity);
        return toFeedbackResponse(entity);
    }

    @Transactional(readOnly = true)
    public BetaTrialFeedbackResponse getFeedback(String id) {
        Long feedbackId = parseLong(id, "id");
        BetaTrialFeedbackEntity entity = betaTrialFeedbackMapper.selectById(feedbackId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用反馈不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toFeedbackResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BetaTrialFeedbackSummaryResponse> listFeedback(String sessionIdStr,
                                                                String severity,
                                                                String triageStatus) {
        Long sessionId = parseLong(sessionIdStr, "sessionId");
        BetaTrialSessionEntity session = betaTrialSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用会话不存在");
        }
        projectPermissionService.checkProjectMember(session.getProjectId());

        LambdaQueryWrapper<BetaTrialFeedbackEntity> query = new LambdaQueryWrapper<>();
        query.eq(BetaTrialFeedbackEntity::getSessionId, sessionId);
        if (severity != null && !severity.isBlank()) {
            query.eq(BetaTrialFeedbackEntity::getSeverity, severity);
        }
        if (triageStatus != null && !triageStatus.isBlank()) {
            query.eq(BetaTrialFeedbackEntity::getTriageStatus, triageStatus);
        }
        query.orderByDesc(BetaTrialFeedbackEntity::getCreateTime);

        return betaTrialFeedbackMapper.selectList(query).stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetaPassBlockSummaryResponse getPassBlockSummary(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr, "sessionId");
        List<BetaTrialFeedbackEntity> all = betaTrialFeedbackMapper.selectList(
                new LambdaQueryWrapper<BetaTrialFeedbackEntity>()
                        .eq(BetaTrialFeedbackEntity::getSessionId, sessionId));

        BetaPassBlockSummaryResponse resp = new BetaPassBlockSummaryResponse();
        resp.setTotalFeedback((long) all.size());

        long releaseBlocking = all.stream().filter(f -> f.getReleaseBlocking() != null && f.getReleaseBlocking()).count();
        resp.setReleaseBlockingCount(releaseBlocking);

        long p0 = all.stream().filter(f -> "P0".equals(f.getSeverity())).count();
        long p1 = all.stream().filter(f -> "P1".equals(f.getSeverity())).count();
        resp.setP0Count(p0);
        resp.setP1Count(p1);

        long newCount = all.stream().filter(f -> BetaFeedbackTriageStatus.NEW.name().equals(f.getTriageStatus())).count();
        long triagedCount = all.stream().filter(f -> BetaFeedbackTriageStatus.TRIAGED.name().equals(f.getTriageStatus())).count();
        long scheduledCount = all.stream().filter(f -> BetaFeedbackTriageStatus.SCHEDULED.name().equals(f.getTriageStatus())).count();
        long doneCount = all.stream().filter(f -> BetaFeedbackTriageStatus.DONE.name().equals(f.getTriageStatus())).count();
        long wontFixCount = all.stream().filter(f -> BetaFeedbackTriageStatus.WONT_FIX.name().equals(f.getTriageStatus())).count();

        resp.setNewCount(newCount);
        resp.setTriagedCount(triagedCount);
        resp.setScheduledCount(scheduledCount);
        resp.setDoneCount(doneCount);
        resp.setWontFixCount(wontFixCount);

        return resp;
    }

    @Transactional
    public void deleteFeedback(String id) {
        Long feedbackId = parseLong(id, "id");
        BetaTrialFeedbackEntity entity = betaTrialFeedbackMapper.selectById(feedbackId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Beta 试用反馈不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(),
                ProjectRole.OWNER, ProjectRole.MAINTAINER);
        betaTrialFeedbackMapper.deleteById(feedbackId);
    }

    private BetaTrialFeedbackResponse toFeedbackResponse(BetaTrialFeedbackEntity entity) {
        BetaTrialFeedbackResponse resp = new BetaTrialFeedbackResponse();
        resp.setId(entity.getId().toString());
        resp.setSessionId(entity.getSessionId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setCategory(entity.getCategory());
        resp.setSubcategory(entity.getSubcategory());
        resp.setSeverity(entity.getSeverity());
        resp.setSourceType(entity.getSourceType());
        resp.setTitle(entity.getTitle());
        resp.setDetail(entity.getDetail());
        resp.setExpectedBehavior(entity.getExpectedBehavior());
        resp.setActualBehavior(entity.getActualBehavior());
        resp.setSuggestedAction(entity.getSuggestedAction());
        resp.setTriageStatus(entity.getTriageStatus());
        resp.setMappedIncidentId(entity.getMappedIncidentId() != null ? entity.getMappedIncidentId().toString() : null);
        resp.setMappedKnownIssueId(entity.getMappedKnownIssueId() != null ? entity.getMappedKnownIssueId().toString() : null);
        resp.setReleaseBlocking(entity.getReleaseBlocking());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private BetaTrialFeedbackSummaryResponse toSummaryResponse(BetaTrialFeedbackEntity entity) {
        BetaTrialFeedbackSummaryResponse resp = new BetaTrialFeedbackSummaryResponse();
        resp.setId(entity.getId().toString());
        resp.setSessionId(entity.getSessionId().toString());
        resp.setCategory(entity.getCategory());
        resp.setSeverity(entity.getSeverity());
        resp.setTitle(entity.getTitle());
        resp.setTriageStatus(entity.getTriageStatus());
        resp.setReleaseBlocking(entity.getReleaseBlocking());
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
