package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class PortfolioDriftDashboardResponse {

    private LocalDate snapshotDate;
    private Integer stableCount;
    private Integer watchCount;
    private Integer highCount;
    private Integer criticalCount;
    private List<PortfolioDriftSnapshotResponse> topDriftProjects;
    private String driftTrendSummary;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getStableCount() { return stableCount; }
    public void setStableCount(Integer stableCount) { this.stableCount = stableCount; }
    public Integer getWatchCount() { return watchCount; }
    public void setWatchCount(Integer watchCount) { this.watchCount = watchCount; }
    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer highCount) { this.highCount = highCount; }
    public Integer getCriticalCount() { return criticalCount; }
    public void setCriticalCount(Integer criticalCount) { this.criticalCount = criticalCount; }
    public List<PortfolioDriftSnapshotResponse> getTopDriftProjects() { return topDriftProjects; }
    public void setTopDriftProjects(List<PortfolioDriftSnapshotResponse> topDriftProjects) { this.topDriftProjects = topDriftProjects; }
    public String getDriftTrendSummary() { return driftTrendSummary; }
    public void setDriftTrendSummary(String driftTrendSummary) { this.driftTrendSummary = driftTrendSummary; }
}
