package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_recipe_effectiveness_snapshot")
public class GovernanceRecipeEffectivenessSnapshotEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private Long recipeId; private String recipeKey; private String recipeName;
    private Integer usageCount; private Integer completionCount; private BigDecimal successRate;
    private BigDecimal avgCompletionHours; private BigDecimal failureRate;
    private BigDecimal effectivenessScore; private String effectivenessLevel; private String summaryText;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public Long getRecipeId() { return recipeId; } public void setRecipeId(Long v) { this.recipeId = v; }
    public String getRecipeKey() { return recipeKey; } public void setRecipeKey(String v) { this.recipeKey = v; }
    public String getRecipeName() { return recipeName; } public void setRecipeName(String v) { this.recipeName = v; }
    public Integer getUsageCount() { return usageCount; } public void setUsageCount(Integer v) { this.usageCount = v; }
    public Integer getCompletionCount() { return completionCount; } public void setCompletionCount(Integer v) { this.completionCount = v; }
    public BigDecimal getSuccessRate() { return successRate; } public void setSuccessRate(BigDecimal v) { this.successRate = v; }
    public BigDecimal getAvgCompletionHours() { return avgCompletionHours; } public void setAvgCompletionHours(BigDecimal v) { this.avgCompletionHours = v; }
    public BigDecimal getFailureRate() { return failureRate; } public void setFailureRate(BigDecimal v) { this.failureRate = v; }
    public BigDecimal getEffectivenessScore() { return effectivenessScore; } public void setEffectivenessScore(BigDecimal v) { this.effectivenessScore = v; }
    public String getEffectivenessLevel() { return effectivenessLevel; } public void setEffectivenessLevel(String v) { this.effectivenessLevel = v; }
    public String getSummaryText() { return summaryText; } public void setSummaryText(String v) { this.summaryText = v; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
