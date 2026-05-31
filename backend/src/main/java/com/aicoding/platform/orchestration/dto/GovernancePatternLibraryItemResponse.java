package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernancePatternLibraryItemResponse {
    private String id; private String patternKey; private String displayName;
    private String recommendationCategory; private String guardrailKey; private String priority;
    private String patternJson; private String notes; private Boolean enabled;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getPatternKey() { return patternKey; } public void setPatternKey(String v) { this.patternKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getPatternJson() { return patternJson; } public void setPatternJson(String v) { this.patternJson = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
    public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean v) { this.enabled = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
