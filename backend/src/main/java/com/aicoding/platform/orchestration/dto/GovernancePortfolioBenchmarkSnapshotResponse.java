package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernancePortfolioBenchmarkSnapshotResponse {
    private String id; private LocalDate snapshotDate; private String benchmarkWindow; private String metricKey;
    private BigDecimal metricValue; private BigDecimal percentileRank; private BigDecimal peerAvg;
    private BigDecimal peerP90; private Integer sampleCount; private String signalLevel; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getBenchmarkWindow() { return benchmarkWindow; } public void setBenchmarkWindow(String v) { this.benchmarkWindow = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public BigDecimal getMetricValue() { return metricValue; } public void setMetricValue(BigDecimal v) { this.metricValue = v; }
    public BigDecimal getPercentileRank() { return percentileRank; } public void setPercentileRank(BigDecimal v) { this.percentileRank = v; }
    public BigDecimal getPeerAvg() { return peerAvg; } public void setPeerAvg(BigDecimal v) { this.peerAvg = v; }
    public BigDecimal getPeerP90() { return peerP90; } public void setPeerP90(BigDecimal v) { this.peerP90 = v; }
    public Integer getSampleCount() { return sampleCount; } public void setSampleCount(Integer v) { this.sampleCount = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
