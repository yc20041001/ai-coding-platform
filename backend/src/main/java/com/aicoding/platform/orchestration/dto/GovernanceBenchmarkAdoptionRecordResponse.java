package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GovernanceBenchmarkAdoptionRecordResponse {
    private String id; private String projectId; private String projectName; private String metricKey;
    private String adoptionStatus; private BigDecimal currentScore; private BigDecimal targetScore;
    private String blockerType; private String blockerNote; private String ownerId; private String ownerName;
    private LocalDateTime adoptedAt; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public String getAdoptionStatus() { return adoptionStatus; } public void setAdoptionStatus(String v) { this.adoptionStatus = v; }
    public BigDecimal getCurrentScore() { return currentScore; } public void setCurrentScore(BigDecimal v) { this.currentScore = v; }
    public BigDecimal getTargetScore() { return targetScore; } public void setTargetScore(BigDecimal v) { this.targetScore = v; }
    public String getBlockerType() { return blockerType; } public void setBlockerType(String v) { this.blockerType = v; }
    public String getBlockerNote() { return blockerNote; } public void setBlockerNote(String v) { this.blockerNote = v; }
    public String getOwnerId() { return ownerId; } public void setOwnerId(String v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getAdoptedAt() { return adoptedAt; } public void setAdoptedAt(LocalDateTime v) { this.adoptedAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
