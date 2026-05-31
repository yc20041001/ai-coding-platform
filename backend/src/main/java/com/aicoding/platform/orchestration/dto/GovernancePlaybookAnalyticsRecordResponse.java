package com.aicoding.platform.orchestration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GovernancePlaybookAnalyticsRecordResponse {
    private String id; private LocalDate snapshotDate; private String templateKey; private String templateName;
    private Integer planCount; private Integer completedPlanCount; private Integer blockedPlanCount;
    private BigDecimal avgCompletionRate; private BigDecimal avgStepCompletionRate;
    private BigDecimal avgResolutionHours; private Integer relatedRecipeCount;
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public LocalDate getSnapshotDate() { return snapshotDate; } public void setSnapshotDate(LocalDate v) { this.snapshotDate = v; }
    public String getTemplateKey() { return templateKey; } public void setTemplateKey(String v) { this.templateKey = v; }
    public String getTemplateName() { return templateName; } public void setTemplateName(String v) { this.templateName = v; }
    public Integer getPlanCount() { return planCount; } public void setPlanCount(Integer v) { this.planCount = v; }
    public Integer getCompletedPlanCount() { return completedPlanCount; } public void setCompletedPlanCount(Integer v) { this.completedPlanCount = v; }
    public Integer getBlockedPlanCount() { return blockedPlanCount; } public void setBlockedPlanCount(Integer v) { this.blockedPlanCount = v; }
    public BigDecimal getAvgCompletionRate() { return avgCompletionRate; } public void setAvgCompletionRate(BigDecimal v) { this.avgCompletionRate = v; }
    public BigDecimal getAvgStepCompletionRate() { return avgStepCompletionRate; } public void setAvgStepCompletionRate(BigDecimal v) { this.avgStepCompletionRate = v; }
    public BigDecimal getAvgResolutionHours() { return avgResolutionHours; } public void setAvgResolutionHours(BigDecimal v) { this.avgResolutionHours = v; }
    public Integer getRelatedRecipeCount() { return relatedRecipeCount; } public void setRelatedRecipeCount(Integer v) { this.relatedRecipeCount = v; }
}
