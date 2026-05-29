package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;

public class GovernanceOperationsSummaryResponse {
    private LocalDate snapshotDate; private Integer slaPolicyCount; private Integer openEscalationCount;
    private Integer highEscalationCount; private Integer criticalEscalationCount; private Integer healthyOwnerCount;
    private Integer watchOwnerCount; private Integer riskOwnerCount; private Integer criticalOwnerCount;
    private Integer overdueRecommendationCount; private Integer waiverExpiringSoonCount;
    private Integer overallThroughput7d; private String summaryMarkdown;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getSlaPolicyCount() { return slaPolicyCount; } public void setSlaPolicyCount(Integer v) { this.slaPolicyCount = v; }
    public Integer getOpenEscalationCount() { return openEscalationCount; } public void setOpenEscalationCount(Integer v) { this.openEscalationCount = v; }
    public Integer getHighEscalationCount() { return highEscalationCount; } public void setHighEscalationCount(Integer v) { this.highEscalationCount = v; }
    public Integer getCriticalEscalationCount() { return criticalEscalationCount; } public void setCriticalEscalationCount(Integer v) { this.criticalEscalationCount = v; }
    public Integer getHealthyOwnerCount() { return healthyOwnerCount; } public void setHealthyOwnerCount(Integer v) { this.healthyOwnerCount = v; }
    public Integer getWatchOwnerCount() { return watchOwnerCount; } public void setWatchOwnerCount(Integer v) { this.watchOwnerCount = v; }
    public Integer getRiskOwnerCount() { return riskOwnerCount; } public void setRiskOwnerCount(Integer v) { this.riskOwnerCount = v; }
    public Integer getCriticalOwnerCount() { return criticalOwnerCount; } public void setCriticalOwnerCount(Integer v) { this.criticalOwnerCount = v; }
    public Integer getOverdueRecommendationCount() { return overdueRecommendationCount; } public void setOverdueRecommendationCount(Integer v) { this.overdueRecommendationCount = v; }
    public Integer getWaiverExpiringSoonCount() { return waiverExpiringSoonCount; } public void setWaiverExpiringSoonCount(Integer v) { this.waiverExpiringSoonCount = v; }
    public Integer getOverallThroughput7d() { return overallThroughput7d; } public void setOverallThroughput7d(Integer v) { this.overallThroughput7d = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
}
