package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;
import java.util.List;

public class PredictiveRiskDashboardResponse {
    private LocalDate snapshotDate; private Integer signalCount; private Integer highSignalCount;
    private Integer criticalSignalCount; private Integer ownerRiskSignals; private Integer projectRiskSignals;
    private Integer portfolioRiskSignals; private List<PredictiveRiskSignalResponse> topSignals;
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getSignalCount() { return signalCount; } public void setSignalCount(Integer v) { this.signalCount = v; }
    public Integer getHighSignalCount() { return highSignalCount; } public void setHighSignalCount(Integer v) { this.highSignalCount = v; }
    public Integer getCriticalSignalCount() { return criticalSignalCount; } public void setCriticalSignalCount(Integer v) { this.criticalSignalCount = v; }
    public Integer getOwnerRiskSignals() { return ownerRiskSignals; } public void setOwnerRiskSignals(Integer v) { this.ownerRiskSignals = v; }
    public Integer getProjectRiskSignals() { return projectRiskSignals; } public void setProjectRiskSignals(Integer v) { this.projectRiskSignals = v; }
    public Integer getPortfolioRiskSignals() { return portfolioRiskSignals; } public void setPortfolioRiskSignals(Integer v) { this.portfolioRiskSignals = v; }
    public List<PredictiveRiskSignalResponse> getTopSignals() { return topSignals; }
    public void setTopSignals(List<PredictiveRiskSignalResponse> v) { this.topSignals = v; }
}
