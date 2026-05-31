package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceHandoffChecklistResponse {
    private String id; private String recommendationId; private String executionPlanId;
    private String fromOwnerId; private String fromOwnerName; private String toOwnerId; private String toOwnerName;
    private String checklistStatus; private String checklistItemsJson; private String handoffNote;
    private LocalDateTime handedOffAt; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getExecutionPlanId() { return executionPlanId; } public void setExecutionPlanId(String v) { this.executionPlanId = v; }
    public String getFromOwnerId() { return fromOwnerId; } public void setFromOwnerId(String v) { this.fromOwnerId = v; }
    public String getFromOwnerName() { return fromOwnerName; } public void setFromOwnerName(String v) { this.fromOwnerName = v; }
    public String getToOwnerId() { return toOwnerId; } public void setToOwnerId(String v) { this.toOwnerId = v; }
    public String getToOwnerName() { return toOwnerName; } public void setToOwnerName(String v) { this.toOwnerName = v; }
    public String getChecklistStatus() { return checklistStatus; } public void setChecklistStatus(String v) { this.checklistStatus = v; }
    public String getChecklistItemsJson() { return checklistItemsJson; } public void setChecklistItemsJson(String v) { this.checklistItemsJson = v; }
    public String getHandoffNote() { return handoffNote; } public void setHandoffNote(String v) { this.handoffNote = v; }
    public LocalDateTime getHandedOffAt() { return handedOffAt; } public void setHandedOffAt(LocalDateTime v) { this.handedOffAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
