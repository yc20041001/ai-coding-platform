package com.aicoding.platform.orchestration.application;

import com.aicoding.platform.orchestration.domain.GovernanceOperatorFeedbackEntity;
import com.aicoding.platform.orchestration.dto.GovernanceOperatorFeedbackResponse;
import com.aicoding.platform.orchestration.infrastructure.GovernanceOperatorFeedbackMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernanceOperatorFeedbackService {

    private final GovernanceOperatorFeedbackMapper mapper;

    public GovernanceOperatorFeedbackService(GovernanceOperatorFeedbackMapper mapper) { this.mapper = mapper; }

    @Transactional
    public GovernanceOperatorFeedbackResponse recordFeedback(String sessionIdStr, String feedbackTargetType, int rating,
                                                               Boolean helpfulFlag, Boolean acceptedFlag, String reasonCode,
                                                               String noteText) {
        GovernanceOperatorFeedbackEntity entity = new GovernanceOperatorFeedbackEntity();
        entity.setSessionId(parseLong(sessionIdStr));
        entity.setFeedbackTargetType(feedbackTargetType);
        entity.setFeedbackRating(rating);
        entity.setHelpfulFlag(Boolean.TRUE.equals(helpfulFlag) ? 1 : 0);
        entity.setAcceptedFlag(Boolean.TRUE.equals(acceptedFlag) ? 1 : 0);
        entity.setReasonCode(reasonCode);
        entity.setNoteText(noteText);
        mapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<GovernanceOperatorFeedbackResponse> listFeedback(String sessionIdStr) {
        Long sessionId = parseLong(sessionIdStr);
        LambdaQueryWrapper<GovernanceOperatorFeedbackEntity> w = new LambdaQueryWrapper<>();
        w.eq(GovernanceOperatorFeedbackEntity::getSessionId, sessionId);
        w.orderByDesc(GovernanceOperatorFeedbackEntity::getCreateTime);
        return mapper.selectList(w).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GovernanceOperatorFeedbackResponse> listAllFeedback() {
        return mapper.selectList(new LambdaQueryWrapper<GovernanceOperatorFeedbackEntity>()
                .orderByDesc(GovernanceOperatorFeedbackEntity::getCreateTime).last("LIMIT 100"))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private GovernanceOperatorFeedbackResponse toResponse(GovernanceOperatorFeedbackEntity e) {
        GovernanceOperatorFeedbackResponse r = new GovernanceOperatorFeedbackResponse();
        r.setId(e.getId() != null ? e.getId().toString() : null);
        r.setSessionId(e.getSessionId() != null ? e.getSessionId().toString() : null);
        r.setOperatorId(e.getOperatorId() != null ? e.getOperatorId().toString() : null);
        r.setOperatorName(e.getOperatorName()); r.setSuggestionType(e.getSuggestionType());
        r.setSuggestionId(e.getSuggestionId() != null ? e.getSuggestionId().toString() : null);
        r.setGuidedTaskId(e.getGuidedTaskId() != null ? e.getGuidedTaskId().toString() : null);
        r.setReuseBundleId(e.getReuseBundleId() != null ? e.getReuseBundleId().toString() : null);
        r.setFeedbackTargetType(e.getFeedbackTargetType()); r.setFeedbackRating(e.getFeedbackRating());
        r.setHelpfulFlag(e.getHelpfulFlag() != null && e.getHelpfulFlag() == 1);
        r.setAcceptedFlag(e.getAcceptedFlag() != null && e.getAcceptedFlag() == 1);
        r.setReasonCode(e.getReasonCode()); r.setNoteText(e.getNoteText());
        r.setCreateTime(e.getCreateTime());
        return r;
    }

    private static Long parseLong(String v) { try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; } }
}
