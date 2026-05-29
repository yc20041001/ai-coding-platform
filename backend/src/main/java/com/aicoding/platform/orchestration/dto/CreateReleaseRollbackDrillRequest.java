package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class CreateReleaseRollbackDrillRequest {

    private String planId;
    private String projectId;
    private String releaseLabel;
    private String drillScope;
    private String environmentName;
    private String ownerId;
    private String executorId;
    private LocalDateTime plannedAt;
    private String successCriteria;
    private String rollbackStepsSummary;
    private String blockersSummary;
    private String resultSummary;
    private String evidenceJson;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
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
