package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_escalation_event")
public class GovernanceEscalationEventEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long recommendationId;
    private Long projectId;
    private String escalationType;
    private String escalationLevel;
    private String eventStatus;
    private String summary;
    private String detail;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getEscalationType() { return escalationType; } public void setEscalationType(String v) { this.escalationType = v; }
    public String getEscalationLevel() { return escalationLevel; } public void setEscalationLevel(String v) { this.escalationLevel = v; }
    public String getEventStatus() { return eventStatus; } public void setEventStatus(String v) { this.eventStatus = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { this.detail = v; }
    public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; } public void setTriggeredAt(LocalDateTime v) { this.triggeredAt = v; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; } public void setAcknowledgedAt(LocalDateTime v) { this.acknowledgedAt = v; }
    public LocalDateTime getResolvedAt() { return resolvedAt; } public void setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
