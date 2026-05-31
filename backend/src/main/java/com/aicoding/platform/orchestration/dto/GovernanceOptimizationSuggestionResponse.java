package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;

public class GovernanceOptimizationSuggestionResponse {
    private String id; private LocalDate snapshotDate; private String suggestionType; private String priority;
    private String targetType; private String targetKey; private String currentMetricValue;
    private String suggestedAction; private String expectedImpactText; private String rationaleText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTargetType() { return targetType; } public void setTargetType(String v) { this.targetType = v; }
    public String getTargetKey() { return targetKey; } public void setTargetKey(String v) { this.targetKey = v; }
    public String getCurrentMetricValue() { return currentMetricValue; } public void setCurrentMetricValue(String v) { this.currentMetricValue = v; }
    public String getSuggestedAction() { return suggestedAction; } public void setSuggestedAction(String v) { this.suggestedAction = v; }
    public String getExpectedImpactText() { return expectedImpactText; } public void setExpectedImpactText(String v) { this.expectedImpactText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
}
