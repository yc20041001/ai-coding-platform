package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GovernanceWorkflowSummaryResponse {

    private LocalDate snapshotDate;
    private Integer totalRecommendationCount;
    private Integer openCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer blockedCount;
    private Integer overdueCount;
    private Integer activeWaiverCount;
    private BigDecimal completionRate;
    private BigDecimal overdueRate;
    private List<GovernanceRecommendationItemResponse> topPriorityItems;
    private List<GovernanceRecommendationItemResponse> topOverdueItems;
    private String summaryMarkdown;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalRecommendationCount() { return totalRecommendationCount; }
    public void setTotalRecommendationCount(Integer totalRecommendationCount) { this.totalRecommendationCount = totalRecommendationCount; }
    public Integer getOpenCount() { return openCount; }
    public void setOpenCount(Integer openCount) { this.openCount = openCount; }
    public Integer getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(Integer inProgressCount) { this.inProgressCount = inProgressCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getBlockedCount() { return blockedCount; }
    public void setBlockedCount(Integer blockedCount) { this.blockedCount = blockedCount; }
    public Integer getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    public Integer getActiveWaiverCount() { return activeWaiverCount; }
    public void setActiveWaiverCount(Integer activeWaiverCount) { this.activeWaiverCount = activeWaiverCount; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public List<GovernanceRecommendationItemResponse> getTopPriorityItems() { return topPriorityItems; }
    public void setTopPriorityItems(List<GovernanceRecommendationItemResponse> topPriorityItems) { this.topPriorityItems = topPriorityItems; }
    public List<GovernanceRecommendationItemResponse> getTopOverdueItems() { return topOverdueItems; }
    public void setTopOverdueItems(List<GovernanceRecommendationItemResponse> topOverdueItems) { this.topOverdueItems = topOverdueItems; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public void setSummaryMarkdown(String summaryMarkdown) { this.summaryMarkdown = summaryMarkdown; }
}
