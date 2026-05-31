package com.aicoding.platform.orchestration.dto;

public class CreateGovernancePlaybookTemplateRequest {
    private String templateKey; private String displayName; private String recommendationCategory;
    private String guardrailKey; private String priority; private String templateStepsJson;
    private String successCriteriaJson; private String handoffNotes;
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { this.priority = v; }
    public String getTemplateStepsJson() { return templateStepsJson; } public void setTemplateStepsJson(String v) { this.templateStepsJson = v; }
    public String getSuccessCriteriaJson() { return successCriteriaJson; } public void setSuccessCriteriaJson(String v) { this.successCriteriaJson = v; }
    public String getHandoffNotes() { return handoffNotes; } public void setHandoffNotes(String v) { this.handoffNotes = v; }
}
