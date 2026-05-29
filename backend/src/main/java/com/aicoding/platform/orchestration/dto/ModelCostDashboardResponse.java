package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.util.List;

public class ModelCostDashboardResponse {

    private BigDecimal totalCostToday;
    private BigDecimal totalCostThisWeek;
    private BigDecimal totalCostThisMonth;
    private Long totalRequestsToday;
    private BigDecimal averageCostPerRequest;
    private BigDecimal costChangePercent;
    private List<ModelCostSummaryResponse> topModelsByCost;
    private List<ModelCostAlertResponse> recentAlerts;

    public BigDecimal getTotalCostToday() {
        return totalCostToday;
    }

    public void setTotalCostToday(BigDecimal totalCostToday) {
        this.totalCostToday = totalCostToday;
    }

    public BigDecimal getTotalCostThisWeek() {
        return totalCostThisWeek;
    }

    public void setTotalCostThisWeek(BigDecimal totalCostThisWeek) {
        this.totalCostThisWeek = totalCostThisWeek;
    }

    public BigDecimal getTotalCostThisMonth() {
        return totalCostThisMonth;
    }

    public void setTotalCostThisMonth(BigDecimal totalCostThisMonth) {
        this.totalCostThisMonth = totalCostThisMonth;
    }

    public Long getTotalRequestsToday() {
        return totalRequestsToday;
    }

    public void setTotalRequestsToday(Long totalRequestsToday) {
        this.totalRequestsToday = totalRequestsToday;
    }

    public BigDecimal getAverageCostPerRequest() {
        return averageCostPerRequest;
    }

    public void setAverageCostPerRequest(BigDecimal averageCostPerRequest) {
        this.averageCostPerRequest = averageCostPerRequest;
    }

    public BigDecimal getCostChangePercent() {
        return costChangePercent;
    }

    public void setCostChangePercent(BigDecimal costChangePercent) {
        this.costChangePercent = costChangePercent;
    }

    public List<ModelCostSummaryResponse> getTopModelsByCost() {
        return topModelsByCost;
    }

    public void setTopModelsByCost(List<ModelCostSummaryResponse> topModelsByCost) {
        this.topModelsByCost = topModelsByCost;
    }

    public List<ModelCostAlertResponse> getRecentAlerts() {
        return recentAlerts;
    }

    public void setRecentAlerts(List<ModelCostAlertResponse> recentAlerts) {
        this.recentAlerts = recentAlerts;
    }
}
