package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernanceCampaignEffectivenessRankingResponse {
    private String id; private LocalDate snapshotDate; private String campaignKey; private String campaignName;
    private String rankingWindow; private BigDecimal avgUplift; private Integer projectCount;
    private String effectivenessLevel; private Integer rankPosition; private String summaryText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getCampaignKey() { return campaignKey; } public void setCampaignKey(String v) { this.campaignKey = v; }
    public String getCampaignName() { return campaignName; } public void setCampaignName(String v) { this.campaignName = v; }
    public String getRankingWindow() { return rankingWindow; } public void setRankingWindow(String v) { this.rankingWindow = v; }
    public BigDecimal getAvgUplift() { return avgUplift; } public void setAvgUplift(BigDecimal v) { this.avgUplift = v; }
    public Integer getProjectCount() { return projectCount; } public void setProjectCount(Integer v) { this.projectCount = v; }
    public String getEffectivenessLevel() { return effectivenessLevel; } public void setEffectivenessLevel(String v) { this.effectivenessLevel = v; }
    public Integer getRankPosition() { return rankPosition; } public void setRankPosition(Integer v) { this.rankPosition = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
}
