package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OrganizationGovernanceSummaryResponse {

    private LocalDate snapshotDate;
    private Integer totalProjectCount;
    private Integer blockCount;
    private Integer warnCount;
    private List<String> topRiskProjects;
    private List<String> topDriftProjects;
    private List<GovernanceRecommendationResponse> topRecommendations;
    private String summaryMarkdown;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalProjectCount() { return totalProjectCount; }
    public void setTotalProjectCount(Integer totalProjectCount) { this.totalProjectCount = totalProjectCount; }
    public Integer getBlockCount() { return blockCount; }
    public void setBlockCount(Integer blockCount) { this.blockCount = blockCount; }
    public Integer getWarnCount() { return warnCount; }
    public void setWarnCount(Integer warnCount) { this.warnCount = warnCount; }
    public List<String> getTopRiskProjects() { return topRiskProjects; }
    public void setTopRiskProjects(List<String> topRiskProjects) { this.topRiskProjects = topRiskProjects; }
    public List<String> getTopDriftProjects() { return topDriftProjects; }
    public void setTopDriftProjects(List<String> topDriftProjects) { this.topDriftProjects = topDriftProjects; }
    public List<GovernanceRecommendationResponse> getTopRecommendations() { return topRecommendations; }
    public void setTopRecommendations(List<GovernanceRecommendationResponse> topRecommendations) { this.topRecommendations = topRecommendations; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public void setSummaryMarkdown(String summaryMarkdown) { this.summaryMarkdown = summaryMarkdown; }
}
