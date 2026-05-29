package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class UpdateReleaseRolloutPlanRequest {

    private String rolloutStrategy;
    private String targetEnvironment;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private Integer observationWindowMinutes;
    private String rollbackTriggerSummary;
    private String successCriteriaSummary;
    private String readinessSummary;

    public String getRolloutStrategy() { return rolloutStrategy; }
    public void setRolloutStrategy(String rolloutStrategy) { this.rolloutStrategy = rolloutStrategy; }
    public String getTargetEnvironment() { return targetEnvironment; }
    public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }
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
