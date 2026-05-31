package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceAssistiveActionQualityReviewResponse {
    private String id; private String assistiveActionId; private String draftPlanId; private String operatorId;
    private String operatorName; private String outcomeResult; private Integer usefulnessRating;
    private String reasonCode; private String feedbackText; private LocalDateTime reviewedAt;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getAssistiveActionId() { return assistiveActionId; } public void setAssistiveActionId(String v) { this.assistiveActionId = v; }
    public String getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(String v) { this.draftPlanId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getOutcomeResult() { return outcomeResult; } public void setOutcomeResult(String v) { this.outcomeResult = v; }
    public Integer getUsefulnessRating() { return usefulnessRating; } public void setUsefulnessRating(Integer v) { this.usefulnessRating = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getFeedbackText() { return feedbackText; } public void setFeedbackText(String v) { this.feedbackText = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
