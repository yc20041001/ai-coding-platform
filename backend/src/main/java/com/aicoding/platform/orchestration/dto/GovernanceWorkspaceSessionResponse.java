package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceWorkspaceSessionResponse {
    private String id; private String operatorId; private String operatorName; private String sessionStatus;
    private String focusMode; private String selectedProjectId; private String selectedRecommendationId;
    private String selectedOwnerId; private String contextSummary; private LocalDateTime startedAt;
    private LocalDateTime endedAt; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getSessionStatus() { return sessionStatus; } public void setSessionStatus(String v) { this.sessionStatus = v; }
    public String getFocusMode() { return focusMode; } public void setFocusMode(String v) { this.focusMode = v; }
    public String getSelectedProjectId() { return selectedProjectId; } public void setSelectedProjectId(String v) { this.selectedProjectId = v; }
    public String getSelectedRecommendationId() { return selectedRecommendationId; } public void setSelectedRecommendationId(String v) { this.selectedRecommendationId = v; }
    public String getSelectedOwnerId() { return selectedOwnerId; } public void setSelectedOwnerId(String v) { this.selectedOwnerId = v; }
    public String getContextSummary() { return contextSummary; } public void setContextSummary(String v) { this.contextSummary = v; }
    public LocalDateTime getStartedAt() { return startedAt; } public void setStartedAt(LocalDateTime v) { this.startedAt = v; }
    public LocalDateTime getEndedAt() { return endedAt; } public void setEndedAt(LocalDateTime v) { this.endedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
