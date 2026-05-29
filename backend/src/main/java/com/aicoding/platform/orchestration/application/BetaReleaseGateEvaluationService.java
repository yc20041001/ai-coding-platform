package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaEnvironmentReadinessEntity;
import com.aicoding.platform.orchestration.domain.BetaReleaseDecisionEntity;
import com.aicoding.platform.orchestration.domain.BetaReleaseGateEvaluationEntity;
import com.aicoding.platform.orchestration.domain.BetaReleaseGateRuleEntity;
import com.aicoding.platform.orchestration.domain.BetaTrialFeedbackEntity;
import com.aicoding.platform.orchestration.domain.ModelCostAlertEntity;
import com.aicoding.platform.orchestration.domain.PrReviewQualityRecordEntity;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolKnowledgeQualityReviewEntity;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateDashboardResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateEvaluationResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseDecisionResponse;
import com.aicoding.platform.orchestration.infrastructure.BetaEnvironmentReadinessMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseDecisionMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateEvaluationMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateRuleMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaTrialFeedbackMapper;
import com.aicoding.platform.orchestration.infrastructure.ModelCostAlertMapper;
import com.aicoding.platform.orchestration.infrastructure.PrReviewQualityRecordMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolKnowledgeQualityReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BetaReleaseGateEvaluationService {

    private final BetaReleaseGateRuleMapper betaReleaseGateRuleMapper;
    private final BetaReleaseGateEvaluationMapper betaReleaseGateEvaluationMapper;
    private final BetaReleaseDecisionMapper betaReleaseDecisionMapper;
    private final BetaTrialFeedbackMapper betaTrialFeedbackMapper;
    private final BetaEnvironmentReadinessMapper betaEnvironmentReadinessMapper;
    private final ModelCostAlertMapper modelCostAlertMapper;
    private final PrReviewQualityRecordMapper prReviewQualityRecordMapper;
    private final ToolIncidentMapper toolIncidentMapper;
    private final ToolKnowledgeQualityReviewMapper toolKnowledgeQualityReviewMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaReleaseGateEvaluationService(BetaReleaseGateRuleMapper betaReleaseGateRuleMapper,
                                            BetaReleaseGateEvaluationMapper betaReleaseGateEvaluationMapper,
                                            BetaReleaseDecisionMapper betaReleaseDecisionMapper,
                                            BetaTrialFeedbackMapper betaTrialFeedbackMapper,
                                            BetaEnvironmentReadinessMapper betaEnvironmentReadinessMapper,
                                            ModelCostAlertMapper modelCostAlertMapper,
                                            PrReviewQualityRecordMapper prReviewQualityRecordMapper,
                                            ToolIncidentMapper toolIncidentMapper,
                                            ToolKnowledgeQualityReviewMapper toolKnowledgeQualityReviewMapper,
                                            ProjectPermissionService projectPermissionService) {
        this.betaReleaseGateRuleMapper = betaReleaseGateRuleMapper;
        this.betaReleaseGateEvaluationMapper = betaReleaseGateEvaluationMapper;
        this.betaReleaseDecisionMapper = betaReleaseDecisionMapper;
        this.betaTrialFeedbackMapper = betaTrialFeedbackMapper;
        this.betaEnvironmentReadinessMapper = betaEnvironmentReadinessMapper;
        this.modelCostAlertMapper = modelCostAlertMapper;
        this.prReviewQualityRecordMapper = prReviewQualityRecordMapper;
        this.toolIncidentMapper = toolIncidentMapper;
        this.toolKnowledgeQualityReviewMapper = toolKnowledgeQualityReviewMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public List<BetaReleaseGateEvaluationResponse> evaluate(String projectIdStr, String evaluationType, String evaluationTarget) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        LambdaQueryWrapper<BetaReleaseGateRuleEntity> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.and(w -> w.eq(BetaReleaseGateRuleEntity::getProjectId, projectId)
                .or().isNull(BetaReleaseGateRuleEntity::getProjectId));
        ruleWrapper.eq(BetaReleaseGateRuleEntity::getEnabled, 1);
        ruleWrapper.orderByAsc(BetaReleaseGateRuleEntity::getSortOrder);
        List<BetaReleaseGateRuleEntity> rules = betaReleaseGateRuleMapper.selectList(ruleWrapper);

        String target = evaluationTarget != null ? evaluationTarget : "release-" + System.currentTimeMillis();
        LocalDateTime evaluatedAt = LocalDateTime.now();
        List<BetaReleaseGateEvaluationEntity> evaluations = new ArrayList<>();

        for (BetaReleaseGateRuleEntity rule : rules) {
            BetaReleaseGateEvaluationEntity eval = evaluateSingleRule(projectId, rule, target, evaluationType, evaluatedAt);
            if (eval != null) {
                betaReleaseGateEvaluationMapper.insert(eval);
                evaluations.add(eval);
            }
        }

        return evaluations.stream().map(this::toEvaluationResponse).collect(Collectors.toList());
    }

    private BetaReleaseGateEvaluationEntity evaluateSingleRule(Long projectId, BetaReleaseGateRuleEntity rule,
                                                                String target, String evaluationType, LocalDateTime evaluatedAt) {
        BigDecimal actualValue = collectActualValue(projectId, rule);
        if (actualValue == null) {
            return null;
        }

        BigDecimal thresholdValue = rule.getThresholdValue();
        String operator = rule.getThresholdOperator();
        String gateStatus = computeGateStatus(actualValue, thresholdValue, operator);

        String summary = rule.getDisplayName() + ": " + formatValue(actualValue) + " "
                + operatorSymbol(operator) + " " + formatValue(thresholdValue) + " → " + gateStatus;

        BetaReleaseGateEvaluationEntity eval = new BetaReleaseGateEvaluationEntity();
        eval.setProjectId(projectId);
        eval.setEvaluationTarget(target);
        eval.setEvaluationType(evaluationType != null ? evaluationType : "MANUAL");
        eval.setRuleKey(rule.getRuleKey());
        eval.setCategory(rule.getCategory());
        eval.setGateStatus(gateStatus);
        eval.setActualValue(actualValue);
        eval.setThresholdValue(thresholdValue);
        eval.setBlocking(rule.getBlocking());
        eval.setSummary(summary);
        eval.setEvaluatedAt(evaluatedAt);
        return eval;
    }

    private BigDecimal collectActualValue(Long projectId, BetaReleaseGateRuleEntity rule) {
        return switch (rule.getRuleKey()) {
            case "P0_FEEDBACK_COUNT" -> countFeedbackBySeverity(projectId, "P0");
            case "P1_FEEDBACK_COUNT" -> countFeedbackBySeverity(projectId, "P1");
            case "RELEASE_BLOCKING_FEEDBACK_COUNT" -> countBlockingFeedback(projectId);
            case "READINESS_FAIL_COUNT" -> countReadinessFailures(projectId);
            case "MODEL_COST_ALERT_HIGH_COUNT" -> countHighCostAlerts(projectId);
            case "PR_REVIEW_FAILURE_RATIO" -> computePrReviewFailureRatio(projectId);
            case "PR_REVIEW_ADOPTION_RATIO" -> computePrReviewAdoptionRatio(projectId);
            case "OPEN_CRITICAL_INCIDENT_COUNT" -> countOpenCriticalIncidents(projectId);
            case "KNOWLEDGE_QUALITY_REJECTED_COUNT" -> countKnowledgeQualityRejected(projectId);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal countFeedbackBySeverity(Long projectId, String severity) {
        LambdaQueryWrapper<BetaTrialFeedbackEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(BetaTrialFeedbackEntity::getProjectId, projectId)
                .eq(BetaTrialFeedbackEntity::getSeverity, severity);
        return BigDecimal.valueOf(betaTrialFeedbackMapper.selectCount(qw));
    }

    private BigDecimal countBlockingFeedback(Long projectId) {
        LambdaQueryWrapper<BetaTrialFeedbackEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(BetaTrialFeedbackEntity::getProjectId, projectId)
                .eq(BetaTrialFeedbackEntity::getReleaseBlocking, true);
        return BigDecimal.valueOf(betaTrialFeedbackMapper.selectCount(qw));
    }

    private BigDecimal countReadinessFailures(Long projectId) {
        LambdaQueryWrapper<BetaEnvironmentReadinessEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(BetaEnvironmentReadinessEntity::getProjectId, projectId)
                .eq(BetaEnvironmentReadinessEntity::getCheckStatus, "FAILED");
        return BigDecimal.valueOf(betaEnvironmentReadinessMapper.selectCount(qw));
    }

    private BigDecimal countHighCostAlerts(Long projectId) {
        LambdaQueryWrapper<ModelCostAlertEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(ModelCostAlertEntity::getProjectId, projectId)
                .in(ModelCostAlertEntity::getSeverity, "HIGH", "CRITICAL")
                .eq(ModelCostAlertEntity::getStatus, "OPEN");
        return BigDecimal.valueOf(modelCostAlertMapper.selectCount(qw));
    }

    private BigDecimal computePrReviewFailureRatio(Long projectId) {
        QueryWrapper<PrReviewQualityRecordEntity> qw = new QueryWrapper<>();
        qw.select("COUNT(*) as total",
                        "SUM(CASE WHEN review_status = 'FAILED' THEN 1 ELSE 0 END) as failed")
                .eq("project_id", projectId);
        List<Map<String, Object>> result = prReviewQualityRecordMapper.selectMaps(qw);
        if (result.isEmpty()) return BigDecimal.ZERO;
        long total = toLong(result.get(0).get("total"));
        if (total == 0) return BigDecimal.ZERO;
        long failed = toLong(result.get(0).get("failed"));
        return BigDecimal.valueOf(failed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computePrReviewAdoptionRatio(Long projectId) {
        QueryWrapper<PrReviewQualityRecordEntity> qw = new QueryWrapper<>();
        qw.select("COUNT(*) as total",
                        "SUM(CASE WHEN adoption_status IN ('ADOPTED', 'PARTIAL') THEN 1 ELSE 0 END) as adopted")
                .eq("project_id", projectId);
        List<Map<String, Object>> result = prReviewQualityRecordMapper.selectMaps(qw);
        if (result.isEmpty()) return BigDecimal.ZERO;
        long total = toLong(result.get(0).get("total"));
        if (total == 0) return BigDecimal.ZERO;
        long adopted = toLong(result.get(0).get("adopted"));
        return BigDecimal.valueOf(adopted).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal countOpenCriticalIncidents(Long projectId) {
        LambdaQueryWrapper<ToolIncidentEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(ToolIncidentEntity::getProjectId, projectId)
                .eq(ToolIncidentEntity::getSeverity, "CRITICAL")
                .eq(ToolIncidentEntity::getStatus, "OPEN");
        return BigDecimal.valueOf(toolIncidentMapper.selectCount(qw));
    }

    private BigDecimal countKnowledgeQualityRejected(Long projectId) {
        LambdaQueryWrapper<ToolKnowledgeQualityReviewEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(ToolKnowledgeQualityReviewEntity::getProjectId, projectId)
                .eq(ToolKnowledgeQualityReviewEntity::getReviewStatus, "REJECTED");
        return BigDecimal.valueOf(toolKnowledgeQualityReviewMapper.selectCount(qw));
    }

    private String computeGateStatus(BigDecimal actual, BigDecimal threshold, String operator) {
        int cmp = actual.compareTo(threshold);
        boolean pass;
        switch (operator) {
            case "GT" -> pass = cmp > 0;
            case "GTE" -> pass = cmp >= 0;
            case "LT" -> pass = cmp < 0;
            case "LTE" -> pass = cmp <= 0;
            case "EQ" -> pass = cmp == 0;
            case "NEQ" -> pass = cmp != 0;
            default -> { return "SKIP"; }
        }
        return pass ? "PASS" : "BLOCK";
    }

    @Transactional(readOnly = true)
    public List<BetaReleaseGateEvaluationResponse> listEvaluations(String projectIdStr, String evaluationTarget,
                                                                    int page, int size) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<BetaReleaseGateEvaluationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BetaReleaseGateEvaluationEntity::getProjectId, projectId);
        if (evaluationTarget != null && !evaluationTarget.isBlank()) {
            wrapper.eq(BetaReleaseGateEvaluationEntity::getEvaluationTarget, evaluationTarget);
        }
        wrapper.orderByDesc(BetaReleaseGateEvaluationEntity::getEvaluatedAt);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        return betaReleaseGateEvaluationMapper.selectList(wrapper).stream()
                .map(this::toEvaluationResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetaReleaseGateDashboardResponse getGateDashboard(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        BetaReleaseGateDashboardResponse dashboard = new BetaReleaseGateDashboardResponse();
        BetaReleaseGateDashboardResponse.GateSummary summary = new BetaReleaseGateDashboardResponse.GateSummary();

        LambdaQueryWrapper<BetaReleaseGateRuleEntity> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.and(w -> w.eq(BetaReleaseGateRuleEntity::getProjectId, projectId)
                .or().isNull(BetaReleaseGateRuleEntity::getProjectId));
        ruleWrapper.eq(BetaReleaseGateRuleEntity::getEnabled, 1);
        summary.setTotalRules(betaReleaseGateRuleMapper.selectCount(ruleWrapper));

        List<BetaReleaseGateEvaluationEntity> latestEvals = getLatestEvaluations(projectId);
        long blockingFailures = 0;
        long warnings = 0;
        long passes = 0;
        for (BetaReleaseGateEvaluationEntity e : latestEvals) {
            if ("BLOCK".equals(e.getGateStatus())) {
                if (e.getBlocking() == 1) blockingFailures++;
                else warnings++;
            } else if ("PASS".equals(e.getGateStatus())) {
                passes++;
            }
        }
        summary.setBlockingFailures(blockingFailures);
        summary.setWarningCount(warnings);
        summary.setPassCount(passes);
        summary.setOverallStatus(blockingFailures > 0 ? "BLOCK" : warnings > 0 ? "WARN" : "PASS");
        dashboard.setSummary(summary);

        dashboard.setEvaluations(latestEvals.stream().map(this::toEvaluationResponse).collect(Collectors.toList()));

        List<BetaReleaseDecisionEntity> recentDecisions = betaReleaseDecisionMapper.selectList(
                new LambdaQueryWrapper<BetaReleaseDecisionEntity>()
                        .eq(BetaReleaseDecisionEntity::getProjectId, projectId)
                        .orderByDesc(BetaReleaseDecisionEntity::getCreateTime)
                        .last("LIMIT 10"));
        dashboard.setRecentDecisions(recentDecisions.stream().map(this::toDecisionResponse).collect(Collectors.toList()));

        return dashboard;
    }

    private List<BetaReleaseGateEvaluationEntity> getLatestEvaluations(Long projectId) {
        LambdaQueryWrapper<BetaReleaseGateEvaluationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BetaReleaseGateEvaluationEntity::getProjectId, projectId);
        wrapper.orderByDesc(BetaReleaseGateEvaluationEntity::getEvaluatedAt);
        List<BetaReleaseGateEvaluationEntity> all = betaReleaseGateEvaluationMapper.selectList(wrapper);

        return all.stream()
                .collect(Collectors.toMap(
                        BetaReleaseGateEvaluationEntity::getRuleKey,
                        e -> e,
                        (a, b) -> a))
                .values().stream().collect(Collectors.toList());
    }

    private BetaReleaseGateEvaluationResponse toEvaluationResponse(BetaReleaseGateEvaluationEntity entity) {
        BetaReleaseGateEvaluationResponse resp = new BetaReleaseGateEvaluationResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setEvaluationTarget(entity.getEvaluationTarget());
        resp.setEvaluationType(entity.getEvaluationType());
        resp.setRuleKey(entity.getRuleKey());
        resp.setCategory(entity.getCategory());
        resp.setGateStatus(entity.getGateStatus());
        resp.setActualValue(entity.getActualValue());
        resp.setThresholdValue(entity.getThresholdValue());
        resp.setBlocking(entity.getBlocking());
        resp.setSummary(entity.getSummary());
        resp.setDetail(entity.getDetail());
        resp.setEvidenceJson(entity.getEvidenceJson());
        resp.setEvaluatedAt(entity.getEvaluatedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private BetaReleaseDecisionResponse toDecisionResponse(BetaReleaseDecisionEntity entity) {
        BetaReleaseDecisionResponse resp = new BetaReleaseDecisionResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setReleaseLabel(entity.getReleaseLabel());
        resp.setDecisionStatus(entity.getDecisionStatus());
        resp.setDecisionReason(entity.getDecisionReason());
        resp.setBlockingIssueCount(entity.getBlockingIssueCount());
        resp.setWarningIssueCount(entity.getWarningIssueCount());
        resp.setApproverId(entity.getApproverId() != null ? entity.getApproverId().toString() : null);
        resp.setApprovedAt(entity.getApprovedAt());
        resp.setReportMarkdown(entity.getReportMarkdown());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        return Long.parseLong(value.toString());
    }

    private static String formatValue(BigDecimal value) {
        if (value == null) return "0";
        if (value.scale() <= 0) return String.valueOf(value.longValue());
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String operatorSymbol(String operator) {
        return switch (operator) {
            case "GT" -> ">";
            case "GTE" -> "≥";
            case "LT" -> "<";
            case "LTE" -> "≤";
            case "EQ" -> "=";
            case "NEQ" -> "≠";
            default -> operator;
        };
    }
}
