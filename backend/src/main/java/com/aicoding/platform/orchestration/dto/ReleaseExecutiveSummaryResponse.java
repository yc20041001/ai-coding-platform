package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReleaseExecutiveSummaryResponse {

    private String projectId;
    private String planId;
    private String releaseLabel;
    private String decisionStatus;
    private String rolloutStatus;
    private String overallOutcome;
    private BigDecimal confidenceScore;
    private String confidenceLevel;
    private Integer blockingIssueCount;
    private Integer warningIssueCount;
    private Boolean rollbackReady;
    private BigDecimal signoffCompletionRate;
    private Integer openIncidentCount;
    private Integer activeAlertCount;
    private Integer failedVerificationCount;
    private String latestPostmortemOutcome;
    private String summaryText;
    private LocalDateTime lastUpdatedAt;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getReleaseLabel() { return releaseLabel; }
    public void setReleaseLabel(String releaseLabel) { this.releaseLabel = releaseLabel; }
    public String getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(String decisionStatus) { this.decisionStatus = decisionStatus; }
    public String getRolloutStatus() { return rolloutStatus; }
    public void setRolloutStatus(String rolloutStatus) { this.rolloutStatus = rolloutStatus; }
    public String getOverallOutcome() { return overallOutcome; }
    public void setOverallOutcome(String overallOutcome) { this.overallOutcome = overallOutcome; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public Integer getBlockingIssueCount() { return blockingIssueCount; }
    public void setBlockingIssueCount(Integer blockingIssueCount) { this.blockingIssueCount = blockingIssueCount; }
    public Integer getWarningIssueCount() { return warningIssueCount; }
    public void setWarningIssueCount(Integer warningIssueCount) { this.warningIssueCount = warningIssueCount; }
    public Boolean getRollbackReady() { return rollbackReady; }
    public void setRollbackReady(Boolean rollbackReady) { this.rollbackReady = rollbackReady; }
    public BigDecimal getSignoffCompletionRate() { return signoffCompletionRate; }
    public void setSignoffCompletionRate(BigDecimal signoffCompletionRate) { this.signoffCompletionRate = signoffCompletionRate; }
    public Integer getOpenIncidentCount() { return openIncidentCount; }
    public void setOpenIncidentCount(Integer openIncidentCount) { this.openIncidentCount = openIncidentCount; }
    public Integer getActiveAlertCount() { return activeAlertCount; }
    public void setActiveAlertCount(Integer activeAlertCount) { this.activeAlertCount = activeAlertCount; }
    public Integer getFailedVerificationCount() { return failedVerificationCount; }
    public void setFailedVerificationCount(Integer failedVerificationCount) { this.failedVerificationCount = failedVerificationCount; }
    public String getLatestPostmortemOutcome() { return latestPostmortemOutcome; }
    public void setLatestPostmortemOutcome(String latestPostmortemOutcome) { this.latestPostmortemOutcome = latestPostmortemOutcome; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
