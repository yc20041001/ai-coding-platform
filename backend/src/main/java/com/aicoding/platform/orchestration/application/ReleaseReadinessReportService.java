package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.orchestration.domain.BetaReleaseDecisionEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutPlanEntity;
import com.aicoding.platform.orchestration.domain.ReleaseRolloutStepEntity;
import com.aicoding.platform.orchestration.domain.ReleaseVerificationRecordEntity;
import com.aicoding.platform.orchestration.dto.*;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseDecisionMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutPlanMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseRolloutStepMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleaseVerificationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReleaseReadinessReportService {

    private final ReleaseRolloutPlanMapper releaseRolloutPlanMapper;
    private final ReleaseRolloutStepMapper releaseRolloutStepMapper;
    private final ReleaseVerificationRecordMapper releaseVerificationRecordMapper;
    private final BetaReleaseDecisionMapper betaReleaseDecisionMapper;
    private final ReleaseVerificationService releaseVerificationService;
    private final ReleaseRolloutStepService releaseRolloutStepService;

    public ReleaseReadinessReportService(ReleaseRolloutPlanMapper releaseRolloutPlanMapper,
                                          ReleaseRolloutStepMapper releaseRolloutStepMapper,
                                          ReleaseVerificationRecordMapper releaseVerificationRecordMapper,
                                          BetaReleaseDecisionMapper betaReleaseDecisionMapper,
                                          ReleaseVerificationService releaseVerificationService,
                                          ReleaseRolloutStepService releaseRolloutStepService) {
        this.releaseRolloutPlanMapper = releaseRolloutPlanMapper;
        this.releaseRolloutStepMapper = releaseRolloutStepMapper;
        this.releaseVerificationRecordMapper = releaseVerificationRecordMapper;
        this.betaReleaseDecisionMapper = betaReleaseDecisionMapper;
        this.releaseVerificationService = releaseVerificationService;
        this.releaseRolloutStepService = releaseRolloutStepService;
    }

    @Transactional(readOnly = true)
    public ReleaseReadinessDashboardResponse getDashboard(String projectIdStr, String releaseLabel) {
        Long projectId = parseLong(projectIdStr, "projectId");

        ReleaseReadinessDashboardResponse resp = new ReleaseReadinessDashboardResponse();
        resp.setProjectId(projectIdStr);
        resp.setReleaseLabel(releaseLabel);
        resp.setLastEvaluatedAt(LocalDateTime.now());

        // Find latest plan for this project/label
        LambdaQueryWrapper<ReleaseRolloutPlanEntity> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(ReleaseRolloutPlanEntity::getProjectId, projectId);
        if (releaseLabel != null && !releaseLabel.isEmpty()) {
            planWrapper.eq(ReleaseRolloutPlanEntity::getReleaseLabel, releaseLabel);
        }
        planWrapper.orderByDesc(ReleaseRolloutPlanEntity::getCreateTime);
        planWrapper.last("LIMIT 1");
        ReleaseRolloutPlanEntity latestPlan = releaseRolloutPlanMapper.selectOne(planWrapper);

        if (latestPlan != null) {
            resp.setRolloutStatus(latestPlan.getRolloutStatus());

            // Aggregate steps
            LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
            stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, latestPlan.getId());
            List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);
            long totalSteps = steps.size();
            long passedSteps = steps.stream().filter(s -> "PASSED".equals(s.getStepStatus())).count();
            resp.setPreReleasePassRate(totalSteps > 0 ? (double) passedSteps / totalSteps : 0.0);

            // Aggregate verifications
            LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
            verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, latestPlan.getId());
            List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);
            resp.setBlockingIssueCount((int) verifications.stream()
                    .filter(v -> "FAILED".equals(v.getVerificationStatus()) && ("BLOCKING".equals(v.getSeverity()) || "CRITICAL".equals(v.getSeverity())))
                    .count());
            resp.setWarningIssueCount((int) verifications.stream()
                    .filter(v -> "FAILED".equals(v.getVerificationStatus()) && ("HIGH".equals(v.getSeverity()) || "MEDIUM".equals(v.getSeverity())))
                    .count());
            resp.setObservationVerificationCount(verifications.size());
            resp.setRollbackRecommended(verifications.stream()
                    .anyMatch(v -> "FAILED".equals(v.getVerificationStatus()) && "CRITICAL".equals(v.getSeverity())));
        }

        // Find latest decision
        LambdaQueryWrapper<BetaReleaseDecisionEntity> decWrapper = new LambdaQueryWrapper<>();
        decWrapper.eq(BetaReleaseDecisionEntity::getProjectId, projectId);
        decWrapper.orderByDesc(BetaReleaseDecisionEntity::getCreateTime);
        decWrapper.last("LIMIT 1");
        BetaReleaseDecisionEntity latestDecision = betaReleaseDecisionMapper.selectOne(decWrapper);
        if (latestDecision != null) {
            resp.setDecisionStatus(latestDecision.getDecisionStatus());
        }

        // Determine overall readiness status
        if (resp.getBlockingIssueCount() != null && resp.getBlockingIssueCount() > 0) {
            resp.setOverallReadinessStatus("BLOCKED");
        } else if (resp.getWarningIssueCount() != null && resp.getWarningIssueCount() > 0) {
            resp.setOverallReadinessStatus("WARN");
        } else if ("GO".equals(resp.getDecisionStatus())) {
            resp.setOverallReadinessStatus("READY");
        } else {
            resp.setOverallReadinessStatus("PENDING");
        }

        resp.setOpenIncidentCount(0);
        resp.setActiveAlertCount(0);
        resp.setHighRiskFeedbackCount(0);
        resp.setCostAlertCount(0);
        resp.setPrQualityWarnCount(0);

        return resp;
    }

    @Transactional(readOnly = true)
    public ReleaseRolloutSummaryResponse getSummary(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        ReleaseRolloutSummaryResponse resp = new ReleaseRolloutSummaryResponse();
        resp.setPlanId(plan.getId().toString());
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setRolloutStatus(plan.getRolloutStatus());
        resp.setRolloutStrategy(plan.getRolloutStrategy());
        resp.setTargetEnvironment(plan.getTargetEnvironment());
        resp.setStartedAt(plan.getPlannedStartAt());
        resp.setCompletedAt(plan.getPlannedEndAt());
        resp.setCreateTime(plan.getCreateTime());

        LambdaQueryWrapper<ReleaseRolloutStepEntity> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(ReleaseRolloutStepEntity::getPlanId, planId);
        List<ReleaseRolloutStepEntity> steps = releaseRolloutStepMapper.selectList(stepWrapper);
        resp.setTotalSteps(steps.size());
        resp.setPassedSteps((int) steps.stream().filter(s -> "PASSED".equals(s.getStepStatus())).count());
        resp.setFailedSteps((int) steps.stream().filter(s -> "FAILED".equals(s.getStepStatus())).count());
        resp.setSkippedSteps((int) steps.stream().filter(s -> "SKIPPED".equals(s.getStepStatus())).count());
        resp.setBlockedSteps((int) steps.stream().filter(s -> "BLOCKED".equals(s.getStepStatus())).count());

        LambdaQueryWrapper<ReleaseVerificationRecordEntity> verWrapper = new LambdaQueryWrapper<>();
        verWrapper.eq(ReleaseVerificationRecordEntity::getPlanId, planId);
        List<ReleaseVerificationRecordEntity> verifications = releaseVerificationRecordMapper.selectList(verWrapper);
        resp.setTotalVerifications(verifications.size());
        resp.setFailedVerifications((int) verifications.stream().filter(v -> "FAILED".equals(v.getVerificationStatus())).count());
        resp.setBlockingVerifications((int) verifications.stream()
                .filter(v -> "FAILED".equals(v.getVerificationStatus()) && ("BLOCKING".equals(v.getSeverity()) || "CRITICAL".equals(v.getSeverity())))
                .count());

        boolean hasBlockingFailure = verifications.stream()
                .anyMatch(v -> "FAILED".equals(v.getVerificationStatus()) && "CRITICAL".equals(v.getSeverity()));
        resp.setRollbackRecommended(hasBlockingFailure);

        if ("COMPLETED".equals(plan.getRolloutStatus())) {
            resp.setOverallResult("SUCCESS");
        } else if ("ROLLED_BACK".equals(plan.getRolloutStatus())) {
            resp.setOverallResult("ROLLED_BACK");
        } else if (hasBlockingFailure) {
            resp.setOverallResult("AT_RISK");
        } else {
            resp.setOverallResult("IN_PROGRESS");
        }

        return resp;
    }

    @Transactional(readOnly = true)
    public ReleaseReadinessReportResponse generateReport(String planIdStr) {
        Long planId = parseLong(planIdStr, "planId");
        ReleaseRolloutPlanEntity plan = releaseRolloutPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Rollout plan 不存在");
        }

        ReleaseReadinessReportResponse resp = new ReleaseReadinessReportResponse();
        resp.setReleaseLabel(plan.getReleaseLabel());
        resp.setRolloutStatus(plan.getRolloutStatus());
        resp.setRolloutStrategy(plan.getRolloutStrategy());
        resp.setTargetEnvironment(plan.getTargetEnvironment());
        resp.setGeneratedAt(LocalDateTime.now());

        // Find latest decision
        LambdaQueryWrapper<BetaReleaseDecisionEntity> decWrapper = new LambdaQueryWrapper<>();
        decWrapper.eq(BetaReleaseDecisionEntity::getProjectId, plan.getProjectId());
        decWrapper.orderByDesc(BetaReleaseDecisionEntity::getCreateTime);
        decWrapper.last("LIMIT 1");
        BetaReleaseDecisionEntity latestDecision = betaReleaseDecisionMapper.selectOne(decWrapper);
        resp.setDecisionStatus(latestDecision != null ? latestDecision.getDecisionStatus() : "N/A");

        // Load steps and verifications
        List<ReleaseRolloutStepResponse> steps = releaseRolloutStepService.listSteps(planIdStr);
        List<ReleaseVerificationRecordResponse> verifications = releaseVerificationService.listVerifications(planIdStr, null);
        resp.setSteps(steps);
        resp.setVerifications(verifications);

        // Determine overall readiness
        boolean hasBlocking = verifications.stream()
                .anyMatch(v -> "FAILED".equals(v.getVerificationStatus())
                        && ("BLOCKING".equals(v.getSeverity()) || "CRITICAL".equals(v.getSeverity())));
        boolean hasWarning = verifications.stream()
                .anyMatch(v -> "FAILED".equals(v.getVerificationStatus())
                        && ("HIGH".equals(v.getSeverity()) || "MEDIUM".equals(v.getSeverity())));

        if (hasBlocking) {
            resp.setOverallReadinessStatus("BLOCKED");
        } else if (hasWarning) {
            resp.setOverallReadinessStatus("WARN");
        } else if ("GO".equals(resp.getDecisionStatus())) {
            resp.setOverallReadinessStatus("READY");
        } else {
            resp.setOverallReadinessStatus("PENDING");
        }

        // Generate markdown report
        StringBuilder md = new StringBuilder();
        md.append("# Release Readiness Report\n\n");
        md.append("**Release**: ").append(plan.getReleaseLabel()).append("\n\n");
        md.append("**Status**: ").append(plan.getRolloutStatus()).append("\n\n");
        md.append("**Decision**: ").append(resp.getDecisionStatus()).append("\n\n");
        md.append("**Overall Readiness**: ").append(resp.getOverallReadinessStatus()).append("\n\n");
        md.append("---\n\n");
        md.append("## Rollout Steps\n\n");
        md.append("| # | Step | Status | Result |\n");
        md.append("|---|---|---|---|\n");
        for (ReleaseRolloutStepResponse s : steps) {
            md.append("| ").append(s.getStepOrder()).append(" | ")
                    .append(s.getDisplayName()).append(" | ")
                    .append(s.getStepStatus()).append(" | ")
                    .append(s.getActualResult() != null ? s.getActualResult() : "-").append(" |\n");
        }
        md.append("\n## Verification Results\n\n");
        md.append("| Verification | Phase | Status | Severity |\n");
        md.append("|---|---|---|---|\n");
        for (ReleaseVerificationRecordResponse v : verifications) {
            md.append("| ").append(v.getDisplayName()).append(" | ")
                    .append(v.getVerificationPhase()).append(" | ")
                    .append(v.getVerificationStatus()).append(" | ")
                    .append(v.getSeverity() != null ? v.getSeverity() : "-").append(" |\n");
        }

        resp.setReportMarkdown(md.toString());
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
