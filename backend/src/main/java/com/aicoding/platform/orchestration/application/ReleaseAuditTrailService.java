package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.ReleaseAuditEventEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRollbackDrillEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutStepEntity;
import com.aicoding.platform.orchestration.domain.ReleaseVerificationRecordEntity;
import com.aicoding.platform.orchestration.domain.ReleasePostmortemReviewEntity;
import com.aicoding.platform.orchestration.dto.ReleaseAuditEventResponse;
import com.aicoding.platform.orchestration.dto.ReleaseAuditReportResponse;
import com.aicoding.platform.orchestration.dto.ReleaseAuditTimelineResponse;
import com.aicoding.platform.orchestration.infrastructure.ReleaseAuditEventMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRollbackDrillMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutStepMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseVerificationRecordMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleasePostmortemReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReleaseAuditTrailService {

    private final ReleaseAuditEventMapper releaseAuditEventMapper;
    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final ReleaseRollbackDrillMapper releaseRollbackDrillMapper;
    private final ReleasePostmortemReviewMapper releasePostmortemReviewMapper;

    public ReleaseAuditTrailService(ReleaseAuditEventMapper releaseAuditEventMapper,
                                    ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                    ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                    ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                    ReleaseRollbackDrillMapper releaseRollbackDrillMapper,
                                    ReleasePostmortemReviewMapper releasePostmortemReviewMapper) {
        this.releaseAuditEventMapper = releaseAuditEventMapper;
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.releaseRollbackDrillMapper = releaseRollbackDrillMapper;
        this.releasePostmortemReviewMapper = releasePostmortemReviewMapper;
    }

    @Transactional
    public void recordEvent(Long projectId, Long planId, String releaseLabel,
                            String eventType, Long actorId, String actorName,
                            String summary, String detail) {
        ReleaseAuditEventEntity entity = new ReleaseAuditEventEntity();
        entity.setProjectId(projectId);
        entity.setPlanId(planId);
        entity.setReleaseLabel(releaseLabel);
        entity.setEventType(eventType);
        entity.setActorId(actorId);
        entity.setActorName(actorName);
        entity.setSummary(summary != null ? summary : eventType);
        entity.setDetail(detail);
        entity.setEventTime(LocalDateTime.now());
        releaseAuditEventMapper.insert(entity);
    }

    @Transactional(readOnly = true)
    public List<ReleaseAuditEventResponse> listEvents(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        LambdaQueryWrapper<ReleaseAuditEventEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseAuditEventEntity::getPlanId, planId);
        wrapper.orderByDesc(ReleaseAuditEventEntity::getEventTime);
        return releaseAuditEventMapper.selectList(wrapper).stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseAuditTimelineResponse getTimeline(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        LambdaQueryWrapper<ReleaseAuditEventEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseAuditEventEntity::getPlanId, planId);
        wrapper.orderByDesc(ReleaseAuditEventEntity::getEventTime);
        List<ReleaseAuditEventEntity> events = releaseAuditEventMapper.selectList(wrapper);

        ReleaseAuditTimelineResponse resp = new ReleaseAuditTimelineResponse();
        resp.setPlanId(plan.getId().toString());
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setTotalEvents(events.size());
        resp.setLatestEventTime(events.isEmpty() ? null : events.get(0).getEventTime());

        Map<String, Integer> countsByType = new HashMap<>();
        for (ReleaseAuditEventEntity e : events) {
            countsByType.merge(e.getEventType(), 1, (a, b) -> a + b);
        }
        resp.setEventCountsByType(countsByType);
        resp.setEvents(events.stream().map(this::toEventResponse).collect(Collectors.toList()));

        return resp;
    }

    @Transactional(readOnly = true)
    public ReleaseAuditReportResponse generateAuditReport(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        ReleaseAuditReportResponse resp = new ReleaseAuditReportResponse();
        resp.setPlanId(plan.getId().toString());
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setGeneratedAt(LocalDateTime.now());

        // Load all audit events
        LambdaQueryWrapper<ReleaseAuditEventEntity> eventWrapper = new LambdaQueryWrapper<>();
        eventWrapper.eq(ReleaseAuditEventEntity::getPlanId, planId);
        eventWrapper.orderByAsc(ReleaseAuditEventEntity::getEventTime);
        List<ReleaseAuditEventEntity> events = releaseAuditEventMapper.selectList(eventWrapper);

        // Load steps
        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        stepWrapper.orderByAsc(ReleaseRolloutStepEntity::getStepOrder);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);

        // Load verifications
        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        verWrapper.orderByAsc(ReleaseVerificationRecordEntity::getRecordedAt);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);

        // Load drills
        LambdaQueryWrapper<ReleaseRollbackDrillEntity> drillWrapper = new LambdaQueryWrapper<>();
        drillWrapper.eq(ReleaseRollbackDrillEntity::getPlanId, planId);
        List<ReleaseRollbackDrillEntity> drills = releaseRollbackDrillMapper.selectList(drillWrapper);

        // Load postmortem reviews
        LambdaQueryWrapper<ReleasePostmortemReviewEntity> pmWrapper = new LambdaQueryWrapper<>();
        pmWrapper.eq(ReleasePostmortemReviewEntity::getPlanId, planId);
        List<ReleasePostmortemReviewEntity> reviews = releasePostmortemReviewMapper.selectList(pmWrapper);

        // Generate markdown report
        StringBuilder md = new StringBuilder();
        md.append("# Release Audit Report\n\n");
        md.append("**Release**: ").append(plan.getReleaseLabel()).append("\n\n");
        md.append("**Status**: ").append(plan.getRolloutStatus()).append("\n\n");
        md.append("**Strategy**: ").append(plan.getRolloutStrategy()).append("\n\n");
        md.append("**Target Environment**: ").append(plan.getTargetEnvironment()).append("\n\n");
        md.append("**Generated At**: ").append(LocalDateTime.now()).append("\n\n");
        md.append("---\n\n");

        md.append("## Rollout Timeline\n\n");
        md.append("| Time | Event | Summary | Actor |\n");
        md.append("|---|---|---|---|\n");
        for (ReleaseAuditEventEntity e : events) {
            md.append("| ").append(e.getEventTime()).append(" | ")
                    .append(e.getEventType()).append(" | ")
                    .append(e.getSummary() != null ? e.getSummary() : "-").append(" | ")
                    .append(e.getActorName() != null ? e.getActorName() : "-").append(" |\n");
        }

        md.append("\n## Rollout Steps\n\n");
        md.append("| # | Step | Status | Result |\n");
        md.append("|---|---|---|---|\n");
        for (ReleaseRolloutStepEntity s : steps) {
            md.append("| ").append(s.getStepOrder()).append(" | ")
                    .append(s.getDisplayName()).append(" | ")
                    .append(s.getStepStatus()).append(" | ")
                    .append(s.getActualResult() != null ? s.getActualResult() : "-").append(" |\n");
        }

        md.append("\n## Verification Results\n\n");
        md.append("| Verification | Status | Severity |\n");
        md.append("|---|---|---|\n");
        for (ReleaseVerificationRecordEntity v : verifications) {
            md.append("| ").append(v.getDisplayName()).append(" | ")
                    .append(v.getVerificationStatus()).append(" | ")
                    .append(v.getSeverity() != null ? v.getSeverity() : "-").append(" |\n");
        }

        md.append("\n## Rollback Drills\n\n");
        if (drills.isEmpty()) {
            md.append("No rollback drills recorded.\n\n");
        } else {
            md.append("| Drill | Status | Scope | Duration |\n");
            md.append("|---|---|---|---|\n");
            for (ReleaseRollbackDrillEntity d : drills) {
                md.append("| ").append(d.getReleaseLabel()).append(" | ")
                        .append(d.getDrillStatus()).append(" | ")
                        .append(d.getDrillScope()).append(" | ")
                        .append(d.getDurationSeconds() != null ? d.getDurationSeconds() + "s" : "-").append(" |\n");
            }
        }

        md.append("\n## Post-release Reviews\n\n");
        if (reviews.isEmpty()) {
            md.append("No post-release reviews recorded.\n\n");
        } else {
            md.append("| Review | Status | Outcome |\n");
            md.append("|---|---|---|\n");
            for (ReleasePostmortemReviewEntity r : reviews) {
                md.append("| ").append(r.getReleaseLabel()).append(" | ")
                        .append(r.getReviewStatus()).append(" | ")
                        .append(r.getOverallOutcome()).append(" |\n");
            }
        }

        resp.setReportMarkdown(md.toString());
        return resp;
    }

    private ReleaseAuditEventResponse toEventResponse(ReleaseAuditEventEntity entity) {
        ReleaseAuditEventResponse resp = new ReleaseAuditEventResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setEventType(entity.getEventType());
        resp.setActorId(entity.getActorId() != null ? entity.getActorId().toString() : null);
        resp.setActorName(entity.getActorName());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setRelatedStepId(entity.getRelatedStepId() != null ? entity.getRelatedStepId().toString() : null);
        resp.setRelatedVerificationId(entity.getRelatedVerificationId() != null ? entity.getRelatedVerificationId().toString() : null);
        resp.setRelatedIncidentId(entity.getRelatedIncidentId() != null ? entity.getRelatedIncidentId().toString() : null);
        resp.setRelatedAlertId(entity.getRelatedAlertId() != null ? entity.getRelatedAlertId().toString() : null);
        resp.setEvidenceJson(entity.getEvidenceJson());
        resp.setEventTime(entity.getEventTime());
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
