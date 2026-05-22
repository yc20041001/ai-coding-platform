package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolOperatorReviewResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String runId;
    private String toolExecutionId;
    private String toolJobId;
    private String reviewTargetType;
    private String reviewTargetId;
    private String status;
    private String severity;
    private String title;
    private String summary;
    private String resolution;
    private String assigneeId;
    private String createdBy;
    private String resolvedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime resolvedAt;

    public ToolOperatorReviewResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(String toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getToolJobId() { return toolJobId; }
    public void setToolJobId(String toolJobId) { this.toolJobId = toolJobId; }

    public String getReviewTargetType() { return reviewTargetType; }
    public void setReviewTargetType(String reviewTargetType) { this.reviewTargetType = reviewTargetType; }

    public String getReviewTargetId() { return reviewTargetId; }
    public void setReviewTargetId(String reviewTargetId) { this.reviewTargetId = reviewTargetId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
