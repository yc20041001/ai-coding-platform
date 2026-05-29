package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceCapacityForecastResponse {
    private String id; private LocalDate snapshotDate; private Integer forecastHorizonDays;
    private String ownerId; private String ownerName; private Integer currentOpenCount;
    private Integer currentOverdueCount; private BigDecimal avgCompletedPerDay;
    private Integer projectedNewItems; private Integer projectedCompletedItems;
    private Integer projectedBacklogCount; private Integer projectedOverdueCount;
    private String capacityRiskLevel; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Integer getForecastHorizonDays() { return forecastHorizonDays; } public void setForecastHorizonDays(Integer v) { this.forecastHorizonDays = v; }
    public String getOwnerId() { return ownerId; } public void setOwnerId(String v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { this.ownerName = v; }
    public Integer getCurrentOpenCount() { return currentOpenCount; } public void setCurrentOpenCount(Integer v) { this.currentOpenCount = v; }
    public Integer getCurrentOverdueCount() { return currentOverdueCount; } public void setCurrentOverdueCount(Integer v) { this.currentOverdueCount = v; }
    public BigDecimal getAvgCompletedPerDay() { return avgCompletedPerDay; } public void setAvgCompletedPerDay(BigDecimal v) { this.avgCompletedPerDay = v; }
    public Integer getProjectedNewItems() { return projectedNewItems; } public void setProjectedNewItems(Integer v) { this.projectedNewItems = v; }
    public Integer getProjectedCompletedItems() { return projectedCompletedItems; } public void setProjectedCompletedItems(Integer v) { this.projectedCompletedItems = v; }
    public Integer getProjectedBacklogCount() { return projectedBacklogCount; } public void setProjectedBacklogCount(Integer v) { this.projectedBacklogCount = v; }
    public Integer getProjectedOverdueCount() { return projectedOverdueCount; } public void setProjectedOverdueCount(Integer v) { this.projectedOverdueCount = v; }
    public String getCapacityRiskLevel() { return capacityRiskLevel; } public void setCapacityRiskLevel(String v) { this.capacityRiskLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
