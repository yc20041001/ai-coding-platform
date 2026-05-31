package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("governance_remediation_recipe")
public class GovernanceRemediationRecipeEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String recipeKey; private String displayName; private String recipeType;
    private String recommendationCategory; private String guardrailKey;
    private String stepsJson; private String prerequisitesJson; private String successCriteriaJson;
    private BigDecimal effectivenessScore; private Integer usageCount; private Integer enabled;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
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
    public Integer getEnabled() { return enabled; } public void setEnabled(Integer v) { this.enabled = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime v) { this.updateTime = v; }
}
