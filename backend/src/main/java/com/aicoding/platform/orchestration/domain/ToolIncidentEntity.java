package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_incident")
public class ToolIncidentEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long taskId;
    private Long runId;
    private Long toolExecutionId;
    private Long toolJobId;
    private Long operatorReviewId;
    private String sourceType;
    private Long sourceId;
    private String severity;
    private String status;
    private String title;
    private String summary;
    private String resolution;
    private Long assigneeId;
    private Long createdBy;
    private Long acknowledgedBy;
    private Long resolvedBy;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private Integer slaMinutes;
    private LocalDateTime dueAt;
    private LocalDateTime breachedAt;
    private String slaStatus;
    private Integer escalationLevel;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public ToolIncidentEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(Long toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public Long getToolJobId() { return toolJobId; }
    public void setToolJobId(Long toolJobId) { this.toolJobId = toolJobId; }

    public Long getOperatorReviewId() { return operatorReviewId; }
    public void setOperatorReviewId(Long operatorReviewId) { this.operatorReviewId = operatorReviewId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

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

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
