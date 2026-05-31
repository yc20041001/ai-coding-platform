package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;

public class GovernanceDraftOptimizationSignalResponse {
    private String id; private String signalType; private String scopeType; private String scopeKey;
    private BigDecimal adoptionRate; private BigDecimal rejectionRate;
    private BigDecimal avgUsefulnessRating; private Integer sampleCount;
    private String signalLevel; private String suggestionText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSignalType() { return signalType; } public void setSignalType(String v) { this.signalType = v; }
    public String getScopeType() { return scopeType; } public void setScopeType(String v) { this.scopeType = v; }
    public String getScopeKey() { return scopeKey; } public void setScopeKey(String v) { this.scopeKey = v; }
    public BigDecimal getAdoptionRate() { return adoptionRate; } public void setAdoptionRate(BigDecimal v) { this.adoptionRate = v; }
    public BigDecimal getRejectionRate() { return rejectionRate; } public void setRejectionRate(BigDecimal v) { this.rejectionRate = v; }
    public BigDecimal getAvgUsefulnessRating() { return avgUsefulnessRating; } public void setAvgUsefulnessRating(BigDecimal v) { this.avgUsefulnessRating = v; }
    public Integer getSampleCount() { return sampleCount; } public void setSampleCount(Integer v) { this.sampleCount = v; }
    public String getSignalLevel() { return signalLevel; } public void setSignalLevel(String v) { this.signalLevel = v; }
    public String getSuggestionText() { return suggestionText; } public void setSuggestionText(String v) { this.suggestionText = v; }
}
