package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.BetaReleaseDecisionEntity;
import com.aicoding.platform.orchestration.domain.BetaReleaseGateEvaluationEntity;
import com.aicoding.platform.orchestration.dto.BetaReleaseDecisionResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseGateEvaluationResponse;
import com.aicoding.platform.orchestration.dto.BetaReleaseReadinessReportResponse;
import com.aicoding.platform.orchestration.dto.CreateBetaReleaseDecisionRequest;
import com.aicoding.platform.orchestration.dto.UpdateBetaReleaseDecisionRequest;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseDecisionMapper;
import com.aicoding.platform.orchestration.infrastructure.BetaReleaseGateEvaluationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaReleaseDecisionService {

    private final BetaReleaseDecisionMapper betaReleaseDecisionMapper;
    private final BetaReleaseGateEvaluationMapper betaReleaseGateEvaluationMapper;
    private final ProjectPermissionService projectPermissionService;

    public BetaReleaseDecisionService(BetaReleaseDecisionMapper betaReleaseDecisionMapper,
                                      BetaReleaseGateEvaluationMapper betaReleaseGateEvaluationMapper,
                                      ProjectPermissionService projectPermissionService) {
        this.betaReleaseDecisionMapper = betaReleaseDecisionMapper;
        this.betaReleaseGateEvaluationMapper = betaReleaseGateEvaluationMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public BetaReleaseDecisionResponse createDecision(String projectIdStr, CreateBetaReleaseDecisionRequest request) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER);

        BetaReleaseDecisionEntity entity = new BetaReleaseDecisionEntity();
        entity.setProjectId(projectId);
        entity.setReleaseLabel(request.getReleaseLabel());
        entity.setDecisionStatus(request.getDecisionStatus());
        entity.setDecisionReason(request.getDecisionReason());
        entity.setApproverId(request.getApproverId() != null ? parseLong(request.getApproverId(), "approverId") : null);
        entity.setApprovedAt(request.getDecisionStatus() != null && !request.getDecisionStatus().equals("NO_GO")
                ? LocalDateTime.now() : null);

        // Count blocking and warning issues from latest evaluations
        List<BetaReleaseGateEvaluationEntity> latestEvals = getLatestEvaluations(projectId);
        int blockingCount = 0;
        int warningCount = 0;
        for (BetaReleaseGateEvaluationEntity e : latestEvals) {
            if ("BLOCK".equals(e.getGateStatus())) {
                if (e.getBlocking() == 1) blockingCount++;
                else warningCount++;
            }
        }
        entity.setBlockingIssueCount(blockingCount);
        entity.setWarningIssueCount(warningCount);

        // Generate markdown report
        entity.setReportMarkdown(generateReportMarkdown(request.getReleaseLabel(),
                request.getDecisionStatus(), blockingCount, warningCount, latestEvals));

        betaReleaseDecisionMapper.insert(entity);
        return toDecisionResponse(entity);
    }

    @Transactional
    public BetaReleaseDecisionResponse updateDecision(String id, UpdateBetaReleaseDecisionRequest request) {
        Long decisionId = parseLong(id, "id");
        BetaReleaseDecisionEntity entity = betaReleaseDecisionMapper.selectById(decisionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "发布决策不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getDecisionStatus() != null) {
            entity.setDecisionStatus(request.getDecisionStatus());
            entity.setApprovedAt(LocalDateTime.now());
        }
        if (request.getDecisionReason() != null) {
            entity.setDecisionReason(request.getDecisionReason());
        }
        if (request.getApproverId() != null) {
            entity.setApproverId(parseLong(request.getApproverId(), "approverId"));
        }

        betaReleaseDecisionMapper.updateById(entity);
        return toDecisionResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BetaReleaseDecisionResponse> listDecisions(String projectIdStr, int page, int size) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<BetaReleaseDecisionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BetaReleaseDecisionEntity::getProjectId, projectId);
        wrapper.orderByDesc(BetaReleaseDecisionEntity::getCreateTime);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        return betaReleaseDecisionMapper.selectList(wrapper).stream()
                .map(this::toDecisionResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetaReleaseDecisionResponse getDecision(String id) {
        Long decisionId = parseLong(id, "id");
        BetaReleaseDecisionEntity entity = betaReleaseDecisionMapper.selectById(decisionId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "发布决策不存在");
        }
        projectPermissionService.checkProjectMember(entity.getProjectId());
        return toDecisionResponse(entity);
    }

    @Transactional(readOnly = true)
    public BetaReleaseReadinessReportResponse generateReadinessReport(String projectIdStr, String releaseLabel) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        BetaReleaseReadinessReportResponse report = new BetaReleaseReadinessReportResponse();
        report.setReleaseLabel(releaseLabel != null ? releaseLabel : "release-" + System.currentTimeMillis());

        List<BetaReleaseGateEvaluationEntity> latestEvals = getLatestEvaluations(projectId);
        long blockingFailures = latestEvals.stream()
                .filter(e -> "BLOCK".equals(e.getGateStatus()) && e.getBlocking() == 1)
                .count();
        long warnings = latestEvals.stream()
                .filter(e -> "BLOCK".equals(e.getGateStatus()) && e.getBlocking() == 0)
                .count();

        String overallStatus = blockingFailures > 0 ? "BLOCK" : warnings > 0 ? "WARN" : "PASS";
        report.setOverallStatus(overallStatus);
        report.setEvaluations(latestEvals.stream().map(this::toEvaluationResponse).collect(Collectors.toList()));

        String markdown = generateReadinessMarkdown(report.getReleaseLabel(),
                overallStatus, blockingFailures, warnings, latestEvals);
        report.setReportMarkdown(markdown);

        return report;
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

    private String generateReportMarkdown(String releaseLabel, String decisionStatus,
                                          int blockingCount, int warningCount,
                                          List<BetaReleaseGateEvaluationEntity> evaluations) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Beta Release Go/No-Go Decision Report\n\n");
        sb.append("**Release:** ").append(releaseLabel).append("\n\n");
        sb.append("**Decision:** ").append(decisionStatus).append("\n\n");
        sb.append("**Blocking Issues:** ").append(blockingCount).append("\n\n");
        sb.append("**Warnings:** ").append(warningCount).append("\n\n");

        if (!evaluations.isEmpty()) {
            sb.append("## Gate Evaluations\n\n");
            sb.append("| Rule | Category | Status | Actual | Threshold |\n");
            sb.append("|------|----------|--------|--------|-----------|\n");
            for (BetaReleaseGateEvaluationEntity e : evaluations) {
                sb.append("| ").append(e.getRuleKey()).append(" | ")
                        .append(e.getCategory()).append(" | ")
                        .append(e.getGateStatus()).append(" | ")
                        .append(e.getActualValue()).append(" | ")
                        .append(e.getThresholdValue()).append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateReadinessMarkdown(String releaseLabel, String overallStatus,
                                             long blockingFailures, long warnings,
                                             List<BetaReleaseGateEvaluationEntity> evaluations) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Beta Release Readiness Report\n\n");
        sb.append("**Release:** ").append(releaseLabel).append("\n\n");
        sb.append("**Overall Status:** ").append(overallStatus).append("\n\n");
        sb.append("**Blocking Failures:** ").append(blockingFailures).append("\n\n");
        sb.append("**Warnings:** ").append(warnings).append("\n\n");

        if (!evaluations.isEmpty()) {
            sb.append("## Gate Evaluations\n\n");
            sb.append("| Rule | Category | Status | Blocking |\n");
            sb.append("|------|----------|--------|----------|\n");
            for (BetaReleaseGateEvaluationEntity e : evaluations) {
                sb.append("| ").append(e.getRuleKey()).append(" | ")
                        .append(e.getCategory()).append(" | ")
                        .append(e.getGateStatus()).append(" | ")
                        .append(e.getBlocking() == 1 ? "Yes" : "No").append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
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

    private static Long parseLong(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " 格式无效");
        }
    }
}
