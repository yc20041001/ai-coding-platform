package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class ReleasePortfolioRankingResponse {

    private String projectId;
    private String projectName;
    private String latestReleaseLabel;
    private BigDecimal confidenceScore;
    private String confidenceLevel;
    private Integer portfolioRank;
    private String expansionRecommendation;
    private Integer blockingIssueCount;
    private Integer warningIssueCount;
    private Boolean rollbackReady;
    private BigDecimal signoffCompletionRate;
    private String summaryText;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getLatestReleaseLabel() { return latestReleaseLabel; }
    public void setLatestReleaseLabel(String latestReleaseLabel) { this.latestReleaseLabel = latestReleaseLabel; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public Integer getPortfolioRank() { return portfolioRank; }
    public void setPortfolioRank(Integer portfolioRank) { this.portfolioRank = portfolioRank; }
    public String getExpansionRecommendation() { return expansionRecommendation; }
    public void setExpansionRecommendation(String expansionRecommendation) { this.expansionRecommendation = expansionRecommendation; }
    public Integer getBlockingIssueCount() { return blockingIssueCount; }
    public void setBlockingIssueCount(Integer blockingIssueCount) { this.blockingIssueCount = blockingIssueCount; }
    public Integer getWarningIssueCount() { return warningIssueCount; }
    public void setWarningIssueCount(Integer warningIssueCount) { this.warningIssueCount = warningIssueCount; }
    public Boolean getRollbackReady() { return rollbackReady; }
    public void setRollbackReady(Boolean rollbackReady) { this.rollbackReady = rollbackReady; }
    public BigDecimal getSignoffCompletionRate() { return signoffCompletionRate; }
    public void setSignoffCompletionRate(BigDecimal signoffCompletionRate) { this.signoffCompletionRate = signoffCompletionRate; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
}
