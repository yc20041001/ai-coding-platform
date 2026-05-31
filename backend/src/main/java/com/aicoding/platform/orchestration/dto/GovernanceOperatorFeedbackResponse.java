package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceOperatorFeedbackResponse {
    private String id; private String sessionId; private String operatorId; private String operatorName;
    private String suggestionType; private String suggestionId; private String guidedTaskId;
    private String reuseBundleId; private String feedbackTargetType; private Integer feedbackRating;
    private Boolean helpfulFlag; private Boolean acceptedFlag; private String reasonCode;
    private String noteText; private LocalDateTime createTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getSuggestionId() { return suggestionId; } public void setSuggestionId(String v) { this.suggestionId = v; }
    public String getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(String v) { this.guidedTaskId = v; }
    public String getReuseBundleId() { return reuseBundleId; } public void setReuseBundleId(String v) { this.reuseBundleId = v; }
    public String getFeedbackTargetType() { return feedbackTargetType; } public void setFeedbackTargetType(String v) { this.feedbackTargetType = v; }
    public Integer getFeedbackRating() { return feedbackRating; } public void setFeedbackRating(Integer v) { this.feedbackRating = v; }
    public Boolean getHelpfulFlag() { return helpfulFlag; } public void setHelpfulFlag(Boolean v) { this.helpfulFlag = v; }
    public Boolean getAcceptedFlag() { return acceptedFlag; } public void setAcceptedFlag(Boolean v) { this.acceptedFlag = v; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String v) { this.reasonCode = v; }
    public String getNoteText() { return noteText; } public void setNoteText(String v) { this.noteText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
