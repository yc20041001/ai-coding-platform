package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceDraftAdoptionReviewEntity;
import com.aicoding.platform.orchestration.dto.GovernanceDraftAdoptionReviewResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceDraftAdoptionReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceDraftOutcomeReviewService {

    private final GovernanceDraftAdoptionReviewMapper mapper;

    public GovernanceDraftOutcomeReviewService(GovernanceDraftAdoptionReviewMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceDraftAdoptionReviewResponse recordReview(String draftPlanIdStr, String adoptionResult,
                                                                String modificationLevel, int usefulnessRating,
                                                                String reasonCode, String outcomeNoteText) {
        GovernanceDraftAdoptionReviewEntity entity = new GovernanceDraftAdoptionReviewEntity();
        entity.setDraftPlanId(parseLong(draftPlanIdStr));
        entity.setAdoptionResult(adoptionResult);
        entity.setModificationLevel(modificationLevel != null ? modificationLevel : "NONE");
        entity.setUsefulnessRating(usefulnessRating);
        entity.setReasonCode(reasonCode); entity.setOutcomeNoteText(outcomeNoteText);
        entity.setReviewedAt(LocalDateTime.now());
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceDraftAdoptionReviewResponse> listReviews() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceDraftAdoptionReviewEntity>()
                .orderByDesc(GovernanceDraftAdoptionReviewEntity::getReviewedAt))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GovernanceDraftAdoptionReviewResponse getReview(String idStr) {
        return toResponse(findEntity(idStr));
    }

    private GovernanceDraftAdoptionReviewEntity findEntity(String idStr) {
        Long id = parseLong(idStr);
        GovernanceDraftAdoptionReviewEntity entity = mapper.selectById(id);
        if (entity == null) throw new com.aicoding.platform.common.exception.BizException(
                com.aicoding.platform.common.exception.ErrorCode.NOT_FOUND, "Adoption review 不存在");
        return entity;
    }

    private GovernanceDraftAdoptionReviewResponse toResponse(GovernanceDraftAdoptionReviewEntity e) {
        GovernanceDraftAdoptionReviewResponse r = new GovernanceDraftAdoptionReviewResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setDraftPlanId(e.getDraftPlanId() != null ? e.getDraftPlanId().toString() : null);
        r.setRecommendationId(e.getRecommendationId() != null ? e.getRecommendationId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setAdoptionResult(e.getAdoptionResult());
        r.setModificationLevel(e.getModificationLevel()); r.setUsefulnessRating(e.getUsefulnessRating());
        r.setReasonCode(e.getReasonCode()); r.setOutcomeNoteText(e.getOutcomeNoteText());
        r.setReviewedAt(e.getReviewedAt()); r.setCreateTime(e.getCreateTime()); r.setUpdateTime(e.getUpdateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
