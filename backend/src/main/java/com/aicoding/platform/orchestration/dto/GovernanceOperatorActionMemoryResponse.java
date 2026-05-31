package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceOperatorActionMemoryResponse {
    private String id; private String sessionId; private String guidedTaskId; private String recommendationId;
    private String operatorId; private String operatorName; private String actionType;
    private String actionTargetType; private String actionTargetId; private Boolean acceptedFlag;
    private Boolean successFlag; private Integer durationSeconds; private String noteText;
    private LocalDateTime occurredAt; private LocalDateTime createTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getGuidedTaskId() { return guidedTaskId; } public void setGuidedTaskId(String v) { this.guidedTaskId = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { this.actionType = v; }
    public String getActionTargetType() { return actionTargetType; } public void setActionTargetType(String v) { this.actionTargetType = v; }
    public String getActionTargetId() { return actionTargetId; } public void setActionTargetId(String v) { this.actionTargetId = v; }
    public Boolean getAcceptedFlag() { return acceptedFlag; } public void setAcceptedFlag(Boolean v) { this.acceptedFlag = v; }
    public Boolean getSuccessFlag() { return successFlag; } public void setSuccessFlag(Boolean v) { this.successFlag = v; }
    public Integer getDurationSeconds() { return durationSeconds; } public void setDurationSeconds(Integer v) { this.durationSeconds = v; }
    public String getNoteText() { return noteText; } public void setNoteText(String v) { this.noteText = v; }
    public LocalDateTime getOccurredAt() { return occurredAt; } public void setOccurredAt(LocalDateTime v) { this.occurredAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
