package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.OrganizationTrialPolicyEntity;
import com.aicoding.platform.orchestration.domain.ReleaseGuardrailEvaluationEntity;
import com.aicoding.platform.orchestration.domain.ReleasePortfolioSnapshotEntity;
import com.aicoding.platform.orchestration.dto.GovernanceRecommendationResponse;
import com.aicoding.platform.orchestration.dto.ReleaseGuardrailDashboardResponse;
import com.aicoding.platform.orchestration.dto.ReleaseGuardrailEvaluationResponse;
import com.aicoding.platform.orchestration.infrastructure.ReleaseGuardrailEvaluationMapper;
import com.aicoding.platform.orchestration.infrastructure.ReleasePortfolioSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReleaseGuardrailAutomationService {

    private final ReleaseGuardrailEvaluationMapper releaseGuardrailEvaluationMapper;
    private final ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper;
    private final OrganizationTrialPolicyService organizationTrialPolicyService;

    private static final BigDecimal DEFAULT_MIN_CONFIDENCE = BigDecimal.valueOf(50);
    private static final int DEFAULT_MAX_BLOCKING_ISSUES = 3;
    private static final int DEFAULT_MAX_FAILED_VERIFICATIONS = 2;
    private static final BigDecimal DEFAULT_MIN_SIGNOFF = BigDecimal.valueOf(60);
    private static final int DEFAULT_MAX_OPEN_INCIDENTS = 3;
    private static final int DEFAULT_MAX_ACTIVE_ALERTS = 5;

    public ReleaseGuardrailAutomationService(ReleaseGuardrailEvaluationMapper releaseGuardrailEvaluationMapper,
                                              ReleasePortfolioSnapshotMapper releasePortfolioSnapshotMapper,
                                              OrganizationTrialPolicyService organizationTrialPolicyService) {
        this.releaseGuardrailEvaluationMapper = releaseGuardrailEvaluationMapper;
        this.releasePortfolioSnapshotMapper = releasePortfolioSnapshotMapper;
        this.organizationTrialPolicyService = organizationTrialPolicyService;
    }

    @Transactional
    public void refreshGuardrails() {
        LocalDate today = LocalDate.now();

        // Delete existing evaluations for today
        LambdaQueryWrapper<ReleaseGuardrailEvaluationEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ReleaseGuardrailEvaluationEntity::getSnapshotDate, today);
        releaseGuardrailEvaluationMapper.delete(deleteWrapper);

        // Get portfolio snapshots for today
        List<ReleasePortfolioSnapshotEntity> snapshots = getTodayPortfolioSnapshots(today);
        if (snapshots.isEmpty()) return;

        // Get enabled policies
        List<OrganizationTrialPolicyEntity> policies = organizationTrialPolicyService.getEnabledPolicies();
        String defaultPolicyKey = policies.isEmpty() ? "DEFAULT" : policies.get(0).getPolicyKey();

        List<ReleaseGuardrailEvaluationEntity> evaluations = new ArrayList<>();

        for (ReleasePortfolioSnapshotEntity snap : snapshots) {
            Long projectId = snap.getProjectId();
            if (projectId == null) continue;

            String projectName = snap.getProjectName();
            BigDecimal confidence = snap.getConfidenceScore() != null ? snap.getConfidenceScore() : BigDecimal.ZERO;
            int blockingIssues = intOrZero(snap.getBlockingIssueCount());
            int failedVerifications = intOrZero(snap.getFailedVerificationCount());
            BigDecimal signoffRate = snap.getSignoffCompletionRate() != null ? snap.getSignoffCompletionRate() : BigDecimal.ZERO;
            int openIncidents = intOrZero(snap.getOpenIncidentCount());
            int activeAlerts = intOrZero(snap.getActiveAlertCount());
            int rollbackReady = intOrZero(snap.getRollbackReady());

            // MIN_CONFIDENCE_SCORE guardrail
            evaluations.add(evaluateConfidence(today, projectId, projectName, defaultPolicyKey, confidence));

            // MAX_BLOCKING_ISSUES guardrail
            evaluations.add(evaluateBlockingIssues(today, projectId, projectName, defaultPolicyKey, blockingIssues));

            // MAX_FAILED_VERIFICATIONS guardrail
            evaluations.add(evaluateFailedVerifications(today, projectId, projectName, defaultPolicyKey, failedVerifications));

            // REQUIRE_ROLLBACK_READY guardrail
            evaluations.add(evaluateRollbackReady(today, projectId, projectName, defaultPolicyKey, rollbackReady));

            // MIN_SIGNOFF_COMPLETION guardrail
            evaluations.add(evaluateSignoffCompletion(today, projectId, projectName, defaultPolicyKey, signoffRate));

            // MAX_OPEN_INCIDENTS guardrail
            evaluations.add(evaluateOpenIncidents(today, projectId, projectName, defaultPolicyKey, openIncidents));

            // MAX_ACTIVE_ALERTS guardrail
            evaluations.add(evaluateActiveAlerts(today, projectId, projectName, defaultPolicyKey, activeAlerts));
        }

        for (ReleaseGuardrailEvaluationEntity eval : evaluations) {
            releaseGuardrailEvaluationMapper.insert(eval);
        }
    }

    private ReleaseGuardrailEvaluationEntity evaluateConfidence(LocalDate date, Long projectId, String projectName,
                                                                  String policyKey, BigDecimal confidence) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (confidence.compareTo(BigDecimal.valueOf(70)) >= 0) {
            status = "PASS";
            severity = "INFO";
            summary = "Confidence score " + confidence + " meets threshold";
        } else if (confidence.compareTo(DEFAULT_MIN_CONFIDENCE) >= 0) {
            status = "WARN";
            severity = "MEDIUM";
            summary = "Confidence score " + confidence + " is below 70 threshold";
            recommendation = "考虑提升 confidence 后再扩大试用";
        } else {
            status = "BLOCK";
            severity = "HIGH";
            summary = "Confidence score " + confidence + " is below minimum 50 threshold";
            recommendation = "暂停扩展，优先修复 blocking issue 提升 confidence";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MIN_CONFIDENCE_SCORE",
                "CONFIDENCE", status, severity, confidence, BigDecimal.valueOf(70), summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateBlockingIssues(LocalDate date, Long projectId, String projectName,
                                                                     String policyKey, int blockingIssues) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (blockingIssues == 0) {
            status = "PASS";
            severity = "INFO";
            summary = "No blocking issues";
        } else if (blockingIssues <= DEFAULT_MAX_BLOCKING_ISSUES) {
            status = "WARN";
            severity = "MEDIUM";
            summary = blockingIssues + " blocking issues exist";
            recommendation = "优先解决 blocking issue 后再扩大试用";
        } else {
            status = "BLOCK";
            severity = "CRITICAL";
            summary = blockingIssues + " blocking issues exceed limit";
            recommendation = "暂停扩展，必须解决 blocking issue";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MAX_BLOCKING_ISSUES",
                "ISSUES", status, severity, BigDecimal.valueOf(blockingIssues), BigDecimal.valueOf(DEFAULT_MAX_BLOCKING_ISSUES),
                summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateFailedVerifications(LocalDate date, Long projectId, String projectName,
                                                                          String policyKey, int failedVerifications) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (failedVerifications == 0) {
            status = "PASS";
            severity = "INFO";
            summary = "All verifications passed";
        } else if (failedVerifications <= DEFAULT_MAX_FAILED_VERIFICATIONS) {
            status = "WARN";
            severity = "MEDIUM";
            summary = failedVerifications + " verifications failed";
            recommendation = "修复验证失败后再推进";
        } else {
            status = "BLOCK";
            severity = "HIGH";
            summary = failedVerifications + " verifications failed, exceeds limit";
            recommendation = "必须先通过验证才能推进";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MAX_FAILED_VERIFICATIONS",
                "VERIFICATION", status, severity, BigDecimal.valueOf(failedVerifications),
                BigDecimal.valueOf(DEFAULT_MAX_FAILED_VERIFICATIONS), summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateRollbackReady(LocalDate date, Long projectId, String projectName,
                                                                    String policyKey, int rollbackReady) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (rollbackReady == 1) {
            status = "PASS";
            severity = "INFO";
            summary = "Rollback drill completed";
        } else {
            status = "BLOCK";
            severity = "CRITICAL";
            summary = "Rollback drill not completed";
            recommendation = "补齐 rollback drill 再推进";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "REQUIRE_ROLLBACK_READY",
                "ROLLBACK", status, severity, BigDecimal.valueOf(rollbackReady), BigDecimal.ONE,
                summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateSignoffCompletion(LocalDate date, Long projectId, String projectName,
                                                                        String policyKey, BigDecimal signoffRate) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (signoffRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = "PASS";
            severity = "INFO";
            summary = "Signoff completion " + signoffRate + "%";
        } else if (signoffRate.compareTo(DEFAULT_MIN_SIGNOFF) >= 0) {
            status = "WARN";
            severity = "LOW";
            summary = "Signoff completion " + signoffRate + "% is below 80%";
            recommendation = "补齐签字角色";
        } else {
            status = "WARN";
            severity = "MEDIUM";
            summary = "Signoff completion " + signoffRate + "% is significantly low";
            recommendation = "严重缺少签字，优先补齐";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MIN_SIGNOFF_COMPLETION",
                "SIGNOFF", status, severity, signoffRate, BigDecimal.valueOf(80), summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateOpenIncidents(LocalDate date, Long projectId, String projectName,
                                                                    String policyKey, int openIncidents) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (openIncidents == 0) {
            status = "PASS";
            severity = "INFO";
            summary = "No open incidents";
        } else if (openIncidents <= DEFAULT_MAX_OPEN_INCIDENTS) {
            status = "WARN";
            severity = "MEDIUM";
            summary = openIncidents + " open incidents";
            recommendation = "处理 open incident 后再扩大试用";
        } else {
            status = "BLOCK";
            severity = "HIGH";
            summary = openIncidents + " open incidents exceed limit";
            recommendation = "必须先解决 incident 再推进";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MAX_OPEN_INCIDENTS",
                "INCIDENT", status, severity, BigDecimal.valueOf(openIncidents),
                BigDecimal.valueOf(DEFAULT_MAX_OPEN_INCIDENTS), summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity evaluateActiveAlerts(LocalDate date, Long projectId, String projectName,
                                                                   String policyKey, int activeAlerts) {
        String status;
        String severity;
        String summary;
        String recommendation = null;

        if (activeAlerts == 0) {
            status = "PASS";
            severity = "INFO";
            summary = "No active alerts";
        } else if (activeAlerts <= DEFAULT_MAX_ACTIVE_ALERTS) {
            status = "WARN";
            severity = "LOW";
            summary = activeAlerts + " active alerts";
            recommendation = "关注告警并评估影响";
        } else {
            status = "WARN";
            severity = "MEDIUM";
            summary = activeAlerts + " active alerts need attention";
            recommendation = "处理活跃告警后再扩大试用";
        }

        return buildEvaluation(date, projectId, projectName, policyKey, "MAX_ACTIVE_ALERTS",
                "ALERT", status, severity, BigDecimal.valueOf(activeAlerts),
                BigDecimal.valueOf(DEFAULT_MAX_ACTIVE_ALERTS), summary, recommendation, null);
    }

    private ReleaseGuardrailEvaluationEntity buildEvaluation(LocalDate date, Long projectId, String projectName,
                                                              String policyKey, String guardrailKey, String category,
                                                              String status, String severity, BigDecimal actual,
                                                              BigDecimal threshold, String summary, String recommendation,
                                                              String evidenceJson) {
        ReleaseGuardrailEvaluationEntity eval = new ReleaseGuardrailEvaluationEntity();
        eval.setSnapshotDate(date);
        eval.setProjectId(projectId);
        eval.setProjectName(projectName);
        eval.setPolicyKey(policyKey);
        eval.setGuardrailKey(guardrailKey);
        eval.setGuardrailCategory(category);
        eval.setEvaluationStatus(status);
        eval.setSeverity(severity);
        eval.setActualValue(actual);
        eval.setThresholdValue(threshold);
        eval.setSummary(summary);
        eval.setRecommendationText(recommendation);
        eval.setEvidenceJson(evidenceJson);
        eval.setCreateTime(LocalDateTime.now());
        return eval;
    }

    @Transactional(readOnly = true)
    public List<ReleaseGuardrailEvaluationResponse> getGuardrails() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ReleaseGuardrailEvaluationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseGuardrailEvaluationEntity::getSnapshotDate, today);
        wrapper.orderByDesc(ReleaseGuardrailEvaluationEntity::getSeverity);
        List<ReleaseGuardrailEvaluationEntity> list = releaseGuardrailEvaluationMapper.selectList(wrapper);
        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(ReleaseGuardrailEvaluationEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = releaseGuardrailEvaluationMapper.selectList(wrapper);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseGuardrailDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        List<ReleaseGuardrailEvaluationResponse> all = getGuardrails();
        List<ReleaseGuardrailEvaluationResponse> blocked = all.stream()
                .filter(e -> "BLOCK".equals(e.getEvaluationStatus()))
                .collect(Collectors.toList());
        List<ReleaseGuardrailEvaluationResponse> warned = all.stream()
                .filter(e -> "WARN".equals(e.getEvaluationStatus()))
                .collect(Collectors.toList());

        ReleaseGuardrailDashboardResponse resp = new ReleaseGuardrailDashboardResponse();
        resp.setSnapshotDate(today);
        resp.setProjectCount((int) all.stream().map(ReleaseGuardrailEvaluationResponse::getProjectId).distinct().count());
        resp.setPassCount((int) all.stream().filter(e -> "PASS".equals(e.getEvaluationStatus())).count());
        resp.setWarnCount(warned.size());
        resp.setBlockCount(blocked.size());
        resp.setCriticalCount((int) all.stream().filter(e -> "CRITICAL".equals(e.getSeverity())).count());
        resp.setTopBlockedProjects(blocked.stream().limit(5).collect(Collectors.toList()));
        resp.setTopWarningProjects(warned.stream().limit(5).collect(Collectors.toList()));
        resp.setRecommendationCount((int) all.stream().filter(e -> e.getRecommendationText() != null).count());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<GovernanceRecommendationResponse> getRecommendations() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ReleaseGuardrailEvaluationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleaseGuardrailEvaluationEntity::getSnapshotDate, today);
        wrapper.isNotNull(ReleaseGuardrailEvaluationEntity::getRecommendationText);
        wrapper.orderByDesc(ReleaseGuardrailEvaluationEntity::getSeverity);
        List<ReleaseGuardrailEvaluationEntity> list = releaseGuardrailEvaluationMapper.selectList(wrapper);

        if (list.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(ReleaseGuardrailEvaluationEntity::getRecommendationText);
            wrapper.orderByDesc(ReleaseGuardrailEvaluationEntity::getCreateTime);
            wrapper.last("LIMIT 50");
            list = releaseGuardrailEvaluationMapper.selectList(wrapper);
        }

        List<GovernanceRecommendationResponse> result = new ArrayList<>();
        for (ReleaseGuardrailEvaluationEntity e : list) {
            GovernanceRecommendationResponse rec = new GovernanceRecommendationResponse();
            rec.setProjectId(e.getProjectId() != null ? e.getProjectId().toString() : null);
            rec.setProjectName(e.getProjectName());
            rec.setPriority(toPriority(e.getSeverity()));
            rec.setCategory(e.getGuardrailCategory());
            rec.setTitle(e.getSummary());
            rec.setSummary(e.getRecommendationText());
            rec.setSourceType("GUARDRAIL");
            rec.setPolicyKey(e.getPolicyKey());
            rec.setGuardrailKey(e.getGuardrailKey());
            rec.setSnapshotDate(e.getSnapshotDate());
            result.add(rec);
        }
        return result;
    }

    private String toPriority(String severity) {
        if (severity == null) return "P3";
        switch (severity) {
            case "CRITICAL": return "P0";
            case "HIGH": return "P1";
            case "MEDIUM": return "P2";
            default: return "P3";
        }
    }

    private List<ReleasePortfolioSnapshotEntity> getTodayPortfolioSnapshots(LocalDate date) {
        LambdaQueryWrapper<ReleasePortfolioSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReleasePortfolioSnapshotEntity::getSnapshotDate, date);
        return releasePortfolioSnapshotMapper.selectList(wrapper);
    }

    private ReleaseGuardrailEvaluationResponse toResponse(ReleaseGuardrailEvaluationEntity entity) {
        ReleaseGuardrailEvaluationResponse resp = new ReleaseGuardrailEvaluationResponse();
        resp.setId(entity.getId() != null ? entity.getId().toString() : null);
        resp.setSnapshotDate(entity.getSnapshotDate());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setProjectName(entity.getProjectName());
        resp.setPolicyKey(entity.getPolicyKey());
        resp.setGuardrailKey(entity.getGuardrailKey());
        resp.setGuardrailCategory(entity.getGuardrailCategory());
        resp.setEvaluationStatus(entity.getEvaluationStatus());
        resp.setSeverity(entity.getSeverity());
        resp.setActualValue(entity.getActualValue());
        resp.setThresholdValue(entity.getThresholdValue());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setRecommendationText(entity.getRecommendationText());
        resp.setEvidenceJson(entity.getEvidenceJson());
        return resp;
    }

    private static int intOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
