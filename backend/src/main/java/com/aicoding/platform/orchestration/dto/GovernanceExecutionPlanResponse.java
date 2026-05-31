package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GovernanceExecutionPlanResponse {
    private String id; private String recommendationId; private String projectId; private String planStatus;
    private String templateKey; private String ownerId; private String ownerName; private LocalDateTime dueAt;
    private String stepsJson; private BigDecimal completionRate; private String summaryText;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getPlanStatus() { return planStatus; } public void setPlanStatus(String v) { this.planStatus = v; }
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public String getOwnerId() { return ownerId; } public void setOwnerId(String v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public LocalDateTime getDueAt() { return dueAt; } public void setDueAt(LocalDateTime v) { this.dueAt = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public BigDecimal getCompletionRate() { return completionRate; } public void setCompletionRate(BigDecimal v) { this.completionRate = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
