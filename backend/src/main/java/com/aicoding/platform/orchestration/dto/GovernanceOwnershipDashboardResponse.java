package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class GovernanceOwnershipDashboardResponse {
    private LocalDate snapshotDate; private Integer ownerCount; private Integer healthyCount;
    private Integer watchCount; private Integer riskCount; private Integer criticalCount;
    private List<GovernanceOwnershipSnapshotResponse> topOverloadedOwners;
    private List<GovernanceOwnershipSnapshotResponse> topHealthyOwners;
    private Integer overallThroughput7d;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getOwnerCount() { return ownerCount; } public void setOwnerCount(Integer v) { this.ownerCount = v; }
    public Integer getHealthyCount() { return healthyCount; } public void setHealthyCount(Integer v) { this.healthyCount = v; }
    public Integer getWatchCount() { return watchCount; } public void setWatchCount(Integer v) { this.watchCount = v; }
    public Integer getRiskCount() { return riskCount; } public void setRiskCount(Integer v) { this.riskCount = v; }
    public Integer getCriticalCount() { return criticalCount; } public void setCriticalCount(Integer v) { this.criticalCount = v; }
    public List<GovernanceOwnershipSnapshotResponse> getTopOverloadedOwners() { return topOverloadedOwners; }
    public void setTopOverloadedOwners(List<GovernanceOwnershipSnapshotResponse> v) { this.topOverloadedOwners = v; }
    public List<GovernanceOwnershipSnapshotResponse> getTopHealthyOwners() { return topHealthyOwners; }
    public void setTopHealthyOwners(List<GovernanceOwnershipSnapshotResponse> v) { this.topHealthyOwners = v; }
    public Integer getOverallThroughput7d() { return overallThroughput7d; } public void setOverallThroughput7d(Integer v) { this.overallThroughput7d = v; }
}
