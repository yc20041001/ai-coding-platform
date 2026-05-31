package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceAssistiveActionQualityReviewEntity;
import com.aicoding.platform.orchestration.dto.GovernanceAssistiveActionQualityReviewResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceAssistiveActionQualityReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceAssistiveQualityService {

    private final GovernanceAssistiveActionQualityReviewMapper mapper;

    public GovernanceAssistiveQualityService(GovernanceAssistiveActionQualityReviewMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceAssistiveActionQualityReviewResponse recordReview(String assistiveActionIdStr, String draftPlanIdStr,
                                                                         String outcomeResult, int usefulnessRating,
                                                                         String reasonCode, String feedbackText) {
        GovernanceAssistiveActionQualityReviewEntity entity = new GovernanceAssistiveActionQualityReviewEntity();
        entity.setAssistiveActionId(parseLong(assistiveActionIdStr));
        entity.setDraftPlanId(draftPlanIdStr != null ? parseLong(draftPlanIdStr) : Long.valueOf(0));
        entity.setOutcomeResult(outcomeResult); entity.setUsefulnessRating(usefulnessRating);
        entity.setReasonCode(reasonCode); entity.setFeedbackText(feedbackText);
        entity.setReviewedAt(LocalDateTime.now());
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceAssistiveActionQualityReviewResponse> listReviews() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceAssistiveActionQualityReviewEntity>()
                .orderByDesc(GovernanceAssistiveActionQualityReviewEntity::getReviewedAt))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceAssistiveActionQualityReviewResponse getReview(String idStr) {
        GovernanceAssistiveActionQualityReviewEntity entity = mapper.selectById(parseLong(idStr));
        if (entity == null) throw new com.aicoding.platform.common.exception.BizException(
                com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Assistive quality review 不存在");
        return toResponse(entity);
    }

    private GovernanceAssistiveActionQualityReviewResponse toResponse(GovernanceAssistiveActionQualityReviewEntity e) {
        GovernanceAssistiveActionQualityReviewResponse r = new GovernanceAssistiveActionQualityReviewResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setAssistiveActionId(e.getAssistiveActionId() != null ? e.getAssistiveActionId().toString() : null);
        r.setDraftPlanId(e.getDraftPlanId() != null ? e.getDraftPlanId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setOutcomeResult(e.getOutcomeResult());
        r.setUsefulnessRating(e.getUsefulnessRating()); r.setReasonCode(e.getReasonCode());
        r.setFeedbackText(e.getFeedbackText()); r.setReviewedAt(e.getReviewedAt());
        r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.valueOf(v); } catch (NumberFormatException e) { return 0L; } }
}
