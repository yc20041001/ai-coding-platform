package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GovernanceWorkflowDashboardResponse {

    private LocalDate snapshotDate;
    private Integer totalRecommendationCount;
    private Integer openRecommendationCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer blockedCount;
    private Integer overdueCount;
    private Integer activeWaiverCount;
    private Integer expiredWaiverCount;
    private BigDecimal completionRate;
    private BigDecimal overdueRate;
    private List<GovernanceRecommendationItemResponse> topPriorityItems;
    private List<GovernanceRecommendationItemResponse> topOverdueItems;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getTotalRecommendationCount() { return totalRecommendationCount; }
    public void setTotalRecommendationCount(Integer totalRecommendationCount) { this.totalRecommendationCount = totalRecommendationCount; }
    public Integer getOpenRecommendationCount() { return openRecommendationCount; }
    public void setOpenRecommendationCount(Integer openRecommendationCount) { this.openRecommendationCount = openRecommendationCount; }
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
    public Integer getExpiredWaiverCount() { return expiredWaiverCount; }
    public void setExpiredWaiverCount(Integer expiredWaiverCount) { this.expiredWaiverCount = expiredWaiverCount; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getOverdueRate() { return overdueRate; }
    public void setOverdueRate(BigDecimal overdueRate) { this.overdueRate = overdueRate; }
    public List<GovernanceRecommendationItemResponse> getTopPriorityItems() { return topPriorityItems; }
    public void setTopPriorityItems(List<GovernanceRecommendationItemResponse> topPriorityItems) { this.topPriorityItems = topPriorityItems; }
    public List<GovernanceRecommendationItemResponse> getTopOverdueItems() { return topOverdueItems; }
    public void setTopOverdueItems(List<GovernanceRecommendationItemResponse> topOverdueItems) { this.topOverdueItems = topOverdueItems; }
}
