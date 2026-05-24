package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ToolIncidentResponse {

    private String id;
    private String projectId;
    private String taskId;
    private String runId;
    private String toolExecutionId;
    private String toolJobId;
    private String operatorReviewId;
    private String sourceType;
    private String sourceId;
    private String severity;
    private String status;
    private String title;
    private String summary;
    private String resolution;
    private String assigneeId;
    private String createdBy;
    private String acknowledgedBy;
    private String resolvedBy;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer slaMinutes;
    private LocalDateTime dueAt;
    private LocalDateTime breachedAt;
    private String slaStatus;
    private Integer escalationLevel;

    public ToolIncidentResponse() {}

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

    public String getOperatorReviewId() { return operatorReviewId; }
    public void setOperatorReviewId(String operatorReviewId) { this.operatorReviewId = operatorReviewId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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

    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(Integer slaMinutes) { this.slaMinutes = slaMinutes; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public LocalDateTime getBreachedAt() { return breachedAt; }
    public void setBreachedAt(LocalDateTime breachedAt) { this.breachedAt = breachedAt; }

    public String getSlaStatus() { return slaStatus; }
    public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }

    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
}
