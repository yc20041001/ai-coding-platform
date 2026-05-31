package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceWorkspaceSessionInsightResponse {
    private String id; private String sessionId; private String operatorId; private String operatorName;
    private String insightWindow; private Integer totalActions; private Integer acceptedRecommendationCount;
    private Integer dismissedRecommendationCount; private Integer completedGuidedTaskCount;
    private Integer blockedGuidedTaskCount; private Integer avgActionDurationSeconds;
    private BigDecimal productivityScore; private String dominantActionPattern; private String summaryMarkdown;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { this.sessionId = v; }
    public String getOperatorId() { return operatorId; } public void setOperatorId(String v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { this.operatorName = v; }
    public String getInsightWindow() { return insightWindow; } public void setInsightWindow(String v) { this.insightWindow = v; }
    public Integer getTotalActions() { return totalActions; } public void setTotalActions(Integer v) { this.totalActions = v; }
    public Integer getAcceptedRecommendationCount() { return acceptedRecommendationCount; } public void setAcceptedRecommendationCount(Integer v) { this.acceptedRecommendationCount = v; }
    public Integer getDismissedRecommendationCount() { return dismissedRecommendationCount; } public void setDismissedRecommendationCount(Integer v) { this.dismissedRecommendationCount = v; }
    public Integer getCompletedGuidedTaskCount() { return completedGuidedTaskCount; } public void setCompletedGuidedTaskCount(Integer v) { this.completedGuidedTaskCount = v; }
    public Integer getBlockedGuidedTaskCount() { return blockedGuidedTaskCount; } public void setBlockedGuidedTaskCount(Integer v) { this.blockedGuidedTaskCount = v; }
    public Integer getAvgActionDurationSeconds() { return avgActionDurationSeconds; } public void setAvgActionDurationSeconds(Integer v) { this.avgActionDurationSeconds = v; }
    public BigDecimal getProductivityScore() { return productivityScore; } public void setProductivityScore(BigDecimal v) { this.productivityScore = v; }
    public String getDominantActionPattern() { return dominantActionPattern; } public void setDominantActionPattern(String v) { this.dominantActionPattern = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
}
