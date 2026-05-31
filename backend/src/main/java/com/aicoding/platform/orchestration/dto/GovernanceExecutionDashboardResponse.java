package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.util.List;

public class GovernanceExecutionDashboardResponse {
    private Integer totalPlanCount; private Integer readyPlanCount; private Integer inProgressPlanCount;
    private Integer blockedPlanCount; private Integer completedPlanCount; private BigDecimal averageCompletionRate;
    private Integer handoffOpenCount; private List<GovernanceExecutionPlanResponse> topBlockedPlans;
    private List<GovernanceExecutionPlanResponse> topNearDuePlans;
    public Integer getTotalPlanCount() { return totalPlanCount; } public void setTotalPlanCount(Integer v) { this.totalPlanCount = v; }
    public Integer getReadyPlanCount() { return readyPlanCount; } public void setReadyPlanCount(Integer v) { this.readyPlanCount = v; }
    public Integer getInProgressPlanCount() { return inProgressPlanCount; } public void setInProgressPlanCount(Integer v) { this.inProgressPlanCount = v; }
    public Integer getBlockedPlanCount() { return blockedPlanCount; } public void setBlockedPlanCount(Integer v) { this.blockedPlanCount = v; }
    public Integer getCompletedPlanCount() { return completedPlanCount; } public void setCompletedPlanCount(Integer v) { this.completedPlanCount = v; }
    public BigDecimal getAverageCompletionRate() { return averageCompletionRate; } public void setAverageCompletionRate(BigDecimal v) { this.averageCompletionRate = v; }
    public Integer getHandoffOpenCount() { return handoffOpenCount; } public void setHandoffOpenCount(Integer v) { this.handoffOpenCount = v; }
    public List<GovernanceExecutionPlanResponse> getTopBlockedPlans() { return topBlockedPlans; }
    public void setTopBlockedPlans(List<GovernanceExecutionPlanResponse> v) { this.topBlockedPlans = v; }
    public List<GovernanceExecutionPlanResponse> getTopNearDuePlans() { return topNearDuePlans; }
    public void setTopNearDuePlans(List<GovernanceExecutionPlanResponse> v) { this.topNearDuePlans = v; }
}
