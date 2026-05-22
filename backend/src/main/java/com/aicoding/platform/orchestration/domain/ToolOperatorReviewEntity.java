package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("tool_operator_review")
public class ToolOperatorReviewEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long taskId;
    private Long runId;
    private Long toolExecutionId;
    private Long toolJobId;
    private String reviewTargetType;
    private Long reviewTargetId;
    private String status;
    private String severity;
    private String title;
    private String summary;
    private String resolution;
    private Long assigneeId;
    private Long createdBy;
    private Long resolvedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private LocalDateTime resolvedAt;

    public ToolOperatorReviewEntity() {}

    // Convenience lambda getters for LambdaQueryWrapper
    public static SFunction<ToolOperatorReviewEntity, Long> GET_PROJECT_ID = ToolOperatorReviewEntity::getProjectId;
    public static SFunction<ToolOperatorReviewEntity, Long> GET_REVIEW_TARGET_ID = ToolOperatorReviewEntity::getReviewTargetId;
    public static SFunction<ToolOperatorReviewEntity, String> GET_REVIEW_TARGET_TYPE = ToolOperatorReviewEntity::getReviewTargetType;
    public static SFunction<ToolOperatorReviewEntity, String> GET_STATUS = ToolOperatorReviewEntity::getStatus;
    public static SFunction<ToolOperatorReviewEntity, String> GET_SEVERITY = ToolOperatorReviewEntity::getSeverity;
    public static SFunction<ToolOperatorReviewEntity, LocalDateTime> GET_CREATE_TIME = ToolOperatorReviewEntity::getCreateTime;

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

    public String getReviewTargetType() { return reviewTargetType; }
    public void setReviewTargetType(String reviewTargetType) { this.reviewTargetType = reviewTargetType; }

    public Long getReviewTargetId() { return reviewTargetId; }
    public void setReviewTargetId(Long reviewTargetId) { this.reviewTargetId = reviewTargetId; }

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

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
