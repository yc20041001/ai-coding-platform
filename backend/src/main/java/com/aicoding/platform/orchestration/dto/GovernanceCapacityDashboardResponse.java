package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class GovernanceCapacityDashboardResponse {
    private LocalDate snapshotDate; private Integer ownerCount; private Integer lowRiskCount;
    private Integer watchCount; private Integer highCount; private Integer criticalCount;
    private List<GovernanceCapacityForecastResponse> topRiskOwners;
    private Integer averageProjectedBacklog; private Integer averageProjectedOverdue;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getOwnerCount() { return ownerCount; } public void setOwnerCount(Integer v) { this.ownerCount = v; }
    public Integer getLowRiskCount() { return lowRiskCount; } public void setLowRiskCount(Integer v) { this.lowRiskCount = v; }
    public Integer getWatchCount() { return watchCount; } public void setWatchCount(Integer v) { this.watchCount = v; }
    public Integer getHighCount() { return highCount; } public void setHighCount(Integer v) { this.highCount = v; }
    public Integer getCriticalCount() { return criticalCount; } public void setCriticalCount(Integer v) { this.criticalCount = v; }
    public List<GovernanceCapacityForecastResponse> getTopRiskOwners() { return topRiskOwners; }
    public void setTopRiskOwners(List<GovernanceCapacityForecastResponse> v) { this.topRiskOwners = v; }
    public Integer getAverageProjectedBacklog() { return averageProjectedBacklog; } public void setAverageProjectedBacklog(Integer v) { this.averageProjectedBacklog = v; }
    public Integer getAverageProjectedOverdue() { return averageProjectedOverdue; } public void setAverageProjectedOverdue(Integer v) { this.averageProjectedOverdue = v; }
}
