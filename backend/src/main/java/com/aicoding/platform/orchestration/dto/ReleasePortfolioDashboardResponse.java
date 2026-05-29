package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReleasePortfolioDashboardResponse {

    private LocalDate snapshotDate;
    private Integer projectCount;
    private Integer highConfidenceCount;
    private Integer mediumConfidenceCount;
    private Integer lowConfidenceCount;
    private Integer criticalConfidenceCount;
    private Integer expandNowCount;
    private Integer expandWithGuardrailsCount;
    private Integer holdCount;
    private Integer blockCount;
    private BigDecimal averageConfidenceScore;
    private List<ReleasePortfolioRankingResponse> topProjects;
    private List<ReleasePortfolioRankingResponse> bottomProjects;

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getHighConfidenceCount() { return highConfidenceCount; }
    public void setHighConfidenceCount(Integer highConfidenceCount) { this.highConfidenceCount = highConfidenceCount; }
    public Integer getMediumConfidenceCount() { return mediumConfidenceCount; }
    public void setMediumConfidenceCount(Integer mediumConfidenceCount) { this.mediumConfidenceCount = mediumConfidenceCount; }
    public Integer getLowConfidenceCount() { return lowConfidenceCount; }
    public void setLowConfidenceCount(Integer lowConfidenceCount) { this.lowConfidenceCount = lowConfidenceCount; }
    public Integer getCriticalConfidenceCount() { return criticalConfidenceCount; }
    public void setCriticalConfidenceCount(Integer criticalConfidenceCount) { this.criticalConfidenceCount = criticalConfidenceCount; }
    public Integer getExpandNowCount() { return expandNowCount; }
    public void setExpandNowCount(Integer expandNowCount) { this.expandNowCount = expandNowCount; }
    public Integer getExpandWithGuardrailsCount() { return expandWithGuardrailsCount; }
    public void setExpandWithGuardrailsCount(Integer expandWithGuardrailsCount) { this.expandWithGuardrailsCount = expandWithGuardrailsCount; }
    public Integer getHoldCount() { return holdCount; }
    public void setHoldCount(Integer holdCount) { this.holdCount = holdCount; }
    public Integer getBlockCount() { return blockCount; }
    public void setBlockCount(Integer blockCount) { this.blockCount = blockCount; }
    public BigDecimal getAverageConfidenceScore() { return averageConfidenceScore; }
    public void setAverageConfidenceScore(BigDecimal averageConfidenceScore) { this.averageConfidenceScore = averageConfidenceScore; }
    public List<ReleasePortfolioRankingResponse> getTopProjects() { return topProjects; }
    public void setTopProjects(List<ReleasePortfolioRankingResponse> topProjects) { this.topProjects = topProjects; }
    public List<ReleasePortfolioRankingResponse> getBottomProjects() { return bottomProjects; }
    public void setBottomProjects(List<ReleasePortfolioRankingResponse> bottomProjects) { this.bottomProjects = bottomProjects; }
}
