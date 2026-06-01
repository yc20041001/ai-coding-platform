package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceProgressMapSnapshotResponse {
    private String id; private LocalDate snapshotDate; private String projectId; private String projectName;
    private String metricKey; private BigDecimal baselineScore; private BigDecimal currentScore;
    private BigDecimal targetScore; private BigDecimal progressPercentage;
    private String signalLevel; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public BigDecimal getBaselineScore() { return baselineScore; } public void setBaselineScore(BigDecimal v) { this.baselineScore = v; }
    public BigDecimal getCurrentScore() { return currentScore; } public void setCurrentScore(BigDecimal v) { this.currentScore = v; }
    public BigDecimal getTargetScore() { return targetScore; } public void setTargetScore(BigDecimal v) { this.targetScore = v; }
    public BigDecimal getProgressPercentage() { return progressPercentage; } public void setProgressPercentage(BigDecimal v) { this.progressPercentage = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
