package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class GovernanceBacklogDashboardResponse {
    private LocalDate snapshotDate; private Integer projectCount; private Integer healthyCount;
    private Integer watchCount; private Integer riskCount; private Integer criticalCount;
    private List<GovernanceBacklogSnapshotResponse> topGrowingBacklogs;
    private List<GovernanceBacklogSnapshotResponse> topOverdueProjects;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getProjectCount() { return projectCount; } public void setProjectCount(Integer v) { this.projectCount = v; }
    public Integer getHealthyCount() { return healthyCount; } public void setHealthyCount(Integer v) { this.healthyCount = v; }
    public Integer getWatchCount() { return watchCount; } public void setWatchCount(Integer v) { this.watchCount = v; }
    public Integer getRiskCount() { return riskCount; } public void setRiskCount(Integer v) { this.riskCount = v; }
    public Integer getCriticalCount() { return criticalCount; } public void setCriticalCount(Integer v) { this.criticalCount = v; }
    public List<GovernanceBacklogSnapshotResponse> getTopGrowingBacklogs() { return topGrowingBacklogs; }
    public void setTopGrowingBacklogs(List<GovernanceBacklogSnapshotResponse> v) { this.topGrowingBacklogs = v; }
    public List<GovernanceBacklogSnapshotResponse> getTopOverdueProjects() { return topOverdueProjects; }
    public void setTopOverdueProjects(List<GovernanceBacklogSnapshotResponse> v) { this.topOverdueProjects = v; }
}
