package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleaseRolloutPlanResponse {

    private String id;
    private String projectId;
    private String releaseLabel;
    private String sourceDecisionId;
    private String rolloutStatus;
    private String rolloutStrategy;
    private String targetEnvironment;
    private String ownerId;
    private String approverId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private Integer observationWindowMinutes;
    private String rollbackTriggerSummary;
    private String successCriteriaSummary;
    private String readinessSummary;
    private Integer stepCount;
    private Integer passedStepCount;
    private Integer failedStepCount;
    private Integer verificationCount;
    private Integer blockingVerificationCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getSourceDecisionId() { return sourceDecisionId; }
    public void setSourceDecisionId(String sourceDecisionId) { this.sourceDecisionId = sourceDecisionId; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getRolloutStrategy() { return rolloutStrategy; }
    public void setRolloutStrategy(String rolloutStrategy) { this.rolloutStrategy = rolloutStrategy; }
    public String getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public LocalDateTime getPlannedStartAt() { return plannedStartAt; }
    public void setPlannedStartAt(LocalDateTime plannedStartAt) { this.plannedStartAt = plannedStartAt; }
    public LocalDateTime getPlannedEndAt() { return plannedEndAt; }
    public void setPlannedEndAt(LocalDateTime plannedEndAt) { this.plannedEndAt = plannedEndAt; }
    public Integer getObservationWindowMinutes() { return observationWindowMinutes; }
    public void setObservationWindowMinutes(Integer observationWindowMinutes) { this.observationWindowMinutes = observationWindowMinutes; }
    public String getRollbackTriggerSummary() { return rollbackTriggerSummary; }
    public void setRollbackTriggerSummary(String rollbackTriggerSummary) { this.rollbackTriggerSummary = rollbackTriggerSummary; }
    public String getSuccessCriteriaSummary() { return successCriteriaSummary; }
    public void setSuccessCriteriaSummary(String successCriteriaSummary) { this.successCriteriaSummary = successCriteriaSummary; }
    public String getReadinessSummary() { return readinessSummary; }
    public void setReadinessSummary(String readinessSummary) { this.readinessSummary = readinessSummary; }
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    public Integer getPassedStepCount() { return passedStepCount; }
    public void setPassedStepCount(Integer passedStepCount) { this.passedStepCount = passedStepCount; }
    public Integer getFailedStepCount() { return failedStepCount; }
    public void setFailedStepCount(Integer failedStepCount) { this.failedStepCount = failedStepCount; }
    public Integer getVerificationCount() { return verificationCount; }
    public void setVerificationCount(Integer verificationCount) { this.verificationCount = verificationCount; }
    public Integer getBlockingVerificationCount() { return blockingVerificationCount; }
    public void setBlockingVerificationCount(Integer blockingVerificationCount) { this.blockingVerificationCount = blockingVerificationCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
