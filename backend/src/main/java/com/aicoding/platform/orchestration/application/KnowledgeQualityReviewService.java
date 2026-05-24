package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.member.application.ProjectPermissionService;
import com.aicoding.platform.member.domain.ProjectRole;
import com.aicoding.platform.orchestration.domain.KnowledgeQualityOverallStatus;
import com.aicoding.platform.orchestration.domain.KnowledgeQualityReviewStatus;
import com.aicoding.platform.orchestration.domain.ToolIncidentEntity;
import com.aicoding.platform.orchestration.domain.ToolKnowledgeQualityReviewEntity;
import com.aicoding.platform.orchestration.dto.CreateKnowledgeQualityReviewRequest;
import com.aicoding.platform.orchestration.dto.KnowledgeQualityReviewResponse;
import com.aicoding.platform.orchestration.dto.KnowledgeQualityStatusSummaryResponse;
import com.aicoding.platform.orchestration.dto.UpdateKnowledgeQualityReviewRequest;
import com.aicoding.platform.orchestration.infrastructure.ToolIncidentMapper;
import com.aicoding.platform.orchestration.infrastructure.ToolKnowledgeQualityReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeQualityReviewService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQualityReviewService.class);

    private final ToolKnowledgeQualityReviewMapper reviewMapper;
    private final ToolIncidentMapper incidentMapper;
    private final ProjectPermissionService projectPermissionService;

    public KnowledgeQualityReviewService(ToolKnowledgeQualityReviewMapper reviewMapper,
                                         ToolIncidentMapper incidentMapper,
                                         ProjectPermissionService projectPermissionService) {
        this.reviewMapper = reviewMapper;
        this.incidentMapper = incidentMapper;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public KnowledgeQualityReviewResponse createReview(Long incidentId, CreateKnowledgeQualityReviewRequest request) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        validateScores(request.getCompletenessScore(), request.getAccuracyScore(),
                request.getActionabilityScore(), request.getRelevanceScore());

        ToolKnowledgeQualityReviewEntity entity = new ToolKnowledgeQualityReviewEntity();
        entity.setProjectId(incident.getProjectId());
        entity.setIncidentId(incidentId);
        entity.setKnowledgeDocumentId(request.getKnowledgeDocumentId());
        entity.setRetrospectiveId(request.getRetrospectiveId());
        entity.setCompletenessScore(request.getCompletenessScore());
        entity.setAccuracyScore(request.getAccuracyScore());
        entity.setActionabilityScore(request.getActionabilityScore());
        entity.setRelevanceScore(request.getRelevanceScore());
        entity.setReviewStatus(KnowledgeQualityReviewStatus.PENDING.name());
        entity.setOverallStatus(computeOverallStatus(request));
        entity.setChecklistJson(request.getChecklistJson());
        entity.setReviewComment(request.getReviewComment());

        reviewMapper.insert(entity);
        log.info("Created knowledge quality review: id={}, incidentId={}, overallStatus={}",
                entity.getId(), incidentId, entity.getOverallStatus());

        return toResponse(entity);
    }

    @Transactional
    public KnowledgeQualityReviewResponse updateReview(Long reviewId, UpdateKnowledgeQualityReviewRequest request) {
        ToolKnowledgeQualityReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识质量审查记录不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER);

        if (KnowledgeQualityReviewStatus.COMPLETED.name().equals(entity.getReviewStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "已完成的审查不可修改");
        }

        if (request.getCompletenessScore() != null) entity.setCompletenessScore(request.getCompletenessScore());
        if (request.getAccuracyScore() != null) entity.setAccuracyScore(request.getAccuracyScore());
        if (request.getActionabilityScore() != null) entity.setActionabilityScore(request.getActionabilityScore());
        if (request.getRelevanceScore() != null) entity.setRelevanceScore(request.getRelevanceScore());

        if (request.getCompletenessScore() != null || request.getAccuracyScore() != null ||
                request.getActionabilityScore() != null || request.getRelevanceScore() != null) {
            validateScores(entity.getCompletenessScore(), entity.getAccuracyScore(),
                    entity.getActionabilityScore(), entity.getRelevanceScore());
            entity.setOverallStatus(computeOverallStatusFromEntity(entity));
        }

        if (request.getChecklistJson() != null) entity.setChecklistJson(request.getChecklistJson());
        if (request.getReviewComment() != null) entity.setReviewComment(request.getReviewComment());

        if (request.getReviewStatus() != null) {
            String newStatus = request.getReviewStatus();
            String oldStatus = entity.getReviewStatus();
            if (isValidReviewStatusTransition(oldStatus, newStatus)) {
                entity.setReviewStatus(newStatus);
                if (KnowledgeQualityReviewStatus.COMPLETED.name().equals(newStatus)) {
                    entity.setReviewerId(300002L);
                    entity.setReviewedAt(LocalDateTime.now());
                    entity.setOverallStatus(computeOverallStatusFromEntity(entity));
                }
            } else {
                throw new BizException(ErrorCode.VALIDATION_ERROR,
                        "审查状态不允许从 " + oldStatus + " 转换到 " + newStatus);
            }
        }

        reviewMapper.updateById(entity);
        log.info("Updated knowledge quality review: id={}, reviewStatus={}, overallStatus={}",
                reviewId, entity.getReviewStatus(), entity.getOverallStatus());

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public KnowledgeQualityReviewResponse getReview(Long reviewId) {
        ToolKnowledgeQualityReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识质量审查记录不存在");
        }
        projectPermissionService.checkProjectRole(entity.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public KnowledgeQualityReviewResponse getIncidentReview(Long incidentId) {
        ToolIncidentEntity incident = getIncidentOrThrow(incidentId);
        projectPermissionService.checkProjectRole(incident.getProjectId(), ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        ToolKnowledgeQualityReviewEntity entity = reviewMapper.selectOne(
                new LambdaQueryWrapper<ToolKnowledgeQualityReviewEntity>()
                        .eq(ToolKnowledgeQualityReviewEntity::getIncidentId, incidentId)
                        .last("LIMIT 1"));
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该事件没有知识质量审查记录");
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeQualityReviewResponse> listProjectReviews(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<ToolKnowledgeQualityReviewEntity> entities = reviewMapper.selectList(
                new LambdaQueryWrapper<ToolKnowledgeQualityReviewEntity>()
                        .eq(ToolKnowledgeQualityReviewEntity::getProjectId, projectId)
                        .orderByDesc(ToolKnowledgeQualityReviewEntity::getCreateTime));

        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KnowledgeQualityStatusSummaryResponse getProjectStatusSummary(Long projectId) {
        projectPermissionService.checkProjectRole(projectId, ProjectRole.OWNER, ProjectRole.MAINTAINER,
                ProjectRole.DEVELOPER, ProjectRole.VIEWER);

        List<ToolKnowledgeQualityReviewEntity> all = reviewMapper.selectList(
                new LambdaQueryWrapper<ToolKnowledgeQualityReviewEntity>()
                        .eq(ToolKnowledgeQualityReviewEntity::getProjectId, projectId));

        KnowledgeQualityStatusSummaryResponse resp = new KnowledgeQualityStatusSummaryResponse();
        resp.setTotalReviews((long) all.size());

        long approved = 0, needsWork = 0, rejected = 0;
        long pending = 0, inReview = 0;
        double sumCompleteness = 0, sumAccuracy = 0, sumActionability = 0, sumRelevance = 0;

        for (ToolKnowledgeQualityReviewEntity e : all) {
            String ov = e.getOverallStatus();
            if (KnowledgeQualityOverallStatus.APPROVED.name().equals(ov)) approved++;
            else if (KnowledgeQualityOverallStatus.NEEDS_WORK.name().equals(ov)) needsWork++;
            else if (KnowledgeQualityOverallStatus.REJECTED.name().equals(ov)) rejected++;

            String rs = e.getReviewStatus();
            if (KnowledgeQualityReviewStatus.PENDING.name().equals(rs)) pending++;
            else if (KnowledgeQualityReviewStatus.IN_REVIEW.name().equals(rs)) inReview++;

            sumCompleteness += e.getCompletenessScore() != null ? e.getCompletenessScore() : 0;
            sumAccuracy += e.getAccuracyScore() != null ? e.getAccuracyScore() : 0;
            sumActionability += e.getActionabilityScore() != null ? e.getActionabilityScore() : 0;
            sumRelevance += e.getRelevanceScore() != null ? e.getRelevanceScore() : 0;
        }

        resp.setApprovedCount(approved);
        resp.setNeedsWorkCount(needsWork);
        resp.setRejectedCount(rejected);
        resp.setPendingCount(pending);
        resp.setInReviewCount(inReview);

        long count = all.size();
        resp.setAverageCompletenessScore(count > 0 ? sumCompleteness / count : 0);
        resp.setAverageAccuracyScore(count > 0 ? sumAccuracy / count : 0);
        resp.setAverageActionabilityScore(count > 0 ? sumActionability / count : 0);
        resp.setAverageRelevanceScore(count > 0 ? sumRelevance / count : 0);
        resp.setOverallAverageScore(count > 0
                ? (sumCompleteness + sumAccuracy + sumActionability + sumRelevance) / (count * 4) : 0);

        return resp;
    }

    private void validateScores(Integer completeness, Integer accuracy, Integer actionability, Integer relevance) {
        validateSingleScore(completeness, "completenessScore");
        validateSingleScore(accuracy, "accuracyScore");
        validateSingleScore(actionability, "actionabilityScore");
        validateSingleScore(relevance, "relevanceScore");
    }

    private void validateSingleScore(Integer score, String fieldName) {
        if (score == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, fieldName + " 不能为空");
        }
        if (score < 0 || score > 5) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, fieldName + " 必须在 0-5 之间");
        }
    }

    private String computeOverallStatus(CreateKnowledgeQualityReviewRequest request) {
        double avg = (request.getCompletenessScore() + request.getAccuracyScore()
                + request.getActionabilityScore() + request.getRelevanceScore()) / 4.0;
        return computeOverallStatusFromAvg(avg);
    }

    private String computeOverallStatusFromEntity(ToolKnowledgeQualityReviewEntity entity) {
        double avg = (entity.getCompletenessScore() + entity.getAccuracyScore()
                + entity.getActionabilityScore() + entity.getRelevanceScore()) / 4.0;
        return computeOverallStatusFromAvg(avg);
    }

    private String computeOverallStatusFromAvg(double avg) {
        if (avg >= 4.0) return KnowledgeQualityOverallStatus.APPROVED.name();
        if (avg >= 2.0) return KnowledgeQualityOverallStatus.NEEDS_WORK.name();
        return KnowledgeQualityOverallStatus.REJECTED.name();
    }

    private boolean isValidReviewStatusTransition(String oldStatus, String newStatus) {
        if (oldStatus.equals(newStatus)) return true;
        return switch (oldStatus) {
            case "PENDING" -> "IN_REVIEW".equals(newStatus);
            case "IN_REVIEW" -> "COMPLETED".equals(newStatus) || "PENDING".equals(newStatus);
            case "COMPLETED" -> false;
            default -> false;
        };
    }

    private ToolIncidentEntity getIncidentOrThrow(Long incidentId) {
        ToolIncidentEntity incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "事件不存在");
        }
        return incident;
    }

    private KnowledgeQualityReviewResponse toResponse(ToolKnowledgeQualityReviewEntity entity) {
        KnowledgeQualityReviewResponse resp = new KnowledgeQualityReviewResponse();
        resp.setId(entity.getId().toString());
        resp.setProjectId(entity.getProjectId().toString());
        resp.setIncidentId(entity.getIncidentId().toString());
        resp.setKnowledgeDocumentId(entity.getKnowledgeDocumentId() != null ? entity.getKnowledgeDocumentId().toString() : null);
        resp.setRetrospectiveId(entity.getRetrospectiveId() != null ? entity.getRetrospectiveId().toString() : null);
        resp.setCompletenessScore(entity.getCompletenessScore());
        resp.setAccuracyScore(entity.getAccuracyScore());
        resp.setActionabilityScore(entity.getActionabilityScore());
        resp.setRelevanceScore(entity.getRelevanceScore());
        if (entity.getCompletenessScore() != null) {
            double avg = (entity.getCompletenessScore() + entity.getAccuracyScore()
                    + entity.getActionabilityScore() + entity.getRelevanceScore()) / 4.0;
            resp.setAverageScore(avg);
        }
        resp.setReviewStatus(entity.getReviewStatus());
        resp.setOverallStatus(entity.getOverallStatus());
        resp.setChecklistJson(entity.getChecklistJson());
        resp.setReviewComment(entity.getReviewComment());
        resp.setReviewerId(entity.getReviewerId() != null ? entity.getReviewerId().toString() : null);
        resp.setReviewedAt(entity.getReviewedAt());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }
}
