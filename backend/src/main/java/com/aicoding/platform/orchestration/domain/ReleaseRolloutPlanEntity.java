package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("release_rollout_plan")
public class ReleaseRolloutPlanEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;

    private String releaseLabel;

    private Long sourceDecisionId;

    private String rolloutStatus;

    private String rolloutStrategy;

    private String targetEnvironment;

    private Long ownerId;

    private Long approverId;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    private Integer observationWindowMinutes;

    private String rollbackTriggerSummary;

    private String successCriteriaSummary;

    private String readinessSummary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getReleaseLabel() {
        return releaseLabel;
    }

    public void setReleaseLabel(String releaseLabel) {
        this.releaseLabel = releaseLabel;
    }

    public Long getSourceDecisionId() {
        return sourceDecisionId;
    }

    public void setSourceDecisionId(Long sourceDecisionId) {
        this.sourceDecisionId = sourceDecisionId;
    }

    public String getRolloutStatus() {
        return rolloutStatus;
    }

    public void setRolloutStatus(String rolloutStatus) {
        this.rolloutStatus = rolloutStatus;
    }

    public String getRolloutStrategy() {
        return rolloutStrategy;
    }

    public void setRolloutStrategy(String rolloutStrategy) {
        this.rolloutStrategy = rolloutStrategy;
    }

    public String getTargetEnvironment() {
        return targetEnvironment;
    }

    public void setTargetEnvironment(String targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(LocalDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(LocalDateTime plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public Integer getObservationWindowMinutes() {
        return observationWindowMinutes;
    }

    public void setObservationWindowMinutes(Integer observationWindowMinutes) {
        this.observationWindowMinutes = observationWindowMinutes;
    }

    public String getRollbackTriggerSummary() {
        return rollbackTriggerSummary;
    }

    public void setRollbackTriggerSummary(String rollbackTriggerSummary) {
        this.rollbackTriggerSummary = rollbackTriggerSummary;
    }

    public String getSuccessCriteriaSummary() {
        return successCriteriaSummary;
    }

    public void setSuccessCriteriaSummary(String successCriteriaSummary) {
        this.successCriteriaSummary = successCriteriaSummary;
    }

    public String getReadinessSummary() {
        return readinessSummary;
    }

    public void setReadinessSummary(String readinessSummary) {
        this.readinessSummary = readinessSummary;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
