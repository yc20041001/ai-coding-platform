package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class UpdateReleaseRollbackDrillRequest {

    private String drillScope;
    private String environmentName;
    private String ownerId;
    private String executorId;
    private LocalDateTime plannedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationSeconds;
    private String successCriteria;
    private String rollbackStepsSummary;
    private String blockersSummary;
    private String resultSummary;
    private String evidenceJson;

    public String getDrillScope() { return drillScope; }
    public void setDrillScope(String drillScope) { this.drillScope = drillScope; }
    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getExecutorId() { return executorId; }
    public void setExecutorId(String executorId) { this.executorId = executorId; }
    public LocalDateTime getPlannedAt() { return plannedAt; }
    public void setPlannedAt(LocalDateTime plannedAt) { this.plannedAt = plannedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getSuccessCriteria() { return successCriteria; }
    public void setSuccessCriteria(String successCriteria) { this.successCriteria = successCriteria; }
    public String getRollbackStepsSummary() { return rollbackStepsSummary; }
    public void setRollbackStepsSummary(String rollbackStepsSummary) { this.rollbackStepsSummary = rollbackStepsSummary; }
    public String getBlockersSummary() { return blockersSummary; }
    public void setBlockersSummary(String blockersSummary) { this.blockersSummary = blockersSummary; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
}
