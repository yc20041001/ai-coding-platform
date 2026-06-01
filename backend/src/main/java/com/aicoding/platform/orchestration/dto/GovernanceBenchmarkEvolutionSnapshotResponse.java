package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceBenchmarkEvolutionSnapshotResponse {
    private String id; private LocalDate snapshotDate; private String benchmarkType; private String metricKey;
    private BigDecimal currentValue; private BigDecimal previousValue; private BigDecimal delta;
    private BigDecimal deltaPercentage; private String signalLevel; private Integer sampleCount; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getBenchmarkType() { return benchmarkType; } public void setBenchmarkType(String v) { this.benchmarkType = v; }
    public String getMetricKey() { return metricKey; } public void setMetricKey(String v) { this.metricKey = v; }
    public BigDecimal getCurrentValue() { return currentValue; } public void setCurrentValue(BigDecimal v) { this.currentValue = v; }
    public BigDecimal getPreviousValue() { return previousValue; } public void setPreviousValue(BigDecimal v) { this.previousValue = v; }
    public BigDecimal getDelta() { return delta; } public void setDelta(BigDecimal v) { this.delta = v; }
    public BigDecimal getDeltaPercentage() { return deltaPercentage; } public void setDeltaPercentage(BigDecimal v) { this.deltaPercentage = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public Integer getSampleCount() { return sampleCount; } public void setSampleCount(Integer v) { this.sampleCount = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
