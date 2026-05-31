package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceDraftAdoptionReviewResponse {
    private String id; private String draftPlanId; private String recommendationId; private String operatorId;
    private String operatorName; private String adoptionResult; private String modificationLevel;
    private Integer usefulnessRating; private String reasonCode; private String outcomeNoteText;
    private LocalDateTime reviewedAt; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getDraftPlanId() { return draftPlanId; } public void setDraftPlanId(String v) { this.draftPlanId = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getAdoptionResult() { return adoptionResult; } public void setAdoptionResult(String v) { this.adoptionResult = v; }
    public String getModificationLevel() { return modificationLevel; } public void setModificationLevel(String v) { this.modificationLevel = v; }
    public Integer getUsefulnessRating() { return usefulnessRating; } public void setUsefulnessRating(Integer v) { this.usefulnessRating = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getOutcomeNoteText() { return outcomeNoteText; } public void setOutcomeNoteText(String v) { this.outcomeNoteText = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
