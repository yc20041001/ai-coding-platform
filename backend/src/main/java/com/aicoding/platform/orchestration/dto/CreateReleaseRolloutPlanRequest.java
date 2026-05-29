package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class CreateReleaseRolloutPlanRequest {

    private String projectId;
    private String releaseLabel;
    private String sourceDecisionId;
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

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getSourceDecisionId() { return sourceDecisionId; }
    public void setSourceDecisionId(String sourceDecisionId) { this.sourceDecisionId = sourceDecisionId; }
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
}
