package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import java.math.BigDecimal;
import com.aicoding.platform.github.domain.PrReviewFindingEntity;
import com.aicoding.platform.github.domain.PrReviewJobEntity;
import com.aicoding.platform.github.infrastructure.PrReviewFindingMapper;
import com.aicoding.platform.github.infrastructure.PrReviewJobMapper;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.PrReviewQualityRecordEntity;
import com.aicoding.platform.orchestration.dto.CreatePrReviewQualityRecordRequest;
import com.aicoding.platform.orchestration.dto.PrReviewQualityDashboardResponse;
import com.aicoding.platform.orchestration.dto.PrReviewQualityRecordResponse;
import com.aicoding.platform.orchestration.dto.UpdatePrReviewQualityRecordRequest;
import com.aicoding.platform.orchestration.infrastructure.PrReviewQualityRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PrReviewQualityService {

    private final PrReviewQualityRecordMapper prReviewQualityRecordMapper;
    private final PrReviewJobMapper prReviewJobMapper;
    private final PrReviewFindingMapper prReviewFindingMapper;
    private final ProjectPermissionService projectPermissionService;

    public PrReviewQualityService(PrReviewQualityRecordMapper prReviewQualityRecordMapper,
                                  PrReviewJobMapper prReviewJobMapper,
                                  PrReviewFindingMapper prReviewFindingMapper,
                                  ProjectPermissionService projectPermissionService) {
        this.prReviewQualityRecordMapper = prReviewQualityRecordMapper;
        this.prReviewJobMapper = prReviewJobMapper;
        this.prReviewFindingMapper = prReviewFindingMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public PrReviewQualityRecordResponse createQualityRecord(String projectIdStr, CreatePrReviewQualityRecordRequest request) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER, ProjectRole.DEVELOPER);

        Long reviewJobId = parseLong(request.getReviewJobId(), "reviewJobId");
        PrReviewJobEntity job = prReviewJobMapper.selectById(reviewJobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "PR 评审任务不存在");
        }

        // Aggregate findings from pr_review_finding
        QueryWrapper<PrReviewFindingEntity> findingQuery = new QueryWrapper<>();
        findingQuery.select(
                        "COUNT(*) as findings_total",
                        "SUM(CASE WHEN severity = 'HIGH' THEN 1 ELSE 0 END) as high_risk",
                        "SUM(CASE WHEN severity = 'MEDIUM' THEN 1 ELSE 0 END) as medium_risk",
                        "SUM(CASE WHEN severity = 'LOW' THEN 1 ELSE 0 END) as low_risk")
                .eq("review_job_id", reviewJobId);
        List<Map<String, Object>> findings = prReviewFindingMapper.selectMaps(findingQuery);

        PrReviewQualityRecordEntity entity = new PrReviewQualityRecordEntity();
        entity.setProjectId(projectId);
        entity.setReviewJobId(reviewJobId);
        entity.setRepositoryFullName(job.getId() != null ? "unknown" : "unknown");
        entity.setPullRequestNumber(Objects.requireNonNullElse(job.getPullRequestId(), 0L));
        entity.setModelProvider(job.getModelProvider());
        entity.setModelName(job.getModelName());

        if (!findings.isEmpty()) {
            Map<String, Object> f = findings.get(0);
            entity.setFindingsTotal(toInt(f.get("findings_total")));
            entity.setHighRiskFindings(toInt(f.get("high_risk")));
            entity.setMediumRiskFindings(toInt(f.get("medium_risk")));
            entity.setLowRiskFindings(toInt(f.get("low_risk")));
        }

        // Set scores from request
        entity.setUsefulnessScore(request.getUsefulnessScore());
        entity.setFalsePositiveScore(request.getFalsePositiveScore());
        entity.setReviewComment(request.getReviewComment());

        // Set default status values
        entity.setReviewStatus(job.getStatus() != null ? job.getStatus() : "COMPLETED");
        entity.setHumanFeedbackStatus("PENDING");
        entity.setAdoptionStatus("UNKNOWN");

        prReviewQualityRecordMapper.insert(entity);
        return toRecordResponse(entity);
    }

    @Transactional
    public PrReviewQualityRecordResponse updateQualityRecord(String id, UpdatePrReviewQualityRecordRequest request) {
        Long recordId = parseLong(id, "id");
        PrReviewQualityRecordEntity entity = prReviewQualityRecordMapper.selectById(recordId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "PR 评审质量记录不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (request.getHumanFeedbackStatus() != null) {
            entity.setHumanFeedbackStatus(request.getHumanFeedbackStatus());
        }
        if (request.getAdoptionStatus() != null) {
            entity.setAdoptionStatus(request.getAdoptionStatus());
        }
        if (request.getUsefulnessScore() != null) {
            entity.setUsefulnessScore(request.getUsefulnessScore());
        }
        if (request.getFalsePositiveScore() != null) {
            entity.setFalsePositiveScore(request.getFalsePositiveScore());
        }
        if (request.getReviewComment() != null) {
            entity.setReviewComment(request.getReviewComment());
        }

        prReviewQualityRecordMapper.updateById(entity);
        return toRecordResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<PrReviewQualityRecordResponse> listQualityRecords(String projectIdStr, String status,
                                                                    int page, int size) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        LambdaQueryWrapper<PrReviewQualityRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrReviewQualityRecordEntity::getProjectId, projectId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(PrReviewQualityRecordEntity::getReviewStatus, status);
        }
        wrapper.orderByDesc(PrReviewQualityRecordEntity::getCreateTime);
        wrapper.last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        List<PrReviewQualityRecordEntity> entities = prReviewQualityRecordMapper.selectList(wrapper);
        return entities.stream().map(this::toRecordResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrReviewQualityDashboardResponse getQualityDashboard(String projectIdStr) {
        Long projectId = parseLong(projectIdStr, "projectId");
        projectPermissionService.checkProjectMember(projectId);

        PrReviewQualityDashboardResponse dashboard = new PrReviewQualityDashboardResponse();

        // Total counts by status
        QueryWrapper<PrReviewQualityRecordEntity> countQuery = new QueryWrapper<>();
        countQuery.select(
                        "COUNT(*) as total",
                        "SUM(CASE WHEN review_status = 'HIGH_VALUE' THEN 1 ELSE 0 END) as high_value",
                        "SUM(CASE WHEN review_status = 'ACTIONABLE' THEN 1 ELSE 0 END) as actionable",
                        "SUM(CASE WHEN review_status = 'LOW_SIGNAL' THEN 1 ELSE 0 END) as low_signal",
                        "SUM(CASE WHEN review_status = 'FAILED' THEN 1 ELSE 0 END) as failed")
                .eq("project_id", projectId);
        List<Map<String, Object>> counts = prReviewQualityRecordMapper.selectMaps(countQuery);
        if (!counts.isEmpty()) {
            Map<String, Object> c = counts.get(0);
            dashboard.setTotalReviews(toLong(c.get("total")));
            dashboard.setHighValueReviews(toLong(c.get("high_value")));
            dashboard.setActionableReviews(toLong(c.get("actionable")));
            dashboard.setLowSignalReviews(toLong(c.get("low_signal")));
            dashboard.setFailedReviews(toLong(c.get("failed")));
        }

        // Pending feedback count
        QueryWrapper<PrReviewQualityRecordEntity> pendingQuery = new QueryWrapper<>();
        pendingQuery.select("COUNT(*) as pending")
                .eq("project_id", projectId)
                .eq("human_feedback_status", "PENDING");
        List<Map<String, Object>> pendingResult = prReviewQualityRecordMapper.selectMaps(pendingQuery);
        if (!pendingResult.isEmpty()) {
            dashboard.setPendingFeedbackReviews(toLong(pendingResult.get(0).get("pending")));
        }

        // Adopted count
        QueryWrapper<PrReviewQualityRecordEntity> adoptedQuery = new QueryWrapper<>();
        adoptedQuery.select("COUNT(*) as adopted")
                .eq("project_id", projectId)
                .eq("adoption_status", "ADOPTED");
        List<Map<String, Object>> adoptedResult = prReviewQualityRecordMapper.selectMaps(adoptedQuery);
        if (!adoptedResult.isEmpty()) {
            dashboard.setAdoptedReviews(toLong(adoptedResult.get(0).get("adopted")));
        }

        // Average usefulness score
        QueryWrapper<PrReviewQualityRecordEntity> avgQuery = new QueryWrapper<>();
        avgQuery.select("COALESCE(AVG(usefulness_score), 0) as avg_score")
                .eq("project_id", projectId)
                .isNotNull("usefulness_score");
        List<Map<String, Object>> avgResult = prReviewQualityRecordMapper.selectMaps(avgQuery);
        if (!avgResult.isEmpty()) {
            Object val = avgResult.get(0).get("avg_score");
            if (val instanceof Number n) {
                dashboard.setAverageUsefulnessScore(n.doubleValue());
            }
        }

        // Recent reviews
        List<PrReviewQualityRecordEntity> recent = prReviewQualityRecordMapper.selectList(
                new LambdaQueryWrapper<PrReviewQualityRecordEntity>()
                        .eq(PrReviewQualityRecordEntity::getProjectId, projectId)
                        .orderByDesc(PrReviewQualityRecordEntity::getCreateTime)
                        .last("LIMIT 10"));
        dashboard.setRecentReviews(recent.stream().map(this::toRecordResponse).collect(Collectors.toList()));

        return dashboard;
    }

    private PrReviewQualityRecordResponse toRecordResponse(PrReviewQualityRecordEntity entity) {
        PrReviewQualityRecordResponse resp = new PrReviewQualityRecordResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId() != null ? entity.getProjectId().toString() : null);
        resp.setReviewJobId(entity.getReviewJobId() != null ? entity.getReviewJobId().toString() : null);
        resp.setGithubBindingId(entity.getGithubBindingId() != null ? entity.getGithubBindingId().toString() : null);
        resp.setRepositoryFullName(entity.getRepositoryFullName());
        resp.setPullRequestNumber(entity.getPullRequestNumber());
        resp.setStrategyKey(entity.getStrategyKey());
        resp.setModelProvider(entity.getModelProvider());
        resp.setModelName(entity.getModelName());
        resp.setFindingsTotal(entity.getFindingsTotal());
        resp.setHighRiskFindings(entity.getHighRiskFindings());
        resp.setMediumRiskFindings(entity.getMediumRiskFindings());
        resp.setLowRiskFindings(entity.getLowRiskFindings());
        resp.setReviewStatus(entity.getReviewStatus());
        resp.setHumanFeedbackStatus(entity.getHumanFeedbackStatus());
        resp.setAdoptionStatus(entity.getAdoptionStatus());
        resp.setUsefulnessScore(entity.getUsefulnessScore());
        resp.setFalsePositiveScore(entity.getFalsePositiveScore());
        resp.setReviewComment(entity.getReviewComment());
        resp.setReviewedBy(entity.getReviewedBy() != null ? entity.getReviewedBy().toString() : null);
        resp.setReviewedAt(entity.getReviewedAt());
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

    private static Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        return Long.valueOf(value.toString());
    }

    private static Integer toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        return Integer.valueOf(value.toString());
    }
}
