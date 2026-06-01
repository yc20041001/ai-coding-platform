package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceUpliftMeasurementSnapshotResponse {
    private String id; private LocalDate snapshotDate; private String projectId; private String projectName;
    private String campaignKey; private String metricKey; private BigDecimal beforeScore;
    private BigDecimal afterScore; private BigDecimal uplift; private String upliftLevel; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getProjectId() { return projectId; } public void setProjectId(String v) { this.projectId = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getCampaignKey() { return campaignKey; } public void setCampaignKey(String v) { this.campaignKey = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public BigDecimal getBeforeScore() { return beforeScore; } public void setBeforeScore(BigDecimal v) { this.beforeScore = v; }
    public BigDecimal getAfterScore() { return afterScore; } public void setAfterScore(BigDecimal v) { this.afterScore = v; }
    public BigDecimal getUplift() { return uplift; } public void setUplift(BigDecimal v) { this.uplift = v; }
    public String getUpliftLevel() { return upliftLevel; } public void setUpliftLevel(String v) { this.upliftLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
