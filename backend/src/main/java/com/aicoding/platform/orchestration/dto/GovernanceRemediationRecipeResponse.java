package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GovernanceRemediationRecipeResponse {
    private String id; private String recipeKey; private String displayName; private String recipeType;
    private String recommendationCategory; private String guardrailKey; private String stepsJson;
    private String prerequisitesJson; private String successCriteriaJson;
    private BigDecimal effectivenessScore; private Integer usageCount; private Boolean enabled;
    private LocalDateTime createTime; private LocalDateTime updateTime;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getRecipeKey() { return recipeKey; } public void setRecipeKey(String v) { this.recipeKey = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getRecipeType() { return recipeType; } public void setRecipeType(String v) { this.recipeType = v; }
    public String getRecommendationCategory() { return recommendationCategory; } public void setRecommendationCategory(String v) { this.recommendationCategory = v; }
    public String getGuardrailKey() { return guardrailKey; } public void setGuardrailKey(String v) { this.guardrailKey = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public String getPrerequisitesJson() { return prerequisitesJson; } public void setPrerequisitesJson(String v) { this.prerequisitesJson = v; }
    public String getSuccessCriteriaJson() { return successCriteriaJson; } public void setSuccessCriteriaJson(String v) { this.successCriteriaJson = v; }
    public BigDecimal getEffectivenessScore() { return effectivenessScore; } public void setEffectivenessScore(BigDecimal v) { this.effectivenessScore = v; }
    public Integer getUsageCount() { return usageCount; } public void setUsageCount(Integer v) { this.usageCount = v; }
    public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean v) { this.enabled = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
