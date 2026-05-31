package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceAdaptiveGuidanceSignalResponse {
    private String id; private String signalType; private String focusMode; private String category;
    private String suggestionType; private String recommendationPriority;
    private BigDecimal acceptanceRate; private BigDecimal completionRate;
    private BigDecimal avgFeedbackRating; private BigDecimal weightScore;
    private String signalLevel; private String rationaleText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSignalType() { return signalType; } public void setSignalType(String v) { this.signalType = v; }
    public String getFocusMode() { return focusMode; } public void setFocusMode(String v) { this.focusMode = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getRecommendationPriority() { return recommendationPriority; } public void setRecommendationPriority(String v) { this.recommendationPriority = v; }
    public BigDecimal getAcceptanceRate() { return acceptanceRate; } public void setAcceptanceRate(BigDecimal v) { this.acceptanceRate = v; }
    public BigDecimal getCompletionRate() { return completionRate; } public void setCompletionRate(BigDecimal v) { this.completionRate = v; }
    public BigDecimal getAvgFeedbackRating() { return avgFeedbackRating; } public void setAvgFeedbackRating(BigDecimal v) { this.avgFeedbackRating = v; }
    public BigDecimal getWeightScore() { return weightScore; } public void setWeightScore(BigDecimal v) { this.weightScore = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
}
