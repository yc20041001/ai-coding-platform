package com.aicoding.platform.orchestration.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("governance_playbook_analytics_record")
public class GovernancePlaybookAnalyticsRecordEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private LocalDate snapshotDate; private String templateKey; private String templateName;
    private Integer planCount; private Integer completedPlanCount; private Integer blockedPlanCount;
    private BigDecimal avgCompletionRate; private BigDecimal avgStepCompletionRate;
    private BigDecimal avgResolutionHours; private Integer relatedRecipeCount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
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
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
