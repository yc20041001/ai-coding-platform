package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernancePackageReviewEvaluationEntity;
import com.aicoding.platform.orchestration.dto.GovernancePackageReviewEvaluationResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernancePackageReviewEvaluationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernancePackageEvaluationService {

    private final GovernancePackageReviewEvaluationMapper mapper;

    public GovernancePackageEvaluationService(GovernancePackageReviewEvaluationMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernancePackageReviewEvaluationResponse recordEvaluation(String packageIdStr, String evaluationResult,
                                                                        int completenessScore, int accuracyScore,
                                                                        String reasonCode, String reviewNotesText) {
        GovernancePackageReviewEvaluationEntity entity = new GovernancePackageReviewEvaluationEntity();
        entity.setPackageId(parseLong(packageIdStr));
        entity.setEvaluationResult(evaluationResult);
        entity.setCompletenessScore(completenessScore); entity.setAccuracyScore(accuracyScore);
        entity.setOverallScore((completenessScore + accuracyScore) / 2);
        entity.setReasonCode(reasonCode); entity.setReviewNotesText(reviewNotesText);
        entity.setReviewedAt(LocalDateTime.now());
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernancePackageReviewEvaluationResponse> listEvaluations() {
        return mapper.selectList(new LambdaQueryWrapper<GovernancePackageReviewEvaluationEntity>()
                .orderByDesc(GovernancePackageReviewEvaluationEntity::getReviewedAt))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernancePackageReviewEvaluationResponse getEvaluation(String idStr) {
        GovernancePackageReviewEvaluationEntity entity = mapper.selectById(parseLong(idStr));
        if (entity == null) throw new com.aicoding.platform.common.exception.BizException(
                com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Package evaluation 不存在");
        return toResponse(entity);
    }

    private GovernancePackageReviewEvaluationResponse toResponse(GovernancePackageReviewEvaluationEntity e) {
        GovernancePackageReviewEvaluationResponse r = new GovernancePackageReviewEvaluationResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setPackageId(e.getPackageId() != null ? e.getPackageId().toString() : null);
        r.setDraftPlanId(e.getDraftPlanId() != null ? e.getDraftPlanId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setEvaluationResult(e.getEvaluationResult());
        r.setCompletenessScore(e.getCompletenessScore()); r.setAccuracyScore(e.getAccuracyScore());
        r.setOverallScore(e.getOverallScore()); r.setReasonCode(e.getReasonCode());
        r.setReviewNotesText(e.getReviewNotesText()); r.setReviewedAt(e.getReviewedAt());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
