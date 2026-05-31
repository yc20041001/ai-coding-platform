package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernanceGuidedTaskResponse {
    private String id; private String sessionId; private String recommendationId; private String taskType;
    private String priority; private String taskStatus; private String title; private String summary;
    private String sourceType; private String sourceId; private String linkedPlaybookKey;
    private String linkedRecipeKey; private String linkedKnowledgeEntryId; private LocalDateTime dueAt;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getRecommendationId() { return recommendationId; } public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getTaskType() { return taskType; } public void setTaskType(String v) { this.taskType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTaskStatus() { return taskStatus; } public void setTaskStatus(String v) { this.taskStatus = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getSourceType() { return sourceType; } public void setSourceType(String v) { this.sourceType = v; }
    public String getSourceId() { return sourceId; } public void setSourceId(String v) { this.sourceId = v; }
    public String getLinkedPlaybookKey() { return linkedPlaybookKey; } public void setLinkedPlaybookKey(String v) { this.linkedPlaybookKey = v; }
    public String getLinkedRecipeKey() { return linkedRecipeKey; } public void setLinkedRecipeKey(String v) { this.linkedRecipeKey = v; }
    public String getLinkedKnowledgeEntryId() { return linkedKnowledgeEntryId; } public void setLinkedKnowledgeEntryId(String v) { this.linkedKnowledgeEntryId = v; }
    public LocalDateTime getDueAt() { return dueAt; } public void setDueAt(LocalDateTime v) { this.dueAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
