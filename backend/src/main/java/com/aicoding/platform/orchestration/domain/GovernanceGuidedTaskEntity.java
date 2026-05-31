package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("governance_guided_task")
public class GovernanceGuidedTaskEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long sessionId; private Long recommendationId; private String taskType;
    private String priority; private String taskStatus; private String title; private String summary;
    private String sourceType; private Long sourceId; private String linkedPlaybookKey;
    private String linkedRecipeKey; private Long linkedKnowledgeEntryId; private LocalDateTime dueAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getSessionId() { return sessionId; } public void setSessionId(Long v) { this.sessionId = v; }
    public Long getRecommendationId() { return recommendationId; } public void setRecommendationId(Long v) { this.recommendationId = v; }
    public String getTaskType() { return taskType; } public void setTaskType(String v) { this.taskType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTaskStatus() { return taskStatus; } public void setTaskStatus(String v) { this.taskStatus = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { this.summary = v; }
    public String getSourceType() { return sourceType; } public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceId() { return sourceId; } public void setSourceId(Long v) { this.sourceId = v; }
    public String getLinkedPlaybookKey() { return linkedPlaybookKey; } public void setLinkedPlaybookKey(String v) { this.linkedPlaybookKey = v; }
    public String getLinkedRecipeKey() { return linkedRecipeKey; } public void setLinkedRecipeKey(String v) { this.linkedRecipeKey = v; }
    public Long getLinkedKnowledgeEntryId() { return linkedKnowledgeEntryId; } public void setLinkedKnowledgeEntryId(Long v) { this.linkedKnowledgeEntryId = v; }
    public LocalDateTime getDueAt() { return dueAt; } public void setDueAt(LocalDateTime v) { this.dueAt = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
