package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;

public class GovernanceForecastSummaryResponse {
    private LocalDate snapshotDate; private Integer ownerForecastCount; private Integer criticalOwnerCount;
    private Integer highOwnerCount; private Integer signalCount; private Integer criticalSignalCount;
    private Integer projectCount; private Integer criticalBacklogCount; private Integer riskBacklogCount;
    private Integer totalProjectedBacklog; private Integer totalProjectedOverdue; private String summaryMarkdown;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getOwnerForecastCount() { return ownerForecastCount; } public void setOwnerForecastCount(Integer v) { this.ownerForecastCount = v; }
    public Integer getCriticalOwnerCount() { return criticalOwnerCount; } public void setCriticalOwnerCount(Integer v) { this.criticalOwnerCount = v; }
    public Integer getHighOwnerCount() { return highOwnerCount; } public void setHighOwnerCount(Integer v) { this.highOwnerCount = v; }
    public Integer getSignalCount() { return signalCount; } public void setSignalCount(Integer v) { this.signalCount = v; }
    public Integer getCriticalSignalCount() { return criticalSignalCount; } public void setCriticalSignalCount(Integer v) { this.criticalSignalCount = v; }
    public Integer getProjectCount() { return projectCount; } public void setProjectCount(Integer v) { this.projectCount = v; }
    public Integer getCriticalBacklogCount() { return criticalBacklogCount; } public void setCriticalBacklogCount(Integer v) { this.criticalBacklogCount = v; }
    public Integer getRiskBacklogCount() { return riskBacklogCount; } public void setRiskBacklogCount(Integer v) { this.riskBacklogCount = v; }
    public Integer getTotalProjectedBacklog() { return totalProjectedBacklog; } public void setTotalProjectedBacklog(Integer v) { this.totalProjectedBacklog = v; }
    public Integer getTotalProjectedOverdue() { return totalProjectedOverdue; } public void setTotalProjectedOverdue(Integer v) { this.totalProjectedOverdue = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
}
