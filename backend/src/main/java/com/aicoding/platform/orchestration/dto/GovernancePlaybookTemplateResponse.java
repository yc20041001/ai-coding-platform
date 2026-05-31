package com.aicoding.platform.orchestration.dto;

import java.time.LocalDateTime;

public class GovernancePlaybookTemplateResponse {
    private String id; private String templateKey; private String displayName;
    private String recommendationCategory; private String guardrailKey; private String priority;
    private Boolean enabled; private String templateStepsJson; private String successCriteriaJson;
    private String handoffNotes; private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean v) { this.enabled = v; }
    public String getTemplateStepsJson() { return templateStepsJson; } public void setTemplateStepsJson(String v) { this.templateStepsJson = v; }
    public String getSuccessCriteriaJson() { return successCriteriaJson; } public void setSuccessCriteriaJson(String v) { this.successCriteriaJson = v; }
    public String getHandoffNotes() { return handoffNotes; } public void setHandoffNotes(String v) { this.handoffNotes = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
