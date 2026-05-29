package com.aicoding.platform.orchestration.dto;

import java.time.LocalDate;

public class PolicyTuningSuggestionResponse {
    private String id; private LocalDate snapshotDate; private String suggestionType;
    private String priority; private String targetScope; private String targetKey;
    private String currentValue; private String suggestedValue;
    private String expectedImpactText; private String rationaleText;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getSuggestionType() { return suggestionType; } public void setSuggestionType(String v) { this.suggestionType = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTargetScope() { return targetScope; } public void setTargetScope(String v) { this.targetScope = v; }
    public String getTargetKey() { return targetKey; } public void setTargetKey(String v) { this.targetKey = v; }
    public String getCurrentValue() { return currentValue; } public void setCurrentValue(String v) { this.currentValue = v; }
    public String getSuggestedValue() { return suggestedValue; } public void setSuggestedValue(String v) { this.suggestedValue = v; }
    public String getExpectedImpactText() { return expectedImpactText; } public void setExpectedImpactText(String v) { this.expectedImpactText = v; }
    public String getRationaleText() { return rationaleText; } public void setRationaleText(String v) { this.rationaleText = v; }
}
