package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class ReleaseReadinessDashboardResponse {

    private String projectId;
    private String releaseLabel;
    private String decisionStatus;
    private String rolloutStatus;
    private String overallReadinessStatus;
    private Integer blockingIssueCount;
    private Integer warningIssueCount;
    private Integer openIncidentCount;
    private Integer activeAlertCount;
    private Integer highRiskFeedbackCount;
    private Integer costAlertCount;
    private Integer prQualityWarnCount;
    private Double preReleasePassRate;
    private Integer observationVerificationCount;
    private Boolean rollbackRecommended;
    private LocalDateTime lastEvaluatedAt;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getOverallReadinessStatus() { return overallReadinessStatus; }
    public void setOverallReadinessStatus(String overallReadinessStatus) { this.overallReadinessStatus = overallReadinessStatus; }
    public Integer getBlockingIssueCount() { return blockingIssueCount; }
    public void setBlockingIssueCount(Integer blockingIssueCount) { this.blockingIssueCount = blockingIssueCount; }
    public Integer getWarningIssueCount() { return warningIssueCount; }
    public void setWarningIssueCount(Integer warningIssueCount) { this.warningIssueCount = warningIssueCount; }
    public Integer getOpenIncidentCount() { return openIncidentCount; }
    public void setOpenIncidentCount(Integer openIncidentCount) { this.openIncidentCount = openIncidentCount; }
    public Integer getActiveAlertCount() { return activeAlertCount; }
    public void setActiveAlertCount(Integer activeAlertCount) { this.activeAlertCount = activeAlertCount; }
    public Integer getHighRiskFeedbackCount() { return highRiskFeedbackCount; }
    public void setHighRiskFeedbackCount(Integer highRiskFeedbackCount) { this.highRiskFeedbackCount = highRiskFeedbackCount; }
    public Integer getCostAlertCount() { return costAlertCount; }
    public void setCostAlertCount(Integer costAlertCount) { this.costAlertCount = costAlertCount; }
    public Integer getPrQualityWarnCount() { return prQualityWarnCount; }
    public void setPrQualityWarnCount(Integer prQualityWarnCount) { this.prQualityWarnCount = prQualityWarnCount; }
    public Double getPreReleasePassRate() { return preReleasePassRate; }
    public void setPreReleasePassRate(Double preReleasePassRate) { this.preReleasePassRate = preReleasePassRate; }
    public Integer getObservationVerificationCount() { return observationVerificationCount; }
    public void setObservationVerificationCount(Integer observationVerificationCount) { this.observationVerificationCount = observationVerificationCount; }
    public Boolean getRollbackRecommended() { return rollbackRecommended; }
    public void setRollbackRecommended(Boolean rollbackRecommended) { this.rollbackRecommended = rollbackRecommended; }
    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }
}
