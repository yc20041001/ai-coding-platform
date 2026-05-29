package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.*;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReleaseExecutiveSummaryService {

    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final ReleaseRollbackDrillMapper releaseRollbackDrillMapper;
    private final ReleasePostmortemReviewMapper releasePostmortemReviewMapper;
    private final ReleaseSignoffRecordMapper releaseSignoffRecordMapper;
    private final ReleaseConfidenceSnapshotMapper releaseConfidenceSnapshotMapper;
    private final ReleaseSignoffService releaseSignoffService;

    public ReleaseExecutiveSummaryService(ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                           ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                           ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                           ReleaseRollbackDrillMapper releaseRollbackDrillMapper,
                                           ReleasePostmortemReviewMapper releasePostmortemReviewMapper,
                                           ReleaseSignoffRecordMapper releaseSignoffRecordMapper,
                                           ReleaseConfidenceSnapshotMapper releaseConfidenceSnapshotMapper,
                                           ReleaseSignoffService releaseSignoffService) {
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.releaseRollbackDrillMapper = releaseRollbackDrillMapper;
        this.releasePostmortemReviewMapper = releasePostmortemReviewMapper;
        this.releaseSignoffRecordMapper = releaseSignoffRecordMapper;
        this.releaseConfidenceSnapshotMapper = releaseConfidenceSnapshotMapper;
        this.releaseSignoffService = releaseSignoffService;
    }

    @Transactional(readOnly = true)
    public ReleaseExecutiveSummaryResponse getExecutiveSummary(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new com.aicoding.platform.common.exception.BizException(
                    com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Gather data
        int blockingIssues = 0;
        int warningIssues = 0;
        int openIncidents = 0;
        int activeAlerts = 0;
        int failedVerifications;
        boolean rollbackReady;

        // Steps
        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);
        long failedSteps = steps.stream().filter(s -> "FAILED".equals(s.getStepStatus())).count();
        blockingIssues += (int) failedSteps;

        // Verifications
        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);
        failedVerifications = (int) verifications.stream().filter(v -> "FAILED".equals(v.getVerificationStatus())).count();

        // Rollback readiness
        LambdaQueryWrapper<ReleaseRollbackDrillEntity> drillWrapper = new LambdaQueryWrapper<>();
        drillWrapper.eq(ReleaseRollbackDrillEntity::getPlanId, planId);
        drillWrapper.orderByDesc(ReleaseRollbackDrillEntity::getCreateTime);
        drillWrapper.last("LIMIT 1");
        ReleaseRollbackDrillEntity latestDrill = releaseRollbackDrillMapper.selectOne(drillWrapper);
        rollbackReady = latestDrill != null && "PASSED".equals(latestDrill.getDrillStatus())
                && latestDrill.getRollbackStepsSummary() != null && !latestDrill.getRollbackStepsSummary().isBlank()
                && (latestDrill.getBlockersSummary() == null || latestDrill.getBlockersSummary().isBlank());

        // Postmortem
        LambdaQueryWrapper<ReleasePostmortemReviewEntity> pmWrapper = new LambdaQueryWrapper<>();
        pmWrapper.eq(ReleasePostmortemReviewEntity::getPlanId, planId);
        pmWrapper.orderByDesc(ReleasePostmortemReviewEntity::getCreateTime);
        pmWrapper.last("LIMIT 1");
        ReleasePostmortemReviewEntity latestReview = releasePostmortemReviewMapper.selectOne(pmWrapper);

        // Signoffs
        BigDecimal signoffCompletionRate = releaseSignoffService.calculateCompletionRate(planIdStr);

        // Count signoff rejected
        LambdaQueryWrapper<ReleaseSignoffRecordEntity> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.eq(ReleaseSignoffRecordEntity::getPlanId, planId);
        rejectedWrapper.eq(ReleaseSignoffRecordEntity::getSignoffStatus, "REJECTED");
        long rejectedCount = releaseSignoffRecordMapper.selectCount(rejectedWrapper);
        if (rejectedCount > 0) {
            blockingIssues += (int) rejectedCount;
        }

        // Calculate confidence score
        ReleaseConfidenceSnapshotResponse snapshot = calculateConfidence(
                blockingIssues, warningIssues, openIncidents, activeAlerts,
                failedVerifications, rollbackReady, signoffCompletionRate);

        String summaryText = buildSummaryText(plan, snapshot, latestReview);

        ReleaseExecutiveSummaryResponse resp = new ReleaseExecutiveSummaryResponse();
        resp.setProjectId(plan.getProjectId() != null ? plan.getProjectId().toString() : null);
        resp.setPlanId(plan.getId().toString());
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setDecisionStatus(null);
        resp.setRolloutStatus(plan.getRolloutStatus());
        resp.setOverallOutcome(latestReview != null ? latestReview.getOverallOutcome() : null);
        resp.setConfidenceScore(snapshot.getConfidenceScore());
        resp.setConfidenceLevel(snapshot.getConfidenceLevel());
        resp.setBlockingIssueCount(blockingIssues);
        resp.setWarningIssueCount(warningIssues);
        resp.setRollbackReady(rollbackReady);
        resp.setSignoffCompletionRate(signoffCompletionRate);
        resp.setOpenIncidentCount(openIncidents);
        resp.setActiveAlertCount(activeAlerts);
        resp.setFailedVerificationCount(failedVerifications);
        resp.setLatestPostmortemOutcome(latestReview != null ? latestReview.getOverallOutcome() : null);
        resp.setSummaryText(summaryText);
        resp.setLastUpdatedAt(LocalDateTime.now());

        return resp;
    }

    @Transactional(readOnly = true)
    public ReleaseConfidenceSnapshotResponse getConfidenceSnapshot(String planIdStr) {
        ReleaseExecutiveSummaryResponse summary = getExecutiveSummary(planIdStr);
        ReleaseConfidenceSnapshotResponse resp = new ReleaseConfidenceSnapshotResponse();
        resp.setPlanId(summary.getPlanId());
        resp.setReleaseLabel(summary.getReleaseLabel());
        resp.setConfidenceScore(summary.getConfidenceScore());
        resp.setConfidenceLevel(summary.getConfidenceLevel());
        resp.setBlockingIssueCount(summary.getBlockingIssueCount());
        resp.setWarningIssueCount(summary.getWarningIssueCount());
        resp.setOpenIncidentCount(summary.getOpenIncidentCount());
        resp.setActiveAlertCount(summary.getActiveAlertCount());
        resp.setFailedVerificationCount(summary.getFailedVerificationCount());
        resp.setRollbackReady(summary.getRollbackReady());
        resp.setSignoffCompletionRate(summary.getSignoffCompletionRate());
        resp.setSnapshotSummary(summary.getSummaryText() != null && summary.getSummaryText().length() > 255
                ? summary.getSummaryText().substring(0, 255) : summary.getSummaryText());
        resp.setSnapshotTime(LocalDateTime.now());
        return resp;
    }

    @Transactional(readOnly = true)
    public ReleaseComparisonResponse getComparison(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity currentPlan = releaseRolloutPlanMapper.selectById(planId);
        if (currentPlan == null) {
            throw new com.aicoding.platform.common.exception.BizException(
                    com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        // Find previous plan in same project
        LambdaQueryWrapper<ReleaseRolloutPlanEntity> prevWrapper = new LambdaQueryWrapper<>();
        prevWrapper.eq(ReleaseRolloutPlanEntity::getProjectId, currentPlan.getProjectId());
        prevWrapper.lt(ReleaseRolloutPlanEntity::getId, planId);
        prevWrapper.orderByDesc(ReleaseRolloutPlanEntity::getId);
        prevWrapper.last("LIMIT 1");
        ReleaseRolloutPlanEntity prevPlan = releaseRolloutPlanMapper.selectOne(prevWrapper);

        if (prevPlan == null) {
            ReleaseComparisonResponse empty = new ReleaseComparisonResponse();
            empty.setProjectId(currentPlan.getProjectId() != null ? currentPlan.getProjectId().toString() : null);
            empty.setCurrentReleaseLabel(currentPlan.getReleaseLabel());
            empty.setBaselineReleaseLabel(null);
            empty.setTrendSummary("No previous release data available for comparison.");
            return empty;
        }

        ReleaseExecutiveSummaryResponse currentSummary = getExecutiveSummary(planIdStr);
        ReleaseExecutiveSummaryResponse prevSummary = getExecutiveSummary(prevPlan.getId().toString());

        ReleaseComparisonResponse resp = new ReleaseComparisonResponse();
        resp.setProjectId(currentPlan.getProjectId() != null ? currentPlan.getProjectId().toString() : null);
        resp.setCurrentReleaseLabel(currentPlan.getReleaseLabel());
        resp.setBaselineReleaseLabel(prevPlan.getReleaseLabel());

        if (currentSummary.getConfidenceScore() != null && prevSummary.getConfidenceScore() != null) {
            resp.setConfidenceScoreDelta(currentSummary.getConfidenceScore().subtract(prevSummary.getConfidenceScore()));
        }
        if (currentSummary.getBlockingIssueCount() != null && prevSummary.getBlockingIssueCount() != null) {
            resp.setBlockingIssueDelta(currentSummary.getBlockingIssueCount() - prevSummary.getBlockingIssueCount());
        }
        if (currentSummary.getWarningIssueCount() != null && prevSummary.getWarningIssueCount() != null) {
            resp.setWarningIssueDelta(currentSummary.getWarningIssueCount() - prevSummary.getWarningIssueCount());
        }
        if (currentSummary.getFailedVerificationCount() != null && prevSummary.getFailedVerificationCount() != null) {
            resp.setFailedVerificationDelta(currentSummary.getFailedVerificationCount() - prevSummary.getFailedVerificationCount());
        }
        resp.setRollbackReadyChanged(!Objects.equals(currentSummary.getRollbackReady(), prevSummary.getRollbackReady()));
        if (currentSummary.getSignoffCompletionRate() != null && prevSummary.getSignoffCompletionRate() != null) {
            resp.setSignoffCompletionDelta(currentSummary.getSignoffCompletionRate().subtract(prevSummary.getSignoffCompletionRate()));
        }

        StringBuilder trend = new StringBuilder();
        if (resp.getConfidenceScoreDelta() != null) {
            if (resp.getConfidenceScoreDelta().compareTo(BigDecimal.ZERO) > 0) {
                trend.append("Confidence improved by ").append(resp.getConfidenceScoreDelta()).append(" points. ");
            } else if (resp.getConfidenceScoreDelta().compareTo(BigDecimal.ZERO) < 0) {
                trend.append("Confidence decreased by ").append(resp.getConfidenceScoreDelta().abs()).append(" points. ");
            } else {
                trend.append("Confidence unchanged. ");
            }
        }
        trend.append("Blocking issues: ").append(currentSummary.getBlockingIssueCount())
                .append(" (delta: ").append(integerOrZero(resp.getBlockingIssueDelta())).append("). ");
        resp.setTrendSummary(trend.toString());

        return resp;
    }

    @Transactional(readOnly = true)
    public List<ReleaseConfidenceTrendResponse> getTrend() {
        LambdaQueryWrapper<ReleaseConfidenceSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReleaseConfidenceSnapshotEntity::getSnapshotTime);
        wrapper.last("LIMIT 20");
        return releaseConfidenceSnapshotMapper.selectList(wrapper).stream()
                .map(this::toTrendResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseExecutiveReportResponse generateExecutiveReport(String planIdStr) {
        ReleaseExecutiveSummaryResponse summary = getExecutiveSummary(planIdStr);

        StringBuilder md = new StringBuilder();
        md.append("# Executive Release Summary\n\n");
        md.append("**Release**: ").append(summary.getReleaseLabel()).append("\n\n");
        md.append("**Status**: ").append(summary.getRolloutStatus()).append("\n\n");
        md.append("**Generated At**: ").append(LocalDateTime.now()).append("\n\n");
        md.append("---\n\n");

        md.append("## Confidence Assessment\n\n");
        md.append("- Score: ").append(summary.getConfidenceScore()).append("/100\n");
        md.append("- Level: **").append(summary.getConfidenceLevel()).append("**\n\n");

        // Warning indicators
        boolean hasWarnings = false;
        if ("CRITICAL".equals(summary.getConfidenceLevel())) { md.append("⚠️ Confidence level is CRITICAL.\n\n"); hasWarnings = true; }
        if (summary.getBlockingIssueCount() != null && summary.getBlockingIssueCount() > 0) { md.append("⚠️ ").append(summary.getBlockingIssueCount()).append(" blocking issues found.\n\n"); hasWarnings = true; }
        if (summary.getFailedVerificationCount() != null && summary.getFailedVerificationCount() > 0) { md.append("⚠️ ").append(summary.getFailedVerificationCount()).append(" failed verifications.\n\n"); hasWarnings = true; }
        if (Boolean.FALSE.equals(summary.getRollbackReady())) { md.append("⚠️ Rollback drill not ready.\n\n"); hasWarnings = true; }
        if (!hasWarnings) { md.append("No critical warnings detected.\n\n"); }

        md.append("## Rollout Summary\n\n");
        md.append("- Status: ").append(summary.getRolloutStatus()).append("\n");
        md.append("- Rollback Ready: ").append(Boolean.TRUE.equals(summary.getRollbackReady()) ? "Yes" : "No").append("\n");
        md.append("- Signoff Completion: ").append(summary.getSignoffCompletionRate()).append("%\n\n");

        md.append("## Risk Indicators\n\n");
        md.append("| Metric | Value |\n");
        md.append("|---|---|\n");
        md.append("| Blocking Issues | ").append(summary.getBlockingIssueCount()).append(" |\n");
        md.append("| Warning Issues | ").append(summary.getWarningIssueCount()).append(" |\n");
        md.append("| Open Incidents | ").append(summary.getOpenIncidentCount()).append(" |\n");
        md.append("| Active Alerts | ").append(summary.getActiveAlertCount()).append(" |\n");
        md.append("| Failed Verifications | ").append(summary.getFailedVerificationCount()).append(" |\n\n");

        md.append("## Summary\n\n");
        md.append(summary.getSummaryText()).append("\n");

        ReleaseExecutiveReportResponse resp = new ReleaseExecutiveReportResponse();
        resp.setPlanId(summary.getPlanId());
        resp.setReleaseLabel(summary.getReleaseLabel());
        resp.setReportMarkdown(md.toString());
        resp.setGeneratedAt(LocalDateTime.now());

        return resp;
    }

    @Transactional
    public ReleaseConfidenceSnapshotResponse takeConfidenceSnapshot(String planIdStr) {
        ReleaseExecutiveSummaryResponse summary = getExecutiveSummary(planIdStr);
        Long planId = parseLong(planIdStr, "planId");

        ReleaseConfidenceSnapshotEntity entity = new ReleaseConfidenceSnapshotEntity();
        entity.setPlanId(planId);
        entity.setProjectId(summary.getProjectId() != null ? parseLong(summary.getProjectId(), "projectId") : null);
        entity.setReleaseLabel(summary.getReleaseLabel());
        entity.setConfidenceScore(summary.getConfidenceScore());
        entity.setConfidenceLevel(summary.getConfidenceLevel());
        entity.setBlockingIssueCount(integerOrZero(summary.getBlockingIssueCount()));
        entity.setWarningIssueCount(integerOrZero(summary.getWarningIssueCount()));
        entity.setOpenIncidentCount(integerOrZero(summary.getOpenIncidentCount()));
        entity.setActiveAlertCount(integerOrZero(summary.getActiveAlertCount()));
        entity.setFailedVerificationCount(integerOrZero(summary.getFailedVerificationCount()));
        entity.setRollbackReady(Boolean.TRUE.equals(summary.getRollbackReady()) ? 1 : 0);
        entity.setSignoffCompletionRate(summary.getSignoffCompletionRate() != null ? summary.getSignoffCompletionRate() : BigDecimal.ZERO);
        entity.setSnapshotSummary(summary.getSummaryText() != null && summary.getSummaryText().length() > 255
                ? summary.getSummaryText().substring(0, 255) : summary.getSummaryText() != null ? summary.getSummaryText() : "");
        entity.setSnapshotTime(LocalDateTime.now());

        releaseConfidenceSnapshotMapper.insert(entity);

        ReleaseConfidenceSnapshotResponse resp = toSnapshotResponse(entity);
        return resp;
    }

    private ReleaseConfidenceSnapshotResponse calculateConfidence(
            int blockingIssues, int warningIssues, int openIncidents, int activeAlerts,
            int failedVerifications, boolean rollbackReady, BigDecimal signoffCompletionRate) {

        double score = 100.0;
        score -= blockingIssues * 20;
        score -= warningIssues * 5;
        score -= openIncidents * 10;
        score -= activeAlerts * 6;
        score -= failedVerifications * 12;
        score += rollbackReady ? 8 : -12;
        score += signoffCompletionRate.doubleValue() * 0.1;

        score = Math.max(0, Math.min(100, score));

        String level;
        if (score >= 85) level = "HIGH";
        else if (score >= 60) level = "MEDIUM";
        else if (score >= 30) level = "LOW";
        else level = "CRITICAL";

        ReleaseConfidenceSnapshotResponse resp = new ReleaseConfidenceSnapshotResponse();
        resp.setConfidenceScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
        resp.setConfidenceLevel(level);
        resp.setBlockingIssueCount(blockingIssues);
        resp.setWarningIssueCount(warningIssues);
        resp.setOpenIncidentCount(openIncidents);
        resp.setActiveAlertCount(activeAlerts);
        resp.setFailedVerificationCount(failedVerifications);
        resp.setRollbackReady(rollbackReady);
        resp.setSignoffCompletionRate(signoffCompletionRate);
        resp.setSnapshotTime(LocalDateTime.now());

        return resp;
    }

    private String buildSummaryText(ReleaseRolloutPlanEntity plan, ReleaseConfidenceSnapshotResponse snapshot,
                                     ReleasePostmortemReviewEntity latestReview) {
        StringBuilder sb = new StringBuilder();
        sb.append("Release ").append(plan.getReleaseLabel()).append(" ");
        sb.append("is in ").append(plan.getRolloutStatus()).append(" status. ");
        sb.append("Confidence score: ").append(snapshot.getConfidenceScore()).append("/100 (")
                .append(snapshot.getConfidenceLevel()).append("). ");
        sb.append("Blocking issues: ").append(snapshot.getBlockingIssueCount()).append(", ");
        sb.append("failed verifications: ").append(snapshot.getFailedVerificationCount()).append(". ");
        sb.append("Rollback ready: ").append(Boolean.TRUE.equals(snapshot.getRollbackReady()) ? "yes" : "no").append(". ");
        if (latestReview != null) {
            sb.append("Latest postmortem: ").append(latestReview.getOverallOutcome()).append(".");
        }
        return sb.toString();
    }

    private ReleaseConfidenceTrendResponse toTrendResponse(ReleaseConfidenceSnapshotEntity entity) {
        ReleaseConfidenceTrendResponse resp = new ReleaseConfidenceTrendResponse();
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setConfidenceScore(entity.getConfidenceScore());
        resp.setConfidenceLevel(entity.getConfidenceLevel());
        resp.setSnapshotTime(entity.getSnapshotTime());
        return resp;
    }

    private ReleaseConfidenceSnapshotResponse toSnapshotResponse(ReleaseConfidenceSnapshotEntity entity) {
        ReleaseConfidenceSnapshotResponse resp = new ReleaseConfidenceSnapshotResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setPlanId(entity.getPlanId() != null ? entity.getPlanId().toString() : null);
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setConfidenceScore(entity.getConfidenceScore());
        resp.setConfidenceLevel(entity.getConfidenceLevel());
        resp.setBlockingIssueCount(entity.getBlockingIssueCount());
        resp.setWarningIssueCount(entity.getWarningIssueCount());
        resp.setOpenIncidentCount(entity.getOpenIncidentCount());
        resp.setActiveAlertCount(entity.getActiveAlertCount());
        resp.setFailedVerificationCount(entity.getFailedVerificationCount());
        resp.setRollbackReady(entity.getRollbackReady() != null && entity.getRollbackReady() == 1);
        resp.setSignoffCompletionRate(entity.getSignoffCompletionRate());
        resp.setSnapshotSummary(entity.getSnapshotSummary());
        resp.setSnapshotTime(entity.getSnapshotTime());
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new com.aicoding.platform.common.exception.BizException(
                    com.aicoding.platform.common.exception.ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }

    private static Integer integerOrZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
