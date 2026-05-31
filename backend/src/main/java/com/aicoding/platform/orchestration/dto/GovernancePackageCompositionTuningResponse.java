package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernancePackageCompositionTuningResponse {
    private String id; private String scoreRange; private BigDecimal avgCompleteness;
    private BigDecimal avgAccuracy; private BigDecimal avgOverall; private Integer sampleCount;
    private String tuningLevel; private String suggestionText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getScoreRange() { return scoreRange; } public void setScoreRange(String v) { this.scoreRange = v; }
    public BigDecimal getAvgCompleteness() { return avgCompleteness; } public void setAvgCompleteness(BigDecimal v) { this.avgCompleteness = v; }
    public BigDecimal getAvgAccuracy() { return avgAccuracy; } public void setAvgAccuracy(BigDecimal v) { this.avgAccuracy = v; }
    public BigDecimal getAvgOverall() { return avgOverall; } public void setAvgOverall(BigDecimal v) { this.avgOverall = v; }
    public Integer getSampleCount() { return sampleCount; } public void setSampleCount(Integer v) { this.sampleCount = v; }
    public String getTuningLevel() { return tuningLevel; } public void setTuningLevel(String v) { this.tuningLevel = v; }
    public String getSuggestionText() { return suggestionText; } public void setSuggestionText(String v) { this.suggestionText = v; }
}
