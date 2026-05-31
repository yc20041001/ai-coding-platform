package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceCopilotTuningSnapshotResponse {
    private String id; private String snapshotWindow; private Integer totalFeedbackCount;
    private BigDecimal acceptanceRate; private BigDecimal dismissalRate;
    private BigDecimal avgFeedbackRating; private String topSuggestionType;
    private String weakestSuggestionType; private String topFocusMode; private String weakestFocusMode;
    private BigDecimal tuningConfidenceScore; private String summaryMarkdown;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSnapshotWindow() { return snapshotWindow; } public void setSnapshotWindow(String v) { this.snapshotWindow = v; }
    public Integer getTotalFeedbackCount() { return totalFeedbackCount; } public void setTotalFeedbackCount(Integer v) { this.totalFeedbackCount = v; }
    public BigDecimal getAcceptanceRate() { return acceptanceRate; } public void setAcceptanceRate(BigDecimal v) { this.acceptanceRate = v; }
    public BigDecimal getDismissalRate() { return dismissalRate; } public void setDismissalRate(BigDecimal v) { this.dismissalRate = v; }
    public BigDecimal getAvgFeedbackRating() { return avgFeedbackRating; } public void setAvgFeedbackRating(BigDecimal v) { this.avgFeedbackRating = v; }
    public String getTopSuggestionType() { return topSuggestionType; } public void setTopSuggestionType(String v) { this.topSuggestionType = v; }
    public String getWeakestSuggestionType() { return weakestSuggestionType; } public void setWeakestSuggestionType(String v) { this.weakestSuggestionType = v; }
    public String getTopFocusMode() { return topFocusMode; } public void setTopFocusMode(String v) { this.topFocusMode = v; }
    public String getWeakestFocusMode() { return weakestFocusMode; } public void setWeakestFocusMode(String v) { this.weakestFocusMode = v; }
    public BigDecimal getTuningConfidenceScore() { return tuningConfidenceScore; } public void setTuningConfidenceScore(BigDecimal v) { this.tuningConfidenceScore = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; } public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
}
