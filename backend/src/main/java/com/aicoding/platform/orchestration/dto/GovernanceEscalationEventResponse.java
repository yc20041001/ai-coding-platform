package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceEscalationEventResponse {
    private String id; private String recommendationId; private String projectId;
    private String escalationType; private String escalationLevel; private String eventStatus;
    private String summary; private String detail; private String ownerId; private String ownerName;
    private LocalDateTime triggeredAt; private LocalDateTime acknowledgedAt; private LocalDateTime resolvedAt; private LocalDateTime createTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getEscalationType() { return escalationType; } public void setEscalationType(String v) { this.escalationType = v; }
    public String getEscalationLevel() { return escalationLevel; } public void setEscalationLevel(String v) { this.escalationLevel = v; }
    public String getEventStatus() { return eventStatus; } public void setEventStatus(String v) { this.eventStatus = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { this.detail = v; }
    public String getOwnerId() { return ownerId; } public void setOwnerId(String v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; } public void setTriggeredAt(LocalDateTime v) { this.triggeredAt = v; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; } public void setAcknowledgedAt(LocalDateTime v) { this.acknowledgedAt = v; }
    public LocalDateTime getResolvedAt() { return resolvedAt; } public void setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
